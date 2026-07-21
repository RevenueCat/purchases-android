@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * Bidirectional bridge between a Paywalls V2 `web_view` component and native code, one per rendered
 * `web_view`. Implements the `workflow-web-components-sdk` host contract on the `rc-web-components`
 * channel: JS→native via [WebViewCompat.addWebMessageListener], native→JS via
 * `window.__rcWebComponentsReceive` ([WebView.evaluateJavascript]).
 *
 * Inbound frames are trusted by the listener's `sourceOrigin` + `isMainFrame`, not the top-level URL.
 * Each main-frame navigation is a new document that must re-handshake: [onMainFrameNavigationStarted]
 * (from `WebViewClient.onPageStarted`) resets state and cancels the previous [documentScope]. The
 * bridge holds only a [WeakReference] to the [WebView] and goes inert after [release]. Main thread only.
 */
@Suppress("LongParameterList", "TooManyFunctions")
internal class WebViewJavaScriptBridge(
    webView: WebView,
    private val componentId: String,
    expectedUrl: String,
    private val sizeToContentWidth: Boolean = false,
    private val sizeToContentHeight: Boolean = false,
    private val onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit = { _, _ -> },
    private val onDocumentReset: () -> Unit = {},
    private val onSecureMessagingUnsupported: () -> Unit = {},
) {

    private val webViewRef = WeakReference(webView)
    private val expectedOrigin: String? = expectedUrl.toOriginOrNull()

    // The version this SDK build implements — deliberately NOT the schema's `protocol_version`:
    // never accept a handshake for a version we can't service.
    private val protocolVersion: Int = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION

    private var released: Boolean = false

    private var channelOpen: Boolean = false

    private var messageListenerInstalled: Boolean = false

    // No CoroutineExceptionHandler on purpose: don't swallow bugs in a security-sensitive path.
    private fun newDocumentScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Work for the current JS document; cancelled+replaced on navigation and on [release]. */
    private var documentScope: CoroutineScope = newDocumentScope()

    private var lastAppliedWidthCssPx: Int? = null
    private var lastAppliedHeightCssPx: Int? = null

    @VisibleForTesting
    internal val webMessageListener = WebViewCompat.WebMessageListener { _, message, sourceOrigin, isMainFrame, _ ->
        val data = message.data ?: return@WebMessageListener
        documentScope.launch {
            handleInboundMessage(
                json = data,
                sourceOrigin = sourceOrigin,
                isMainFrame = isMainFrame,
            )
        }
    }

    /**
     * Registers the secure message listener. Call once on WebView creation. Falls back to
     * [onSecureMessagingUnsupported] when [WebViewFeature.WEB_MESSAGE_LISTENER] is missing.
     */
    @MainThread
    fun attach() {
        val webView = webViewRef.get() ?: return
        val origin = expectedOrigin
        if (origin == null || !WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            onSecureMessagingUnsupported()
            return
        }
        WebViewCompat.addWebMessageListener(
            webView,
            WebViewEnvelope.NATIVE_OBJECT_NAME,
            setOf(origin),
            webMessageListener,
        )
        messageListenerInstalled = true
    }

    /**
     * Starts a new main-frame document (load, same-origin nav, or reload): resets handshake and
     * resize state so it can `connect` again.
     */
    @MainThread
    @Suppress("UnusedParameter")
    fun onMainFrameNavigationStarted(url: String?) {
        if (released) return
        documentScope.cancel()
        documentScope = newDocumentScope()
        channelOpen = false
        lastAppliedWidthCssPx = null
        lastAppliedHeightCssPx = null
        onDocumentReset()
    }

    /** Stops the bridge: no further inbound/outbound messages. Call from `AndroidView` onRelease. */
    @MainThread
    fun release() {
        released = true
        channelOpen = false
        documentScope.cancel()
        val webView = webViewRef.get()
        if (messageListenerInstalled && webView != null) {
            WebViewCompat.removeWebMessageListener(webView, WebViewEnvelope.NATIVE_OBJECT_NAME)
            messageListenerInstalled = false
        }
    }

    @MainThread
    @Suppress("ReturnCount")
    private fun handleInboundMessage(
        json: String,
        sourceOrigin: Uri?,
        isMainFrame: Boolean,
    ) {
        if (released) return

        if (!isSourceTrusted(sourceOrigin = sourceOrigin, isMainFrame = isMainFrame)) {
            Logger.w(
                "Dropping inbound web view message: source origin is not the resolved component " +
                    "origin or the frame is not the main frame.",
            )
            return
        }

        val envelope = WebViewEnvelope.parse(json) ?: run {
            Logger.w("Dropping inbound web view message: not a valid transport envelope.")
            return
        }

        when (envelope.kind) {
            WebViewEnvelope.KIND_CONNECT -> handleConnect(envelope)
            WebViewEnvelope.KIND_MESSAGE,
            WebViewEnvelope.KIND_REQUEST,
            -> handleAppFrame(envelope)
            else -> Unit
        }
    }

    @MainThread
    private fun handleConnect(envelope: WebViewEnvelope.Parsed) {
        if (channelOpen || released) return

        if (envelope.protocolVersion != protocolVersion) {
            deliverEnvelope(
                WebViewEnvelope.build(
                    kind = WebViewEnvelope.KIND_REJECT,
                    protocolVersion = protocolVersion,
                    componentId = "",
                    error = "Unsupported protocol_version ${envelope.protocolVersion}; " +
                        "native host supports $protocolVersion",
                ),
                allowBeforeNavigation = true,
            )
            return
        }

        channelOpen = true
        deliverEnvelope(
            WebViewEnvelope.build(
                kind = WebViewEnvelope.KIND_INIT,
                protocolVersion = protocolVersion,
                componentId = componentId,
            ),
            allowBeforeNavigation = true,
        )
        sendFitIfNeeded()
    }

    private fun sendFitIfNeeded() {
        if (!sizeToContentWidth && !sizeToContentHeight) return
        val payload = JSONObject().apply {
            if (sizeToContentWidth) put("width", true)
            if (sizeToContentHeight) put("height", true)
        }
        // Handshake frame (sent right after init) → shares init's allow-before-navigation exception.
        deliverEnvelope(
            WebViewEnvelope.build(
                kind = WebViewEnvelope.KIND_MESSAGE,
                protocolVersion = protocolVersion,
                componentId = componentId,
                type = WebViewMessageType.FIT,
                payload = payload,
            ),
            allowBeforeNavigation = true,
        )
    }

    @MainThread
    @Suppress("ReturnCount")
    private fun handleAppFrame(envelope: WebViewEnvelope.Parsed) {
        if (!channelOpen) return

        // A `request` frame must carry an id for response correlation; drop malformed ones.
        if (envelope.kind == WebViewEnvelope.KIND_REQUEST && envelope.id == null) {
            Logger.w("Dropping inbound web view message: request frame is missing an id.")
            return
        }

        // resize is the only app frame serviced in v1 (no message handler, no variables).
        if (envelope.type == WebViewMessageType.RESIZE) {
            handleResize(envelope)
        }
    }

    @MainThread
    private fun handleResize(envelope: WebViewEnvelope.Parsed) {
        if (envelope.componentId != componentId) return
        val payload = envelope.payload ?: return

        val width = applyResize(
            reported = payload.resizeDimension("width").takeIf { sizeToContentWidth },
            lastApplied = lastAppliedWidthCssPx,
        )?.also { lastAppliedWidthCssPx = it }
        val height = applyResize(
            reported = payload.resizeDimension("height").takeIf { sizeToContentHeight },
            lastApplied = lastAppliedHeightCssPx,
        )?.also { lastAppliedHeightCssPx = it }

        if (width != null || height != null) {
            onContentResize(width, height)
        }
    }

    /**
     * New value to apply for one axis, or `null` when unchanged within
     * [RESIZE_APPLY_THRESHOLD_CSS_PX] — guards resize report/apply feedback loops.
     */
    @Suppress("ReturnCount")
    private fun applyResize(reported: Int?, lastApplied: Int?): Int? {
        if (reported == null) return null
        if (lastApplied != null && abs(reported - lastApplied) < RESIZE_APPLY_THRESHOLD_CSS_PX) {
            return null
        }
        return reported
    }

    /**
     * Delivers a host-to-content frame. [allowBeforeNavigation] relaxes the outbound origin check for
     * handshake replies (`init`/`reject`/`fit`), whose triggering `connect` was already origin-gated
     * but may arrive before the top-level `url` is populated. Post-handshake sends must pass `false`.
     */
    private fun deliverEnvelope(envelope: JSONObject, allowBeforeNavigation: Boolean) {
        documentScope.launch {
            val webView = webViewRef.get() ?: return@launch
            // Defense in depth: drop outbound work if the top-level URL left the expected origin.
            if (!isCurrentUrlTrusted(webView, allowBeforeNavigation = allowBeforeNavigation)) {
                Logger.w(
                    "Dropping outbound web view message: current origin does not match the " +
                        "resolved component origin.",
                )
                return@launch
            }
            val payload = envelope.toString().escapeForJavaScript()
            webView.evaluateJavascript(
                "if (window.${WebViewEnvelope.RECEIVE_FUNCTION}) { " +
                    "window.${WebViewEnvelope.RECEIVE_FUNCTION}($payload); " +
                    "}",
                null,
            )
        }
    }

    /** Trusted iff the sender is the main frame on the resolved origin; subframes rely on server CSP. */
    @MainThread
    @Suppress("ReturnCount")
    private fun isSourceTrusted(sourceOrigin: Uri?, isMainFrame: Boolean): Boolean {
        if (!isMainFrame) return false
        if (expectedOrigin == null) return false
        val origin = sourceOrigin?.toOriginOrNull() ?: return false
        return origin == expectedOrigin
    }

    /** Outbound-only defense in depth: whether the top-level URL still has the expected origin. */
    @MainThread
    private fun isCurrentUrlTrusted(webView: WebView, allowBeforeNavigation: Boolean): Boolean {
        if (expectedOrigin == null) return false
        val currentOrigin = webView.url?.toOriginOrNull()
        return when {
            currentOrigin == null -> allowBeforeNavigation
            else -> currentOrigin == expectedOrigin
        }
    }

    private companion object {
        /** Cap a content-reported dimension so a buggy/hostile bundle can't blow up layout. */
        private const val MAX_RESIZE_CSS_PX = 10_000.0

        /** Minimum per-axis change (CSS px) before a new report is applied. */
        private const val RESIZE_APPLY_THRESHOLD_CSS_PX = 1

        /**
         * Clamped positive resize dimension, or `null` unless the value is a genuine finite JSON
         * number (a stringified `"300"` or a boolean is rejected, not coerced).
         */
        @Suppress("ReturnCount")
        fun JSONObject.resizeDimension(key: String): Int? {
            val value = (opt(key) as? Number)?.toDouble() ?: return null
            if (!value.isFinite() || value <= 0) return null
            return value.coerceAtMost(MAX_RESIZE_CSS_PX).toInt()
        }

        /** U+2028/U+2029 are valid in JSON strings but terminate JS statements; escape before embedding. */
        fun String.escapeForJavaScript(): String =
            replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    }
}
