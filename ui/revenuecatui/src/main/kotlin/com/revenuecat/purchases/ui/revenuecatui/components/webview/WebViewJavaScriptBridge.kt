@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.MainThread
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewController
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessageHandler
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.toJsonObject
import com.revenuecat.purchases.ui.revenuecatui.toJsonRepresentation
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URL

/**
 * Installs and drives the bidirectional bridge between a Paywalls V2 `web_view` component and native
 * code. One bridge is created per rendered `web_view`.
 *
 * ## Transport
 * This implementation uses [WebView.addJavascriptInterface] (the project does not depend on
 * `androidx.webkit`). Exactly one native method is exposed — [postMessage] — under the private object
 * name [NATIVE_OBJECT_NAME]. A small document-start shim (see [injectBridgeScript]) then exposes the
 * stable public surface `window.RevenueCatWebView.postMessage(message)`, accepting either an object
 * (serialized to JSON) or a JSON string, without overwriting an existing bridge.
 *
 * Because `addJavascriptInterface` provides no platform origin scoping, the bridge enforces the origin
 * itself: before delivering any inbound message it verifies the WebView's current URL still has the
 * same origin (scheme + host + port) as the resolved component URL, rejecting messages received after
 * navigation to an unexpected origin.
 *
 * ## Native → web
 * Native messages are delivered by invoking `window.__revenueCatReceiveMessage(message)` via
 * [WebView.evaluateJavascript], preserving the flat RFC envelope.
 *
 * ## Lifecycle & threading
 * The bridge holds only a [WeakReference] to the [WebView] (no Activity/Context), and stops delivering
 * messages once [release] is called. Inbound JavaScript callbacks arrive on a binder thread and are
 * hopped onto the main thread; app callbacks and all WebView interactions happen on the main thread.
 */
