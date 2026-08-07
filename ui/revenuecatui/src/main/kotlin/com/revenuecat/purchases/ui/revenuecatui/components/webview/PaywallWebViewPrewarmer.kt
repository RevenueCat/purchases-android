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

    private var slot: PrewarmedWebView? = null

    // A prerender that is never displayed holds a renderer process at foreground priority, which
    // Android will not reclaim on its own. Bound it.
    private val releaseOnTimeout = Runnable {
        Logger.d("Paywalls V2 web_view prewarm expired before display; releasing.")
        releaseAll()
    }

    /**
     * Prerenders [url] for the component that will render it. [url] is resolved through
     * [WebViewUrlResolver] so the resulting identity matches the display path's byte for byte;
     * an unresolvable URL is a no-op.
     *
     * A single slot is held: a request arriving while another component is already prewarmed is
     * dropped rather than evicting it.
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
        if (slot != null) {
            Logger.d("Paywalls V2 web_view not prewarmed: a slot is already held (componentId='$componentId').")
            return
        }
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

        val identity = WebViewIdentity(
            resolvedUrl = resolvedUrl,
            componentId = componentId,
            sizeToContentWidth = sizeToContentWidth,
            sizeToContentHeight = sizeToContentHeight,
        )
        slot = createPrerendered(context, identity) ?: return
        mainHandler.removeCallbacks(releaseOnTimeout)
        mainHandler.postDelayed(releaseOnTimeout, HOLD_TIMEOUT_MS)
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
     * Hands the prewarmed WebView to the display factory, clearing the slot either way. Matching is
     * on resolved URL alone, matching iOS: the component id and `Fit` axes are rebound at activation,
     * so a view warmed for one component can serve any component pointing at the same bundle. A
     * prewarmed view for a different URL is destroyed rather than kept: the paywall being shown is not
     * the one it was warmed for.
     */
    @MainThread
    fun take(resolvedUrl: String): PrewarmedWebView? {
        val current = slot ?: return null
        slot = null
        mainHandler.removeCallbacks(releaseOnTimeout)
        return if (current.resolvedUrl == resolvedUrl) {
            current
        } else {
            Logger.d("Paywalls V2 web_view prewarm discarded: held for a different URL than requested.")
            current.destroy()
            null
        }
    }

    @MainThread
    fun releaseAll() {
        mainHandler.removeCallbacks(releaseOnTimeout)
        slot?.destroy()
        slot = null
    }

    // Inner, not nested: an async failure must release the slot it prerendered into, if that slot is
    // still the one this callback was created for (take() may have already cleared or replaced it).
    private inner class PrerenderLogger(private val resolvedUrl: String) : PrerenderOperationCallback {
        override fun onPrerenderActivated() {
            Logger.d("Paywalls V2 web_view prerender activated for '$resolvedUrl'.")
        }

        override fun onError(exception: PrerenderException) {
            Logger.d("Paywalls V2 web_view prerender failed for '$resolvedUrl': $exception")
            if (slot?.resolvedUrl == resolvedUrl) {
                releaseAll()
            }
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

        val shared = PaywallWebViewPrewarmer()
    }
}
