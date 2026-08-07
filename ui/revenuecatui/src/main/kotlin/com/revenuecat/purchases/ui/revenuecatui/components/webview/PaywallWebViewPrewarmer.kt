@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
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
 * Speculatively prerenders a `web_view` component's bundle so that displaying it later is a local
 * activation instead of a network load.
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

    // Insertion-ordered so the eldest entry is the one evicted when the pool is full. A paywall can
    // contain 0..N web_view components, so one slot could only ever serve the first of them.
    private val pool = LinkedHashMap<String, PrewarmedWebView>()

    // A prerender that is never displayed holds a renderer process at foreground priority, which
    // Android will not reclaim on its own. Each entry expires on its own schedule.
    private val expiries = mutableMapOf<String, Runnable>()

    /**
     * Prerenders [url] for the component that will render it. [url] is resolved through
     * [WebViewUrlResolver] so the resulting identity matches the display path's byte for byte;
     * an unresolvable URL is a no-op.
     *
     * Entries are pooled by resolved URL up to [MAX_PREWARMED]; a repeat request for a URL already
     * held is a no-op, and a request arriving at capacity evicts the eldest entry.
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

        if (pool.containsKey(resolvedUrl)) {
            Logger.d("Paywalls V2 web_view already prewarmed for this URL; keeping the existing one.")
            return
        }

        val identity = WebViewIdentity(
            resolvedUrl = resolvedUrl,
            componentId = componentId,
            sizeToContentWidth = sizeToContentWidth,
            sizeToContentHeight = sizeToContentHeight,
        )
        val prewarmed = createPrerendered(context, identity) ?: return

        // Evict before inserting, so the pool never briefly exceeds its bound. The eldest entry is the
        // one most likely to belong to a superseded offering.
        if (pool.size >= MAX_PREWARMED) {
            pool.keys.firstOrNull()?.let { eldest ->
                Logger.d("Paywalls V2 web_view prewarm pool is full; evicting the oldest entry.")
                discard(eldest)
            }
        }
        pool[resolvedUrl] = prewarmed
        scheduleExpiry(resolvedUrl)
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

    @Suppress("TooGenericExceptionCaught")
    private fun createPrerendered(context: Context, identity: WebViewIdentity): PrewarmedWebView? {
        val callbacks = PrewarmBridgeCallbacks()
        // Application context: this view is never attached to a window, so holding an Activity here
        // would outlive it.
        val configured = createPaywallWebView(
            context = context.applicationContext,
            identity = identity,
            onContentResize = callbacks::dispatchResize,
            onDocumentReset = callbacks::dispatchDocumentReset,
            onLoadFailed = callbacks::dispatchLoadFailed,
        ) ?: return null

        val prewarmed = PrewarmedWebView(
            webView = configured.webView,
            bridge = configured.bridge,
            callbacks = callbacks,
            resolvedUrl = identity.resolvedUrl,
            cancellationSignal = CancellationSignal(),
        )
        return try {
            WebViewCompat.prerenderUrlAsync(
                configured.webView,
                identity.resolvedUrl,
                prewarmed.cancellationSignal,
                ContextCompat.getMainExecutor(context),
                PrerenderLogger(identity.resolvedUrl),
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
     * A miss leaves the pool untouched. Sibling entries belong to other components on the same paywall
     * and are very likely about to be taken themselves.
     */
    @MainThread
    fun take(resolvedUrl: String): PrewarmedWebView? {
        val taken = pool.remove(resolvedUrl)
        if (taken == null) {
            Logger.d("Paywalls V2 web_view not prewarmed for this URL; loading it cold.")
            return null
        }
        expiries.remove(resolvedUrl)?.let(mainHandler::removeCallbacks)
        return taken
    }

    @MainThread
    fun releaseAll() {
        expiries.values.forEach(mainHandler::removeCallbacks)
        expiries.clear()
        pool.values.forEach { it.destroy() }
        pool.clear()
    }

    @VisibleForTesting
    internal fun heldCount(): Int = pool.size

    // Inner, not nested: an async failure must discard the entry it prerendered into, and only that
    // entry (take() may have already removed it, and siblings are unrelated).
    private inner class PrerenderLogger(private val resolvedUrl: String) : PrerenderOperationCallback {
        override fun onPrerenderActivated() {
            Logger.d("Paywalls V2 web_view prerender activated for '$resolvedUrl'.")
        }

        override fun onError(exception: PrerenderException) {
            Logger.d("Paywalls V2 web_view prerender failed for '$resolvedUrl': $exception")
            discard(resolvedUrl)
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

        val shared = PaywallWebViewPrewarmer()
    }
}
