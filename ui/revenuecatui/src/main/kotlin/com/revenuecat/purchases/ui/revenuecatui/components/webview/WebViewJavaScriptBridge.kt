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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
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
     * Registers the secure message listener. Idempotent once installed; a no-op on later calls after
     * success. If installation fails — invalid expected origin (a config error) or missing
     * [WebViewFeature.WEB_MESSAGE_LISTENER] (an old System WebView) — it falls back to
     * [onSecureMessagingUnsupported], logged distinctly, and a later call will retry.
     */
    @MainThread
    @Suppress("ReturnCount")
    fun attach() {
        if (messageListenerInstalled) return
        val webView = webViewRef.get() ?: return
        val origin = expectedOrigin
        if (origin == null) {
            Logger.w(
                "Paywalls V2 web_view expected origin is not a valid origin; the bridge cannot " +
                    "verify message provenance and will reject all messages.",
            )
            onSecureMessagingUnsupported()
            return
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            Logger.d(
                "Paywalls V2 web_view secure messaging is unsupported on this System WebView " +
                    "(missing WEB_MESSAGE_LISTENER); falling back.",
            )
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
    fun onMainFrameNavigationStarted() {
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
            WebViewEnvelope.Kind.CONNECT -> handleConnect(envelope)
            WebViewEnvelope.Kind.MESSAGE,
            WebViewEnvelope.Kind.REQUEST,
            -> handleAppFrame(envelope)
            else -> Logger.w(
                "Dropping inbound web view message: unexpected envelope kind '${envelope.kind}'.",
            )
        }
    }

    @MainThread
    @Suppress("ReturnCount")
    private fun handleConnect(envelope: WebViewEnvelope) {
        if (channelOpen || released) return

        if (envelope.protocolVersion != protocolVersion) {
            deliverEnvelopeNow(
                WebViewEnvelope(
                    kind = WebViewEnvelope.Kind.REJECT,
                    protocolVersion = protocolVersion,
                    componentId = "",
                    error = "Unsupported protocol_version ${envelope.protocolVersion}; " +
                        "native host supports $protocolVersion",
                ),
                allowBeforeNavigation = true,
            )
            return
        }

        // Open the channel only once `init` has actually gone out (matches the web host's
        // post-then-open order): a handshake whose init is dropped by the outbound origin/webView
        // check can then be retried by a later `connect` instead of wedging the bridge half-open.
        val initDelivered = deliverEnvelopeNow(
            WebViewEnvelope(
                kind = WebViewEnvelope.Kind.INIT,
                protocolVersion = protocolVersion,
                componentId = componentId,
            ),
            allowBeforeNavigation = true,
        )
        if (!initDelivered) return

        channelOpen = true
        sendFitIfNeeded()
    }

    private fun sendFitIfNeeded() {
        if (!sizeToContentWidth && !sizeToContentHeight) return
        val payload = buildJsonObject {
            if (sizeToContentWidth) put("width", true)
            if (sizeToContentHeight) put("height", true)
        }
        // Handshake frame (sent right after init) → shares init's allow-before-navigation exception.
        deliverEnvelopeNow(
            WebViewEnvelope(
                kind = WebViewEnvelope.Kind.MESSAGE,
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
    private fun handleAppFrame(envelope: WebViewEnvelope) {
        if (!channelOpen) {
            Logger.w("Dropping inbound web view message: channel is not open.")
            return
        }

        // A `request` frame must carry an id for response correlation; drop malformed ones.
        if (envelope.kind == WebViewEnvelope.Kind.REQUEST && envelope.id == null) {
            Logger.w("Dropping inbound web view message: request frame is missing an id.")
            return
        }

        // resize is the only app frame serviced in v1 (no message handler, no variables).
        if (envelope.type == WebViewMessageType.RESIZE) {
            handleResize(envelope)
        } else {
            Logger.w("Dropping inbound web view message: unsupported message type '${envelope.type}'.")
        }
    }

    @MainThread
    private fun handleResize(envelope: WebViewEnvelope) {
        if (envelope.componentId != componentId) {
            Logger.w("Dropping inbound web view message: resize component id does not match.")
            return
        }
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
     * Delivers a host-to-content frame on the caller's main-thread coroutine; returns whether it went
     * out. Callers already run on [documentScope], and the handshake needs the result so [channelOpen]
     * flips only once `init` has actually been sent.
     *
     * [allowBeforeNavigation] relaxes the outbound origin check for handshake replies (`init`/`reject`/
     * `fit`), whose triggering `connect` was already origin-gated but may arrive before the top-level
     * `url` is populated. Post-handshake sends must pass `false`.
     */
    @MainThread
    @Suppress("ReturnCount")
    private fun deliverEnvelopeNow(envelope: WebViewEnvelope, allowBeforeNavigation: Boolean): Boolean {
        val webView = webViewRef.get() ?: return false
        // Defense in depth: drop outbound work if the top-level URL left the expected origin.
        if (!isCurrentUrlTrusted(webView, allowBeforeNavigation = allowBeforeNavigation)) {
            Logger.w(
                "Dropping outbound web view message: current origin does not match the " +
                    "resolved component origin.",
            )
            return false
        }
        val payload = envelope.toJsonString().escapeForJavaScript()
        webView.evaluateJavascript(
            "if (typeof window.${WebViewEnvelope.RECEIVE_FUNCTION} === 'function') { " +
                "window.${WebViewEnvelope.RECEIVE_FUNCTION}($payload); " +
                "}",
            null,
        )
        return true
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
        fun JsonObject.resizeDimension(key: String): Int? {
            val primitive = this[key] as? JsonPrimitive ?: return null
            if (primitive.isString) return null
            val value = primitive.doubleOrNull ?: return null
            if (!value.isFinite() || value <= 0) return null
            return value.coerceAtMost(MAX_RESIZE_CSS_PX).toInt()
        }

        /** U+2028/U+2029 are valid in JSON strings but terminate JS statements; escape before embedding. */
        fun String.escapeForJavaScript(): String =
            replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    }
}