@Suppress("LongParameterList", "TooManyFunctions")
internal class WebViewJavaScriptBridge(
    webView: WebView,
    private val componentId: String,
    expectedUrl: URL,
    locale: String,
    colorScheme: WebViewColorScheme,
    customVariables: Map<String, CustomVariableValue>,
    messageHandler: PaywallWebViewMessageHandler?,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) : PaywallWebViewController {

    private val webViewRef = WeakReference(webView)
    private val expectedOrigin: String? = expectedUrl.toOriginOrNull()

    // Refreshed from the latest paywall state on every recomposition via update().
    @Volatile private var locale: String = locale

    @Volatile private var colorScheme: WebViewColorScheme = colorScheme

    @Volatile private var customVariables: Map<String, CustomVariableValue> = customVariables

    @Volatile private var messageHandler: PaywallWebViewMessageHandler? = messageHandler

    @Volatile private var released: Boolean = false

    /**
     * Registers the native interface on the WebView. Call once, when the WebView is created.
     */
    @MainThread
    fun attach() {
        webViewRef.get()?.addJavascriptInterface(this, NATIVE_OBJECT_NAME)
    }

    /**
     * Injects the `window.RevenueCatWebView` shim. Call from `WebViewClient.onPageStarted` so the
     * surface is available before the page's own scripts run.
     */
    @MainThread
    fun injectBridgeScript() {
        if (released) return
        webViewRef.get()?.evaluateJavascript(BRIDGE_SCRIPT, null)
    }

    /**
     * Refreshes the SDK-managed variable sources and the app message handler from the latest paywall
     * state. Call from `AndroidView`'s update block.
     */
    fun update(
        locale: String,
        colorScheme: WebViewColorScheme,
        customVariables: Map<String, CustomVariableValue>,
        messageHandler: PaywallWebViewMessageHandler?,
    ) {
        this.locale = locale
        this.colorScheme = colorScheme
        this.customVariables = customVariables
        this.messageHandler = messageHandler
    }

    /**
     * Stops the bridge. After this call no further inbound messages are delivered and no outbound
     * messages are sent. Call from `AndroidView`'s onRelease block.
     */
    @MainThread
    fun release() {
        released = true
        webViewRef.get()?.removeJavascriptInterface(NATIVE_OBJECT_NAME)
    }

    /**
     * Entry point for the injected `window.RevenueCatWebView`. Invoked on a binder thread; we hop to
     * the main thread before touching the WebView or validating the message.
     */
    @JavascriptInterface
    fun postMessage(json: String) {
        if (released) return
        mainHandler.post { handleInboundMessage(json) }
    }

    @MainThread
    @Suppress("ReturnCount")
    private fun handleInboundMessage(json: String) {
        if (released) return
        val webView = webViewRef.get() ?: return

        val currentOrigin = webView.url?.let { runCatching { URL(it).toOriginOrNull() }.getOrNull() }
        if (expectedOrigin == null || currentOrigin == null || currentOrigin != expectedOrigin) {
            Logger.w("Dropping web view message: current origin does not match the resolved component origin.")
            return
        }

        val message = WebViewMessageParser.parse(json, expectedComponentId = componentId) ?: return

        // On a request for variables, the SDK first sends its managed variables (and the paywall's
        // custom variables), then invokes the app handler so the app may add more under `custom`.
        if (message.type == WebViewMessageType.REQUEST_VARIABLES) {
            sendVariables(
                componentId = componentId,
                variables = PaywallWebViewVariablesProvider.sdkManagedVariables(
                    locale = locale,
                    colorScheme = colorScheme,
                    customVariables = customVariables,
                ),
                sanitize = false,
            )
        }

        messageHandler?.onMessage(message, this)
    }

    override fun postVariables(componentId: String, variables: Map<String, PaywallWebViewValue>) {
        sendVariables(componentId = componentId, variables = variables, sanitize = true)
    }

    override fun postMessage(componentId: String, type: String, message: Map<String, PaywallWebViewValue>) {
        val envelope = JSONObject().apply {
            put(WebViewMessageField.TYPE, type)
            put(WebViewMessageField.COMPONENT_ID, componentId)
            message.forEach { (key, value) ->
                // Don't let arbitrary fields overwrite the envelope identity fields.
                if (key != WebViewMessageField.TYPE && key != WebViewMessageField.COMPONENT_ID) {
                    put(key, value.toJsonRepresentation())
                }
            }
        }
        deliverToWebView(envelope)
    }

    private fun sendVariables(
        componentId: String,
        variables: Map<String, PaywallWebViewValue>,
        sanitize: Boolean,
    ) {
        val finalVariables = if (sanitize) {
            PaywallWebViewVariablesProvider.sanitizeAppProvidedVariables(variables)
        } else {
            variables
        }
        val envelope = JSONObject().apply {
            put(WebViewMessageField.TYPE, WebViewMessageType.VARIABLES)
            put(WebViewMessageField.COMPONENT_ID, componentId)
            put(WebViewMessageField.VARIABLES, finalVariables.toJsonObject())
        }
        deliverToWebView(envelope)
    }

    private fun deliverToWebView(envelope: JSONObject) {
        runOnMainThread {
            if (released) return@runOnMainThread
            val webView = webViewRef.get() ?: return@runOnMainThread
            val payload = envelope.toString().escapeForJavaScript()
            webView.evaluateJavascript(
                "if (window.$RECEIVE_FUNCTION) { window.$RECEIVE_FUNCTION($payload); }",
                null,
            )
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private companion object {
        // Private native object name. The public surface (window.RevenueCatWebView) is created by the
        // injected shim below; this avoids exposing the raw native interface to web content directly.
        const val NATIVE_OBJECT_NAME = "__RevenueCatNativeBridge"
        const val PUBLIC_OBJECT_NAME = "RevenueCatWebView"
        const val RECEIVE_FUNCTION = "__revenueCatReceiveMessage"

        val BRIDGE_SCRIPT = """
            (function() {
                if (window.$PUBLIC_OBJECT_NAME) { return; }
                var nativeBridge = window.$NATIVE_OBJECT_NAME;
                if (!nativeBridge) { return; }
                window.$PUBLIC_OBJECT_NAME = {
                    postMessage: function(message) {
                        nativeBridge.postMessage(
                            typeof message === 'string' ? message : JSON.stringify(message)
                        );
                    }
                };
            })();
        """.trimIndent()

        /**
         * JSON is a subset of JS object-literal syntax, but the U+2028/U+2029 separators are valid in
         * JSON strings yet terminate JS statements. Escape them so the payload is safe to embed.
         */
        fun String.escapeForJavaScript(): String =
            replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    }
}

/**
 * The origin of this URL as `scheme://host[:port]`, or `null` if it lacks a host. Only HTTPS URLs are
 * expected here (the resolver already enforces this), but the origin string includes the scheme so a
 * scheme change would also be detected.
 */
private fun URL.toOriginOrNull(): String? {
    val host = host?.takeIf { it.isNotBlank() } ?: return null
    val portSuffix = if (port == -1) "" else ":$port"
    return "$protocol://$host$portSuffix"
}
