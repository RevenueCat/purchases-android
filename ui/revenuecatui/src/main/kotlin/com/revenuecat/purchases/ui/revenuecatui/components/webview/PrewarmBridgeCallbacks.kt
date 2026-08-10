@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

/**
 * Sits between a prewarm-time [WebViewJavaScriptBridge] and the composition that later adopts it.
 *
 * A prewarmed document completes its handshake before any composition exists, so the resize and
 * terminal-failure callbacks it emits have nowhere to go.
 *
 * A resize is cached and replayed on [rebind], so the adopted component starts at the size the content
 * already reported instead of its placeholder.
 *
 * A load failure is recorded in [loadFailed] but never replayed: [PaywallWebViewPrewarmer.take] refuses
 * an entry that failed, so the display path loads cold as it would have without prewarming. Replaying it
 * would turn a transient prewarm-time network failure into a permanent one for the component.
 */
internal class PrewarmBridgeCallbacks {

    private var resizeHandler: (Int?, Int?) -> Unit = { _, _ -> }
    private var documentResetHandler: () -> Unit = {}
    private var loadFailedHandler: () -> Unit = {}

    private var lastWidthCssPx: Int? = null
    private var lastHeightCssPx: Int? = null
    private var ignoreNextDocumentReset = false

    var loadFailed: Boolean = false
        private set

    fun dispatchResize(widthCssPx: Int?, heightCssPx: Int?) {
        widthCssPx?.let { lastWidthCssPx = it }
        heightCssPx?.let { lastHeightCssPx = it }
        resizeHandler(widthCssPx, heightCssPx)
    }

    fun dispatchDocumentReset() {
        if (ignoreNextDocumentReset) {
            ignoreNextDocumentReset = false
            return
        }
        lastWidthCssPx = null
        lastHeightCssPx = null
        documentResetHandler()
    }

    fun dispatchLoadFailed() {
        loadFailed = true
        loadFailedHandler()
    }

    /**
     * Called just before the activation load. Activation commits the document that is already loaded, so
     * the navigation it reports must not discard the size that same document already measured.
     */
    fun ignoreDocumentResetFromActivation() {
        ignoreNextDocumentReset = true
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
        if (lastWidthCssPx != null || lastHeightCssPx != null) {
            onContentResize(lastWidthCssPx, lastHeightCssPx)
        }
    }
}
