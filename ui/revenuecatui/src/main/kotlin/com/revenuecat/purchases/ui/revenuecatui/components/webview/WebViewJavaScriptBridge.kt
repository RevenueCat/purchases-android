@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.MainThread
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewController
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessageHandler
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.toJsonObject
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Installs and drives the bidirectional bridge between a Paywalls V2 `web_view` component and native
 * code. One bridge is created per rendered `web_view`.
 *
 * ## Transport
 * Implements the native Android host contract from `workflow-web-components-sdk`:
 * - JS → native: `window.rcWebComponents.postMessage(jsonString)` via [addJavascriptInterface]
 * - native → JS: `window.__rcWebComponentsReceive(data)` via [WebView.evaluateJavascript]
 * - Wire format: `{ channel: "rc-web-components", kind, protocol_version, component_id, … }`
 *
 * The content SDK (`window.RC`) auto-detects Android when [NATIVE_OBJECT_NAME] is present and runs
 * the same connect/init handshake as on web. No document-start shim is injected.
 *
 * Because `addJavascriptInterface` provides no platform origin scoping, the bridge enforces the origin
 * itself: before delivering any inbound or outbound message it verifies the WebView's current URL still
 * has the same origin (scheme + host + port) as the resolved component URL, rejecting messages received
 * after navigation to an unexpected origin. Origin is always re-checked on the main thread at delivery
 * time.
 *
 * ## Lifecycle & threading
 * The bridge holds only a [WeakReference] to the [WebView] (no Activity/Context), and stops delivering
 * messages once [release] is called. Inbound JavaScript callbacks arrive on a binder thread and are
 * hopped onto the main thread; app callbacks and all WebView interactions happen on the main thread.
 * Outbound [WebView.evaluateJavascript] calls queued before [release] are dropped when they run.
 */
