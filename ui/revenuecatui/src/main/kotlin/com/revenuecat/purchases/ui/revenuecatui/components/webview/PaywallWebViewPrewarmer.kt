@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.webkit.PrerenderException
import androidx.webkit.PrerenderOperationCallback
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Speculatively loads `web_view` component bundles ahead of display, in two tiers.
 *
 * A held prerender is the better one: the display factory adopts that very WebView, so showing the
 * component is a local activation with no parse, no script execution and no layout. It costs a live
 * renderer process for as long as it is held, so only [MAX_PREWARMED] are kept.
 *
 * Every url beyond that is still worth loading, because running a document populates the profile's
 * http cache with the bundle and every subresource it fetches, and that cache is on disk and outlives
 * the WebView that filled it. Those urls are warmed one at a time and discarded as soon as their
 * document finishes, so a paywall carrying more `web_view` components than the pool holds still gets
 * every one of them off the network at display time.
 *
 * The WebView is built by [createPaywallWebView], the same builder the display path uses, because a
 * prewarmed view is adopted on identity alone: any configuration applied to only one of the two
 * would silently reach the screen.
 *
 * Everything here is best-effort. Any failure leaves the display path to load cold, which is
 * exactly today's behaviour. Requires WebView M137+; below that [prewarm] is a no-op.
 *
 * Main thread only.
 */
internal class PaywallWebViewPrewarmer {

    // Deliberately lazy: isFeatureSupported loads the WebView provider, which costs main-thread time
    // and, on older WebViews, spawns a renderer process. Never evaluate it unless we intend to
    // prewarm. take() must stay clear of it too, so that rendering alone never pays that cost.
    private val prerenderSupported: Boolean by lazy {
        WebViewFeature.isFeatureSupported(WebViewFeature.PRERENDER_WITH_URL)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val pool = LinkedHashMap<String, PrewarmedWebView>()

    // A prerender that is never displayed holds a renderer process at foreground priority, which
    // Android will not reclaim on its own. Each entry expires on its own schedule.
    private val expiries = mutableMapOf<String, Runnable>()

    // Urls the pool had no room for, warmed for the http cache alone. One at a time: WebView caps its
    // own renderer processes low, so concurrent loads contend instead of overlapping.
    private val cacheWarmQueue = ArrayDeque<WebViewIdentity>()
    private var cacheWarmInFlight: PrewarmedWebView? = null
    private var abandonCacheWarm: Runnable? = null

    // Held so a queued warm can be built after the call that enqueued it has returned. Application
    // context only, so this cannot outlive anything it should not.
    private var applicationContext: Context? = null

    private var trimCallbacksRegistered = false

    /**
     * A held prerender is a discardable cache, so it is dropped at every trim level rather than left to
     * compete with the host app for memory. Android delivers these on the main thread.
     */
    @VisibleForTesting
    internal val trimCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (pool.isNotEmpty() || cacheWarmInFlight != null) {
                Logger.d("Paywalls V2 web_view warming abandoned on memory trim (level $level).")
            }
            releaseAll()
        }

        override fun onLowMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

