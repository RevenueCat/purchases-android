@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
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
 * and outbound work queued for a previous document generation is dropped.
 *
 * ## Lifecycle & threading
 * The bridge holds only a [WeakReference] to the [WebView] (no Activity/Context), and stops delivering
 * messages once [release] is called. Inbound callbacks are hopped onto the main thread; app callbacks
 * and all WebView interactions happen on the main thread.
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
    private val onDocumentReset: () -> Unit = {},
    private val onSecureMessagingUnsupported: () -> Unit = {},
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val isWebMessageListenerSupported: () -> Boolean = {
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
    },
    private val installWebMessageListener: (
        WebView,
        String,
        Set<String>,
        WebViewCompat.WebMessageListener,
    ) -> Unit = { view, jsObjectName, allowedOrigins, listener ->
        WebViewCompat.addWebMessageListener(view, jsObjectName, allowedOrigins, listener)
    },
    private val uninstallWebMessageListener: (WebView, String) -> Unit = { view, jsObjectName ->
        WebViewCompat.removeWebMessageListener(view, jsObjectName)
    },
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

    @Volatile private var messageListenerInstalled: Boolean = false

    /**
     * Incremented on every main-frame document start. Outbound [WebView.evaluateJavascript] work
     * captures the generation at enqueue time and is dropped if a newer document has started.
     */
    private var documentGeneration: Int = 0

    // Last content sizes applied per fit axis (main thread only), for the resize apply-threshold.
    private var lastAppliedWidthCssPx: Int? = null
    private var lastAppliedHeightCssPx: Int? = null

    private val webMessageListener = WebViewCompat.WebMessageListener { _, message, sourceOrigin, isMainFrame, _ ->
        val data = message.data ?: return@WebMessageListener
        mainHandler.post {
            if (released) return@post
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
     * so the host can take the terminal failure/fallback path.
     */
    @MainThread
    fun attach() {
        val webView = webViewRef.get() ?: return
        val origin = expectedOrigin
        if (origin == null || !isWebMessageListenerSupported()) {
            onSecureMessagingUnsupported()
            return
        }
        installWebMessageListener(
            webView,
            WebViewEnvelope.NATIVE_OBJECT_NAME,
            setOf(origin),
            webMessageListener,
        )
        messageListenerInstalled = true
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
     * Marks the start of a new main-frame JavaScript document (initial load, same-origin
     * navigation, or reload). Resets the handshake and fit/resize threshold state so the new
     * document can `connect` again.
     */
    @MainThread
    @Suppress("UnusedParameter")
    fun onMainFrameNavigationStarted(url: String?) {
        if (released) return
        documentGeneration += 1
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
        val webView = webViewRef.get()
        if (messageListenerInstalled && webView != null) {
            uninstallWebMessageListener(webView, WebViewEnvelope.NATIVE_OBJECT_NAME)
            messageListenerInstalled = false
        }
    }

    /**
     * Processes an inbound transport frame. Production traffic arrives via [webMessageListener];
     * tests call this directly with an explicit [sourceOrigin] and [isMainFrame].
     */
    @MainThread
    @Suppress("ReturnCount")
    internal fun handleInboundMessage(
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
            WebViewEnvelope.KIND_CONNECT -> handleConnect(envelope)
            WebViewEnvelope.KIND_MESSAGE,
            WebViewEnvelope.KIND_REQUEST,
            -> handleAppFrame(envelope)
            else -> Unit
        }
    }

    /**
     * Test helper: posts [json] onto the main thread as a trusted main-frame message from
     * [expectedOrigin], mirroring the WebMessageListener hop.
     */
    internal fun postMessage(json: String) {
        val originUri = expectedOrigin?.let { Uri.parse(it) }
        mainHandler.post {
            if (released) return@post
            handleInboundMessage(json = json, sourceOrigin = originUri, isMainFrame = true)
        }
    }

    /**
     * Test helper for explicit origin / main-frame combinations.
     */
    internal fun postMessage(
        json: String,
        sourceOrigin: Uri?,
        isMainFrame: Boolean,
    ) {
        mainHandler.post {
            if (released) return@post
            handleInboundMessage(json = json, sourceOrigin = sourceOrigin, isMainFrame = isMainFrame)
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
        val generationAtEnqueue = documentGeneration
        runOnMainThread {
            if (released) return@runOnMainThread
            if (generationAtEnqueue != documentGeneration) return@runOnMainThread
            val webView = webViewRef.get() ?: return@runOnMainThread
            val kind = envelope.optString(WebViewMessageField.KIND)
            val allowBeforeNavigation = kind in HANDSHAKE_OUTBOUND_KINDS
            // Defense in depth: drop outbound work if the top-level URL left the expected origin
            // (e.g. after a policy hole). Inbound validation uses sourceOrigin separately.
            if (!isCurrentUrlTrusted(webView, allowBeforeNavigation = allowBeforeNavigation)) {
                Logger.w(
                    "Dropping outbound web view message: current origin does not match the " +
                        "resolved component origin.",
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
 *
 * Accepts both full URLs and bare origins (as provided by [WebViewCompat.WebMessageListener]).
 */
@Suppress("ReturnCount", "MagicNumber")
internal fun String.toOriginOrNull(): String? {
    val uri = Uri.parse(this)
    return uri.toOriginOrNull()
}

/**
 * The origin of this [Uri] as `scheme://host[:port]`, or `null` if it lacks a host.
 */
@Suppress("ReturnCount", "MagicNumber")
internal fun Uri.toOriginOrNull(): String? {
    val scheme = scheme?.lowercase(Locale.US) ?: return null
    val host = host?.takeIf { it.isNotBlank() }?.lowercase(Locale.US) ?: return null
    val effectivePort = when {
        port != -1 -> port
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
 * - Cross-origin sub-frame loads are not blocked here; isolation for those is expected from the
 *   server-provided Content-Security-Policy served with the web content.
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
