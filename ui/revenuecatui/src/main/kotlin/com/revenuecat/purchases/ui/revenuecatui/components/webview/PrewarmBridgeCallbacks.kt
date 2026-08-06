@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

/**
 * Sits between a prewarm-time [WebViewJavaScriptBridge] and the composition that later adopts it.
 *
 * A prewarmed document completes its handshake before any composition exists, so the resize and
 * terminal-failure callbacks it emits have nowhere to go. They are cached here and replayed on
 * [rebind]; without that the adopted component renders at its placeholder size until the content
 * happens to resize again.
 */
internal class PrewarmBridgeCallbacks {

    private var resizeHandler: (Int?, Int?) -> Unit = { _, _ -> }
    private var documentResetHandler: () -> Unit = {}
    private var loadFailedHandler: () -> Unit = {}

    private var lastWidthCssPx: Int? = null
    private var lastHeightCssPx: Int? = null
    private var pendingLoadFailed: Boolean = false

    fun dispatchResize(widthCssPx: Int?, heightCssPx: Int?) {
        widthCssPx?.let { lastWidthCssPx = it }
        heightCssPx?.let { lastHeightCssPx = it }
        resizeHandler(widthCssPx, heightCssPx)
    }

    fun dispatchDocumentReset() {
        lastWidthCssPx = null
        lastHeightCssPx = null
        documentResetHandler()
    }

    fun dispatchLoadFailed() {
        pendingLoadFailed = true
        loadFailedHandler()
    }

    /** Points the callbacks at the adopting composition's state setters and replays what it missed. */
    fun rebind(
        onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit,
        onDocumentReset: () -> Unit,
        onLoadFailed: () -> Unit,
    ) {
        resizeHandler = onContentResize
        documentResetHandler = onDocumentReset
        loadFailedHandler = onLoadFailed
        if (pendingLoadFailed) {
            onLoadFailed()
        } else if (lastWidthCssPx != null || lastHeightCssPx != null) {
            onContentResize(lastWidthCssPx, lastHeightCssPx)
        }
    }
}
