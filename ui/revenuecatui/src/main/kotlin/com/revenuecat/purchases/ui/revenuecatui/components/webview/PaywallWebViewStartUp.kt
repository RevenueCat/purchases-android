@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.webkit.Profile
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Moves WebView startup off the UI thread ahead of the first paywall `web_view` render.
 *
 * WebView startup otherwise happens on first use and blocks the UI thread; measured here at ~190 ms
 * for the first render, dropping to ~112 ms when startup has already run. The paywall profile is
 * preloaded in the same call so the render does not pay for it separately.
 *
 * Safe on every WebView version: the API requires no feature check and degrades internally to an
 * older entry point, and below that still moves provider class loading off the UI thread.
 *
 * A no-op beyond the first call per process, except when the submission itself is rejected before
 * startup is ever attempted. Once startup has been attempted and failed, it is not retried: the
 * androidx contract says further WebView calls on such a device are likely to throw or crash.
 */
internal object PaywallWebViewStartUp {

    private val started = AtomicBoolean(false)

    // The AtomicBoolean guard already keeps at most one submission in flight, so this needs no parallelism
    // limit of its own; unlike a dedicated executor it parks no thread for the process lifetime.
    private val startUpExecutor = guarded(Dispatchers.IO.asExecutor())

    /**
     * Wraps [delegate] so a throw inside a submitted task cannot escape. Provider class loading runs in
     * here and throws on devices whose WebView package is missing or mid-update; androidx catches none of
     * it, and an uncaught throw on a Dispatchers.IO thread kills the host app.
     */
    @VisibleForTesting
    internal fun guarded(delegate: Executor) = Executor { runnable ->
        delegate.execute {
            @Suppress("TooGenericExceptionCaught")
            try {
                runnable.run()
            } catch (error: Exception) {
                Logger.w("Paywalls V2 WebView startup failed: $error")
            }
        }
    }

    fun startUp(context: Context, executor: Executor = startUpExecutor) {
        if (!started.compareAndSet(false, true)) return
        val applicationContext = context.applicationContext
        try {
            // Nothing here touches androidx.webkit on the caller's thread. `isFeatureSupported` loads the
            // WebView provider, which is the exact work this API exists to move off the caller, and androidx
            // requires every androidx.webkit call (WebViewFeature included) to wait for startup.
            executor.execute { requestStartUp(applicationContext, executor) }
        } catch (error: RejectedExecutionException) {
            started.set(false)
            Logger.w("Paywalls V2 WebView startup could not be triggered: $error")
        }
    }

    private fun requestStartUp(applicationContext: Context, executor: Executor) {
        // Naming a set opts every unnamed profile *out*, and named ones have resources allocated during
        // startup, so load exactly the profile the component will end up on: `applyPaywallProfile` uses the
        // paywall profile only where MULTI_PROFILE is supported, and keeps Default otherwise.
        // setProfilesToLoadDuringStartup itself needs no feature check; it no-ops on glue layers without
        // STARTUP_FEATURE_SET_PROFILES_TO_LOAD.
        val profileToLoad = if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            PAYWALL_PROFILE_NAME
        } else {
            Profile.DEFAULT_PROFILE_NAME
        }
        val config = WebViewStartUpConfig.Builder(executor)
            .setProfilesToLoadDuringStartup(setOf(profileToLoad))
            .build()
        try {
            WebViewCompat.startUpWebView(
                applicationContext,
                config,
                object : WebViewOutcomeReceiver<WebViewStartUpResult, WebViewStartupException> {
                    override fun onResult(result: WebViewStartUpResult) {
                        // Null on WebViews without async startup support, where startup still ran but reports no
                        // timings. Logging that as 0 ms would read as free on exactly the slowest devices.
                        val uiThreadTime = result.totalTimeInUiThreadMillis?.let { "$it ms" } ?: "not reported"
                        Logger.d("Paywalls V2 WebView startup complete (UI thread time: $uiThreadTime).")
                    }

                    override fun onError(error: WebViewStartupException) {
                        Logger.w("Paywalls V2 WebView startup failed: $error")
                    }
                },
            )
        } catch (error: RejectedExecutionException) {
            // Startup was never attempted, so a later call may retry.
            started.set(false)
            Logger.w("Paywalls V2 WebView startup could not be triggered: $error")
        }
    }

    @VisibleForTesting
    internal fun resetForTesting() {
        started.set(false)
    }
}