        override fun onConfigurationChanged(newConfig: Configuration) = Unit
    }

    /**
     * Prerenders [url] for the component that will render it. [url] is resolved through
     * [WebViewUrlResolver] so the resulting identity matches the display path's byte for byte;
     * an unresolvable URL is a no-op.
     *
     * Entries are pooled by resolved URL up to [MAX_PREWARMED]. Beyond that the url is queued and warmed
     * for the http cache alone, one at a time. A url already held, in flight or queued is a no-op.
     */
    @MainThread
    @Suppress("ReturnCount")
    fun prewarm(
        context: Context,
        url: String,
        componentId: String,
        sizeToContentWidth: Boolean = false,
        sizeToContentHeight: Boolean = false,
    ) {
        if (componentId.isBlank()) {
            Logger.d("Paywalls V2 web_view not prewarmed: componentId is blank.")
            return
        }
        val resolvedUrl = WebViewUrlResolver.resolve(url)
        if (resolvedUrl == null) {
            Logger.d("Paywalls V2 web_view not prewarmed: URL must be https with no '{{' markers: '$url'")
            return
        }
        if (!prerenderSupported) {
            Logger.d("Paywalls V2 web_view not prewarmed: this System WebView does not support prerendering.")
            return
        }

        if (resolvedUrl in warmingUrls) {
            Logger.d("Paywalls V2 web_view already warming this URL; keeping the existing one.")
            return
        }

        val identity = WebViewIdentity(
            resolvedUrl = resolvedUrl,
            componentId = componentId,
            sizeToContentWidth = sizeToContentWidth,
            sizeToContentHeight = sizeToContentHeight,
        )
        applicationContext = context.applicationContext
        registerTrimCallbacks(context)
        // At capacity nothing is evicted: a held entry is worth more than a queued one, and the overflow
        // still gets its bundle into the http cache.
        if (pool.size >= MAX_PREWARMED) {
            cacheWarmQueue.addLast(identity)
            startNextCacheWarm()
            return
        }
        pool[resolvedUrl] = createPrerendered(context, identity) ?: return
        scheduleExpiry(resolvedUrl)
    }

    /** Every url this instance is already holding, loading, or about to load. */
    private val warmingUrls: Set<String>
        get() = buildSet {
            addAll(pool.keys)
            cacheWarmInFlight?.resolvedUrl?.let(::add)
            cacheWarmQueue.forEach { add(it.resolvedUrl) }
        }

    /**
     * Loads the next queued url purely to populate the profile's http cache, then discards it. Only one
     * runs at a time, and each is abandoned after [CACHE_WARM_BUDGET_MS] so a bundle that never finishes
     * cannot stall the ones behind it.
     */
    @MainThread
    private fun startNextCacheWarm() {
        if (cacheWarmInFlight != null) return
        val context = applicationContext ?: return
        cacheWarmQueue.removeFirstOrNull()?.let { identity -> beginCacheWarm(context, identity) }
    }

    @MainThread
    private fun beginCacheWarm(context: Context, identity: WebViewIdentity) {
        val warming = createPrerendered(context, identity, onDocumentFinished = ::finishCacheWarm)
        if (warming == null) {
            // Nothing to tear down. Try the next url on a later loop turn rather than recursing here.
            mainHandler.post { startNextCacheWarm() }
            return
        }
        cacheWarmInFlight = warming
        Logger.d("Paywalls V2 web_view warming the http cache for '${identity.resolvedUrl}'.")
        val abandon = Runnable {
            Logger.d("Paywalls V2 web_view cache warm did not finish in time; moving on to the next URL.")
            finishCacheWarm()
        }
        abandonCacheWarm = abandon
        mainHandler.postDelayed(abandon, CACHE_WARM_BUDGET_MS)
    }

    /** Drops the in-flight cache warm, keeping what it put in the http cache, and starts the next. */
    @MainThread
    private fun finishCacheWarm() {
        val finished = cacheWarmInFlight ?: return
        cacheWarmInFlight = null
        abandonCacheWarm?.let(mainHandler::removeCallbacks)
        abandonCacheWarm = null
        finished.destroy()
        startNextCacheWarm()
    }

    private fun scheduleExpiry(resolvedUrl: String) {
        val expiry = Runnable {
            Logger.d("Paywalls V2 web_view prewarm expired before display; releasing it.")
            discard(resolvedUrl)
        }
        expiries[resolvedUrl] = expiry
        mainHandler.postDelayed(expiry, HOLD_TIMEOUT_MS)
    }

    /** Destroys one entry and cancels its expiry. Siblings are untouched. */
    @MainThread
    private fun discard(resolvedUrl: String) {
        expiries.remove(resolvedUrl)?.let(mainHandler::removeCallbacks)
        pool.remove(resolvedUrl)?.destroy()
    }

    // Registered on first hold rather than on construction, so an SDK that never prewarms adds no
    // callback to the host application. Kept for the process lifetime: this instance is a singleton.
    private fun registerTrimCallbacks(context: Context) {
        if (trimCallbacksRegistered) return
        trimCallbacksRegistered = true
        context.applicationContext.registerComponentCallbacks(trimCallbacks)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun createPrerendered(
        context: Context,
        identity: WebViewIdentity,
        onDocumentFinished: () -> Unit = {},
    ): PrewarmedWebView? {
        val callbacks = PrewarmBridgeCallbacks()
        // Assigned below, before any load can start: the failure callback needs the entry it belongs to.
        var entry: PrewarmedWebView? = null
        // Application context: prewarm has no Activity to borrow, and this view must not outlive the one
        // that happens to be showing. The cost is that it cannot host JS dialogs, <select> popups, file
        // choosers or fullscreen video, unlike a view the display path builds from an Activity.
        val configured = createPaywallWebView(
            context = context.applicationContext,
            identity = identity,
            onContentResize = callbacks::dispatchResize,
            onDocumentReset = callbacks::dispatchDocumentReset,
            onLoadFailed = {
                callbacks.dispatchLoadFailed()
                // Posted: this arrives inside a WebViewClient callback, and destroy() must not run there.
                entry?.let { failed -> mainHandler.post { discardIfHeld(failed) } }
            },
            onLoadFinished = onDocumentFinished,
        ) ?: return null

        val prewarmed = PrewarmedWebView(
            webView = configured.webView,
            bridge = configured.bridge,
            callbacks = callbacks,
            resolvedUrl = identity.resolvedUrl,
            cancellationSignal = CancellationSignal(),
        )
        entry = prewarmed
        return try {
            WebViewCompat.prerenderUrlAsync(
                configured.webView,
                identity.resolvedUrl,
                prewarmed.cancellationSignal,
                ContextCompat.getMainExecutor(context),
                PrerenderLogger(prewarmed),
            )
            prewarmed
        } catch (error: RuntimeException) {
            Logger.w("Paywalls V2 web_view could not be prewarmed: $error")
            prewarmed.destroy()
            null
        }
    }

    /**
     * Hands the prewarmed WebView for [resolvedUrl] to the display factory, removing it from the pool.
     * Matching is on resolved URL alone, matching iOS: the component id and `Fit` axes are rebound at
     * activation, so a view warmed for one component serves any component pointing at the same bundle.
     *
     * An entry whose document failed to load is destroyed instead of handed over, so the caller loads
     * cold rather than rendering nothing.
     *
     * A miss leaves the pool untouched. Sibling entries belong to other components on the same paywall
     * and are very likely about to be taken themselves.
     */
    @MainThread
    fun take(resolvedUrl: String): PrewarmedWebView? {
        // Whatever the outcome, this url is being displayed now, and that load fills the http cache by
        // itself. A queued warm for it would be pure waste.
        cacheWarmQueue.removeAll { it.resolvedUrl == resolvedUrl }
        val taken = pool.remove(resolvedUrl)
        if (taken == null) {
            Logger.d("Paywalls V2 web_view not prewarmed for this URL; loading it cold.")
            return null
        }
        expiries.remove(resolvedUrl)?.let(mainHandler::removeCallbacks)
        return taken.takeUnless { it.loadFailed } ?: run {
            Logger.d("Paywalls V2 web_view prewarm discarded: its document failed to load while prewarming.")
            taken.destroy()
            null
        }
    }

    /**
     * Discards [entry], whichever tier it belongs to. Compared by instance, not by URL: a later prewarm
     * of the same URL is a different view, and an async failure from this one must not take it down (nor
     * touch one that [take] has already handed to a composition). Siblings are left alone.
     */
    @MainThread
    private fun discardIfHeld(entry: PrewarmedWebView) {
        when {
            pool[entry.resolvedUrl] === entry -> discard(entry.resolvedUrl)
            cacheWarmInFlight === entry -> finishCacheWarm()
        }
    }

    @MainThread
    fun releaseAll() {
        cacheWarmQueue.clear()
        abandonCacheWarm?.let(mainHandler::removeCallbacks)
        abandonCacheWarm = null
        cacheWarmInFlight?.destroy()
        cacheWarmInFlight = null
        expiries.values.forEach(mainHandler::removeCallbacks)
        expiries.clear()
        pool.values.forEach { it.destroy() }
        pool.clear()
    }

    @get:VisibleForTesting
    internal val heldCount: Int get() = pool.size

    @get:VisibleForTesting
    internal val queuedCacheWarmCount: Int get() = cacheWarmQueue.size

    @get:VisibleForTesting
    internal val isWarmingCache: Boolean get() = cacheWarmInFlight != null

    private inner class PrerenderLogger(private val entry: PrewarmedWebView) : PrerenderOperationCallback {
        override fun onPrerenderActivated() {
            Logger.d("Paywalls V2 web_view prerender activated for '${entry.resolvedUrl}'.")
        }

        override fun onError(exception: PrerenderException) {
            Logger.d("Paywalls V2 web_view prerender failed for '${entry.resolvedUrl}': $exception")
            discardIfHeld(entry)
        }
    }

    internal companion object {
        /**
         * How long an unadopted prerender is held. Generous next to the ~0.5s of lead time that
         * saturates the latency benefit, but bounded so a paywall that is never shown cannot pin a
         * renderer process indefinitely.
         */
        @VisibleForTesting
        internal const val HOLD_TIMEOUT_MS: Long = 30_000L

        /**
         * Pool bound. Measured on device, the cost of holding prerenders is dominated by the renderer
         * processes WebView spawns, which it caps low on its own (2 on a 5.6 GB device, 1 on 4 GB):
         * the first held view cost ~104 MB there, the second ~104 MB, and the third through fifth
         * ~1-6 MB each. So a small pool is nearly free once the first entry is paid for, while still
         * covering paywalls that carry several web_view components.
         */
        @VisibleForTesting
        internal const val MAX_PREWARMED: Int = 3

        /**
         * How long one cache warm may run before the next url starts instead. Well above the 200-850 ms
         * a cold bundle load measured on device, so a normal bundle finishes on its own signal; this only
         * decides how long a stalled one is allowed to block the queue. Whether a prerendered document
         * reports `onPageFinished` before activation at all is undocumented, so this doubles as the pace
         * setter if it never arrives.
         */
        @VisibleForTesting
        internal const val CACHE_WARM_BUDGET_MS: Long = 5_000L

        val shared = PaywallWebViewPrewarmer()
    }
}
