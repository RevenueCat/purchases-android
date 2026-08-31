@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Loads `web_view` component bundles offscreen so the paywall profile's http cache holds them before the
 * component renders.
 */
@Suppress("TooManyFunctions")
internal class PaywallWebViewPrewarmer(
    private val maxConcurrentWarms: Int = MAX_CONCURRENT_WARMS,
) {

    private class InFlightWarm(
        val view: PaywallWebView,
        var scheduledTeardown: Runnable,
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    private val queue = ArrayDeque<String>()

    private val inFlight = LinkedHashMap<String, InFlightWarm>()

    private val warmedUrls = mutableSetOf<String>()

    private var applicationContext: Context? = null

    private var displayedComponents = 0

    private val trimCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            // low severity trim levels where we want to keep preloading
            val ignoredTrimLevels = listOf(
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            )
            if (level in ignoredTrimLevels) return
            if (inFlight.isEmpty() && queue.isEmpty()) return
            Logger.d("Paywalls V2 web_view cache warming released on trim (level $level).")
            queue.addAll(0, inFlight.keys.filterNot { it in warmedUrls })
            releaseInFlight()
        }

        @Deprecated("deprecated on interface but mandatory implementation")
        override fun onLowMemory() = Unit

        override fun onConfigurationChanged(newConfig: Configuration) = Unit
    }

    @MainThread
    fun prewarm(context: Context, url: String) {
        val resolvedUrl = WebViewUrlResolver.resolve(url)
        if (resolvedUrl == null) {
            Logger.d("Paywalls V2 web_view not prewarmed: URL must be https with no '{{' markers: '$url'")
            return
        }
        val appContext = context.applicationContext
        if (applicationContext == null) {
            appContext.registerComponentCallbacks(trimCallbacks)
        }
        applicationContext = appContext
        if (alreadyCovered(resolvedUrl)) {
            Logger.d("Paywalls V2 web_view already warmed or warming this URL; skipping.")
        } else {
            queue.addLast(resolvedUrl)
        }
        startAvailable()
    }

    /**
     * Stops warming while a `web_view` component displays [resolvedUrl]
     */
    @MainThread
    fun onDisplayStarted(resolvedUrl: String) {
        queue.remove(resolvedUrl)
        displayedComponents++
        if (displayedComponents > 1) return
        queue.addAll(0, inFlight.keys.filterNot { it in warmedUrls || it == resolvedUrl })
        mainHandler.post(::releaseInFlight)
    }

    @MainThread
    fun markWarmed(resolvedUrl: String) {
        warmedUrls.add(resolvedUrl)
    }

    @MainThread
    fun onCacheCleared() {
        warmedUrls.clear()
        // These loaded against the storage that just went away, so they start over rather than being trusted.
        queue.addAll(0, inFlight.keys.filterNot { it in queue })
        releaseInFlight()
        // Posted so a settle already queued for a released warm runs first and no-ops on its absent url;
        // inline it would settle the restarted warm instead. Released views post nothing new, because
        // destroyPaywallWebView drops their client.
        mainHandler.post(::startAvailable)
    }

    /** Counterpart to [onDisplayStarted]. Warming resumes once the last component is gone. */
    @MainThread
    fun onDisplayEnded() {
        if (displayedComponents == 0) return
        displayedComponents--
        if (displayedComponents == 0) mainHandler.post(::startAvailable)
    }

    @MainThread
    private fun releaseInFlight() {
        inFlight.keys.toList().forEach(::release)
    }

    private fun alreadyCovered(resolvedUrl: String): Boolean =
        resolvedUrl in warmedUrls || resolvedUrl in inFlight || resolvedUrl in queue

    @MainThread
    private fun startAvailable() {
        if (displayedComponents > 0) return
        val context = applicationContext ?: return
        queue.removeAll { it in warmedUrls }
        while (inFlight.size < maxConcurrentWarms) {
            val url = queue.removeFirstOrNull()
            if (url == null || !begin(context, url)) break
        }
    }

    @MainThread
    @Suppress("TooGenericExceptionCaught")
    private fun begin(context: Context, url: String): Boolean {
        val stalled = Runnable {
            Logger.d("Paywalls V2 web_view cache warm stalled; moving on to the next URL.")
            finishWarm(url)
        }
        try {
            val view = createWarmingWebView(
                context = context,
                resolvedUrl = url,
                // Posted: these arrive inside a WebViewClient callback, and destroy() must not run there.
                onLoadFailed = { mainHandler.post { finishWarm(url) } },
                onLoadFinished = { mainHandler.post { settle(url) } },
            )
            inFlight[url] = InFlightWarm(view = view, scheduledTeardown = stalled)
            view.loadUrl(url)
            mainHandler.postDelayed(stalled, WARM_STALL_TIMEOUT_MS)
        } catch (error: Throwable) {
            // A missing or mid-update WebView package throws Error, not Exception.
            Logger.w("Paywalls V2 web_view could not be prewarmed: $error")
            release(url)
            mainHandler.post { startAvailable() }
            return false
        }
        Logger.d("Paywalls V2 web_view warming the http cache for '$url'.")
        return true
    }

    /** `onPageFinished` covers the main frame only, so the view is held for what script still fetches. */
    @MainThread
    private fun settle(url: String) {
        val warm = inFlight[url] ?: return
        warmedUrls.add(url)
        mainHandler.removeCallbacks(warm.scheduledTeardown)
        warm.scheduledTeardown = Runnable { finishWarm(url) }
        mainHandler.postDelayed(warm.scheduledTeardown, SETTLE_GRACE_MS)
    }

    /** WebView callbacks can outlive the warm that armed them, so an unknown url is ignored. */
    @MainThread
    private fun finishWarm(url: String) {
        if (release(url)) startAvailable()
    }

    @MainThread
    private fun release(url: String): Boolean {
        val warm = inFlight.remove(url) ?: return false
        mainHandler.removeCallbacks(warm.scheduledTeardown)
        warm.view.destroyPaywallWebView()
        return true
    }

    @get:VisibleForTesting
    internal val queuedCount: Int get() = queue.size

    @get:VisibleForTesting
    internal val warmingCount: Int get() = inFlight.size

    internal companion object {
        @VisibleForTesting
        internal const val WARM_STALL_TIMEOUT_MS: Long = 3_000L

        @VisibleForTesting
        internal const val SETTLE_GRACE_MS: Long = 250L

        @VisibleForTesting
        internal const val MAX_CONCURRENT_WARMS: Int = 2

        val shared = PaywallWebViewPrewarmer()
    }
}
