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
 * Installs and drives the bidirectional bridge between a Paywalls V2 `web_view` component and native
 * code. One bridge is created per rendered `web_view`.
 *
 * ## Transport
 * Implements the native Android host contract from `workflow-web-components-sdk`:
 * - JS → native: `rcWebComponents.postMessage(jsonString)` via [WebViewCompat.addWebMessageListener]
 * - native → JS: `window.__rcWebComponentsReceive(data)` via [WebView.evaluateJavascript]
 * - Wire format: `{ channel: "rc-web-components", kind, protocol_version, component_id, … }`
 *
 * Inbound frames are validated against the message listener's `sourceOrigin` and `isMainFrame` —
 * the WebView's top-level URL alone is not sufficient (same-origin subframes and navigation races).
 *
 * ## Document lifecycle
 * Each main-frame navigation creates a new JavaScript document that must re-handshake. Call
 * [onMainFrameNavigationStarted] from `WebViewClient.onPageStarted` so a fresh `connect` is accepted
 * and work queued for the previous document's [documentScope] is cancelled.
 *
 * ## Lifecycle & threading
 * The bridge holds only a [WeakReference] to the [WebView] (no Activity/Context), and stops delivering
 * messages once [release] is called. All main-thread work for the current JavaScript document runs on
 * a per-document [CoroutineScope] (`Dispatchers.Main.immediate`); that scope is cancelled and replaced
 * on every main-frame navigation and cancelled permanently on [release], so work queued for a dead
 * document or a released bridge never runs. App callbacks and all WebView interactions happen on the
 * main thread.
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

    // The single protocol version this SDK build implements. Deliberately NOT the schema's
    // `protocol_version`: the envelope version is the wire+SDK major the CONTENT speaks, and the
    // host must never accept a handshake for a version it cannot service, even if a future schema
    // declares one.
    private val protocolVersion: Int = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION

    private var released: Boolean = false

    private var channelOpen: Boolean = false

    private var messageListenerInstalled: Boolean = false

    // Deliberately no CoroutineExceptionHandler: uncaught exceptions should crash like the old
    // Handler.post path — do not swallow bugs in a security-sensitive path.
    private fun newDocumentScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Scope for all main-thread work belonging to the CURRENT JavaScript document.
     * Cancelled and replaced on every main-frame navigation (see
     * [onMainFrameNavigationStarted]) and cancelled permanently on [release] —
     * cancellation is what guarantees that work queued for a dead document or a
     * released bridge never runs.
     */
    private var documentScope: CoroutineScope = newDocumentScope()

    // Last content sizes applied per fit axis (main thread only), for the resize apply-threshold.
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
     * Registers the secure message listener on the WebView. Call once, when the WebView is created.
     * If [WebViewFeature.WEB_MESSAGE_LISTENER] is unavailable, invokes [onSecureMessagingUnsupported]
     * so the host can take the terminal failure path.
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
     * Marks the start of a new main-frame JavaScript document (initial load, same-origin
     * navigation, or reload). Resets the handshake and fit/resize threshold state so the new
     * document can `connect` again.
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

    /**
     * Stops the bridge. After this call no further inbound messages are delivered and no outbound
     * messages are sent. Call from `AndroidView`'s onRelease block.
     */
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

    /**
     * Processes an inbound transport frame. Production traffic arrives via [webMessageListener].
     */
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
        )
        sendFitIfNeeded()
    }

    private fun sendFitIfNeeded() {
        if (!sizeToContentWidth && !sizeToContentHeight) return
        val payload = JSONObject().apply {
            if (sizeToContentWidth) put("width", true)
            if (sizeToContentHeight) put("height", true)
        }
        deliverEnvelope(
            WebViewEnvelope.build(
                kind = WebViewEnvelope.KIND_MESSAGE,
                protocolVersion = protocolVersion,
                componentId = componentId,
                type = WebViewMessageType.FIT,
                payload = payload,
            ),
        )
    }

    @MainThread
    @Suppress("ReturnCount")
    private fun handleAppFrame(envelope: WebViewEnvelope.Parsed) {
        if (!channelOpen) return

        // `resize` is the only app frame the SDK services in v1; it drives content-fit sizing.
        // Everything else is ignored (no app-facing message handler, no variables).
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
     * Returns the value to apply for one axis, or `null` when there is nothing new to apply:
     * missing/invalid report, non-fit axis, or a change smaller than [RESIZE_APPLY_THRESHOLD_CSS_PX]
     * against the last applied value (guards report/apply feedback loops — HTML content's reported
     * width often just echoes the imposed viewport width).
     */
    @Suppress("ReturnCount")
    private fun applyResize(reported: Int?, lastApplied: Int?): Int? {
        if (reported == null) return null
        if (lastApplied != null && abs(reported - lastApplied) < RESIZE_APPLY_THRESHOLD_CSS_PX) {
            return null
        }
        return reported
    }

    private fun deliverEnvelope(envelope: JSONObject) {
        documentScope.launch {
            val webView = webViewRef.get() ?: return@launch
            val kind = envelope.optString(WebViewMessageField.KIND)
            val allowBeforeNavigation = kind in HANDSHAKE_OUTBOUND_KINDS
            // Defense in depth: drop outbound work if the top-level URL left the expected origin
            // (e.g. after a policy hole). Inbound validation uses sourceOrigin separately.
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

    /**
     * Whether [sourceOrigin] matches the resolved component origin and the frame is the main frame.
     * Subframe messages are always rejected — isolation for those is expected from the server CSP.
     */
    @MainThread
    @Suppress("ReturnCount")
    private fun isSourceTrusted(sourceOrigin: Uri?, isMainFrame: Boolean): Boolean {
        if (!isMainFrame) return false
        if (expectedOrigin == null) return false
        val origin = sourceOrigin?.toOriginOrNull() ?: return false
        return origin == expectedOrigin
    }

    /**
     * Whether the WebView's current top-level URL still has the expected origin. Used only as an
     * outbound defense-in-depth check; inbound traffic is gated by [isSourceTrusted].
     */
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
        private val HANDSHAKE_OUTBOUND_KINDS: Set<String> = setOf(
            WebViewEnvelope.KIND_INIT,
            WebViewEnvelope.KIND_REJECT,
        )

        /** Cap a content-reported dimension so a buggy/hostile bundle can't blow up layout. */
        private const val MAX_RESIZE_CSS_PX = 10_000.0

        /** Minimum per-axis change (CSS px) before a new report is applied. */
        private const val RESIZE_APPLY_THRESHOLD_CSS_PX = 1

        /** A validated, clamped resize dimension, or `null` when absent, non-finite, or not positive. */
        @Suppress("ReturnCount")
        fun JSONObject.resizeDimension(key: String): Int? {
            if (!has(key)) return null
            val value = optDouble(key)
            if (!value.isFinite() || value <= 0) return null
            return value.coerceAtMost(MAX_RESIZE_CSS_PX).toInt()
        }

        /**
         * JSON is a subset of JS object-literal syntax, but the U+2028/U+2029 separators are valid in
         * JSON strings yet terminate JS statements. Escape them so the payload is safe to embed.
         */
        fun String.escapeForJavaScript(): String =
            replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    }
}
