@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.os.CancellationSignal
import android.widget.FrameLayout
import androidx.annotation.MainThread

/**
 * A WebView holding a prerendered document between prewarm and display adoption. Never attached to a
 * window: adoption hands it to the display factory, which hosts it and activates the prerender.
 */
internal class PrewarmedWebView(
    private val webView: PaywallWebView,
    val bridge: WebViewJavaScriptBridge,
    private val callbacks: PrewarmBridgeCallbacks,
    val resolvedUrl: String,
    val cancellationSignal: CancellationSignal,
) {

    /**
     * Hands this view to the composition adopting it. It is already configured down to its
     * document-start scripts, so only the callbacks and the component config need repointing;
     * loading the prerendered URL activates the prerender rather than starting a fresh load.
     *
     * The component config is applied *before* the load, because that load resets the handshake and
     * the following `init`/`fit` must carry the adopting component's values, not the warmed one's.
     */
    @MainThread
    fun activateIn(
        identity: WebViewIdentity,
        onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit,
        onDocumentReset: () -> Unit,
        onLoadFailed: () -> Unit,
    ): FrameLayout {
        callbacks.rebind(onContentResize, onDocumentReset, onLoadFailed)
        bridge.rebindComponent(
            componentId = identity.componentId,
            sizeToContentWidth = identity.sizeToContentWidth,
            sizeToContentHeight = identity.sizeToContentHeight,
        )
        webView.loadUrl(resolvedUrl)
        return webView.hostedInFrameLayout()
    }

    @MainThread
    fun destroy() {
        cancellationSignal.cancel()
        webView.releasePaywallWebView(bridge)
    }
}
