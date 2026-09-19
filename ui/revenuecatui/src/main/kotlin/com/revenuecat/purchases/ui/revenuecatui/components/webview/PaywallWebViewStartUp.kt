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
 * Moves WebView startup off the UI thread ahead of the first paywall `web_view` render, which would
 * otherwise pay for it there.
 *
 * Needs no feature check: the API degrades internally on older WebViews, and below that still moves
 * provider class loading off the UI thread.
 *
 * A failed startup is not retried, since androidx warns further WebView calls on such a device are likely
 * to throw or crash; a rejected submission is, because startup was never attempted.
 */
internal object PaywallWebViewStartUp {

    private val started = AtomicBoolean(false)

    private val startUpExecutor = guarded(Dispatchers.IO.asExecutor())

    @VisibleForTesting
    internal fun guarded(delegate: Executor) = Executor { runnable ->
        delegate.execute {
            // androidx runs provider class loading here, and a missing or mid-update WebView package
            // surfaces as NoClassDefFoundError / UnsatisfiedLinkError, not as an Exception.
            @Suppress("TooGenericExceptionCaught")
            try {
                runnable.run()
            } catch (error: Throwable) {
                Logger.w("Paywalls V2 WebView startup failed: $error")
            }
        }
    }

    fun startUp(context: Context, executor: Executor = startUpExecutor) {
        if (!started.compareAndSet(false, true)) return
        val applicationContext = context.applicationContext
        try {
            // `isFeatureSupported` loads the provider, the very work this API exists to move off the caller.
            executor.execute { requestStartUp(applicationContext, executor) }
        } catch (error: RejectedExecutionException) {
            started.set(false)
            Logger.w("Paywalls V2 WebView startup could not be triggered: $error")
        }
    }

    private fun requestStartUp(applicationContext: Context, executor: Executor) {
        // Naming a set opts every unnamed profile out, and named profiles have resources allocated during
        // startup, so load only the one `applyPaywallProfile` will pick.
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
                        // Null on WebViews without async startup support, where startup ran but reports no timings.
                        val uiThreadTime = result.totalTimeInUiThreadMillis?.let { "$it ms" } ?: "not reported"
                        Logger.d("Paywalls V2 WebView startup complete (UI thread time: $uiThreadTime).")
                    }

                    override fun onError(error: WebViewStartupException) {
                        Logger.w("Paywalls V2 WebView startup failed: $error")
                    }
                },
            )
        } catch (error: RejectedExecutionException) {
            started.set(false)
            Logger.w("Paywalls V2 WebView startup could not be triggered: $error")
        }
    }

    @VisibleForTesting
    internal fun resetForTesting() {
        started.set(false)
    }
}