@Suppress("LongParameterList", "TooManyFunctions")
internal class WebViewJavaScriptBridge(
    webView: WebView,
    private val componentId: String,
    expectedUrl: String,
    locale: String,
    messageHandler: PaywallWebViewMessageHandler?,
    private val sizeToContentWidth: Boolean = false,
    private val sizeToContentHeight: Boolean = false,
    private val onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit = { _, _ -> },
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) : PaywallWebViewController {

    private val webViewRef = WeakReference(webView)
    private val expectedOrigin: String? = expectedUrl.toOriginOrNull()

    // The single protocol version this SDK build implements. Deliberately NOT the schema's
    // `protocol_version`: the envelope version is the wire+SDK major the CONTENT speaks, and the
    // host must never accept a handshake for a version it cannot service, even if a future schema
    // declares one.
    private val protocolVersion: Int = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION

    // Refreshed from the latest paywall state on every recomposition via update().
    @Volatile private var locale: String = locale

    @Volatile private var messageHandler: PaywallWebViewMessageHandler? = messageHandler

    @Volatile private var released: Boolean = false

    @Volatile private var channelOpen: Boolean = false

    // Last content sizes applied per fit axis (main thread only), for the resize apply-threshold.
    private var lastAppliedWidthCssPx: Int? = null
    private var lastAppliedHeightCssPx: Int? = null

    /**
     * Registers the native interface on the WebView. Call once, when the WebView is created.
     */
    @MainThread
    fun attach() {
        webViewRef.get()?.addJavascriptInterface(this, WebViewEnvelope.NATIVE_OBJECT_NAME)
    }

    /**
     * Refreshes the locale and the app message handler from the latest paywall state. Call from
     * `AndroidView`'s update block.
     */
    fun update(
        locale: String,
        messageHandler: PaywallWebViewMessageHandler?,
    ) {
        this.locale = locale
        this.messageHandler = messageHandler
    }

    /**
     * Stops the bridge. After this call no further inbound messages are delivered and no outbound
     * messages are sent. Call from `AndroidView`'s onRelease block.
     */
    @MainThread
    fun release() {
        released = true
        channelOpen = false
        webViewRef.get()?.removeJavascriptInterface(WebViewEnvelope.NATIVE_OBJECT_NAME)
    }

    /**
     * Entry point for `window.rcWebComponents.postMessage`. Invoked on a binder thread; we hop to the
     * main thread before touching the WebView or validating the message.
     */
    @JavascriptInterface
    fun postMessage(json: String) {
        mainHandler.post {
            if (released) return@post
            handleInboundMessage(json)
        }
    }

    @MainThread
    @Suppress("ReturnCount")
    private fun handleInboundMessage(json: String) {
        if (released) return
        val webView = webViewRef.get() ?: return

        if (json.toByteArray(Charsets.UTF_8).size > WebViewMessageParser.MAX_PAYLOAD_BYTES) {
            Logger.w(
                "Dropping inbound web view message: payload exceeds " +
                    "${WebViewMessageParser.MAX_PAYLOAD_BYTES} bytes.",
            )
            return
        }

        val envelope = WebViewEnvelope.parse(json) ?: run {
            Logger.w("Dropping inbound web view message: not a valid transport envelope.")
            return
        }

        when (envelope.kind) {
            WebViewEnvelope.KIND_CONNECT -> {
                if (!isOriginTrusted(webView, allowBeforeNavigation = true)) {
                    Logger.w(
                        "Dropping inbound web view connect: current origin does not match the " +
                            "resolved component origin.",
                    )
                    return
                }
                handleConnect(envelope)
            }
            WebViewEnvelope.KIND_MESSAGE,
            WebViewEnvelope.KIND_REQUEST,
            -> {
                if (!isOriginTrusted(webView, allowBeforeNavigation = false)) {
                    Logger.w(
                        "Dropping inbound web view message: current origin does not match the " +
                            "resolved component origin.",
                    )
                    return
                }
                handleAppFrame(envelope)
            }
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

        // Any transport `request` without a correlation `id` is undeliverable — drop it before
        // any handling, matching iOS and the web host.
        if (envelope.kind == WebViewEnvelope.KIND_REQUEST && envelope.id == null) {
            Logger.w("Dropping inbound web view message: transport request is missing 'id'.")
            return
        }

        // `resize` is SDK-internal regardless of kind; it never reaches the app handler.
        if (envelope.type == WebViewMessageType.RESIZE) {
            handleResize(envelope)
            return
        }

        val parsed = WebViewMessageParser.parse(
            envelope = envelope,
            expectedComponentId = componentId,
        ) ?: return

        val message = parsed.message

        if (message.type == WebViewMessageType.REQUEST_VARIABLES) {
            val variables = PaywallWebViewVariablesProvider.sdkManagedVariables(locale = locale)
            val requestId = parsed.requestId
            if (requestId != null) {
                deliverEnvelope(
                    WebViewEnvelope.build(
                        kind = WebViewEnvelope.KIND_RESPONSE,
                        protocolVersion = protocolVersion,
                        componentId = componentId,
                        type = parsed.requestType,
                        id = requestId,
                        payload = variables.toJsonObject(),
                    ),
                )
            } else {
                postVariablesMessage(componentId = componentId, variables = variables)
            }
        }

        messageHandler?.onMessage(message, this)
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
        if (lastApplied != null && kotlin.math.abs(reported - lastApplied) < RESIZE_APPLY_THRESHOLD_CSS_PX) {
            return null
        }
        return reported
    }

    override fun postVariables(componentId: String, variables: Map<String, PaywallWebViewValue>) {
        postVariablesMessage(
            componentId = componentId,
            variables = PaywallWebViewVariablesProvider.sanitizeAppProvidedVariables(variables),
        )
    }

    override fun postMessage(componentId: String, type: String, variables: Map<String, PaywallWebViewValue>) {
        postAppMessage(
            componentId = componentId,
            type = type,
            payload = variables.toJsonObject(),
        )
    }

    private fun postVariablesMessage(componentId: String, variables: Map<String, PaywallWebViewValue>) {
        postAppMessage(
            componentId = componentId,
            type = WebViewMessageType.VARIABLES,
            payload = variables.toJsonObject(),
        )
    }

    private fun postAppMessage(componentId: String, type: String, payload: JSONObject) {
        if (!channelOpen) return
        deliverEnvelope(
            WebViewEnvelope.build(
                kind = WebViewEnvelope.KIND_MESSAGE,
                protocolVersion = protocolVersion,
                componentId = componentId,
                type = type,
                payload = payload,
            ),
        )
    }

    private fun deliverEnvelope(envelope: JSONObject) {
        runOnMainThread {
            if (released) return@runOnMainThread
            val webView = webViewRef.get() ?: return@runOnMainThread
            val kind = envelope.optString(WebViewMessageField.KIND)
            val allowBeforeNavigation = kind in HANDSHAKE_OUTBOUND_KINDS
            if (!isOriginTrusted(webView, allowBeforeNavigation = allowBeforeNavigation)) {
                Logger.w(
                    "Dropping outbound web view message: current origin does not match the resolved component origin.",
                )
                return@runOnMainThread
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
     * Whether the WebView's current URL still has the same origin (scheme + host + port) as the
     * resolved component URL. When [allowBeforeNavigation] is true and the WebView has not committed
     * a URL yet, the expected origin is trusted so the connect/init handshake can complete before
     * the first navigation.
     */
    @MainThread
    private fun isOriginTrusted(webView: WebView, allowBeforeNavigation: Boolean): Boolean {
        if (expectedOrigin == null) return false
        val currentOrigin = webView.url?.toOriginOrNull()
        return when {
            currentOrigin == null -> allowBeforeNavigation
            else -> currentOrigin == expectedOrigin
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (released) return
            block()
        } else {
            mainHandler.post {
                if (released) return@post
                block()
            }
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

/**
 * The origin of this URL as `scheme://host[:port]`, or `null` if it lacks a host. Host comparison is
 * case-insensitive. Default ports (443 for https, 80 for http) are omitted.
 */
@Suppress("ReturnCount", "MagicNumber")
internal fun String.toOriginOrNull(): String? {
    val uri = Uri.parse(this)
    val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
    val host = uri.host?.takeIf { it.isNotBlank() }?.lowercase(Locale.US) ?: return null
    val effectivePort = when {
        uri.port != -1 -> uri.port
        scheme == "https" -> 443
        scheme == "http" -> 80
        else -> -1
    }
    val portSuffix = when {
        effectivePort == -1 -> ""
        scheme == "https" && effectivePort == 443 -> ""
        scheme == "http" && effectivePort == 80 -> ""
        else -> ":$effectivePort"
    }
    return "$scheme://$host$portSuffix"
}

/**
 * Navigation policy for `web_view` content, applied from `shouldOverrideUrlLoading`:
 *
 * - Any non-HTTPS navigation is blocked (any frame).
 * - Main-frame navigation is additionally restricted to the resolved component URL's origin
 *   (same-origin different-path navigation stays allowed). This makes cross-origin message races
 *   structurally impossible; the bridge's per-message origin check remains as defense in depth.
 * - Cross-origin sub-frame loads are left to the Content-Security-Policy.
 */
@Suppress("ReturnCount")
internal fun shouldBlockWebViewNavigation(
    url: String?,
    isMainFrame: Boolean,
    expectedOrigin: String?,
): Boolean {
    val origin = url?.toOriginOrNull() ?: return true
    if (!origin.startsWith("https://")) return true
    return isMainFrame && origin != expectedOrigin
}
