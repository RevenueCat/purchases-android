@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import java.io.ByteArrayInputStream

@JvmSynthetic
@Composable
@Suppress("LongMethod")
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    if (!style.visible) return

    val resolvedUrl = remember(style.urlTemplate, state) {
        WebViewUrlResolver.resolve(style.urlTemplate, state)
    }
    if (resolvedUrl == null) {
        // The web view URL is missing or did not resolve to a valid HTTPS URL with a host, so we
        // render the fallback stack instead of rendering nothing.
        // Note: rendering the fallback when the web content fails to load at runtime
        // (WebViewClient.onReceivedError) is intentionally deferred to a future change.
        style.fallbackStackComponentStyle?.let { fallbackStyle ->
            StackComponentView(
                style = fallbackStyle,
                state = state,
                clickHandler = onClick,
                modifier = modifier,
                componentInteractionTracker = componentInteractionTracker,
            )
        }
        return
    }

    // Bidirectional messaging is always enabled for web_view (part of protocol_version 1). The bridge
    // is installed for every rendered web_view that has a canonical component id. Values that can change
    // across recompositions (color scheme, locale, custom variables, handler) are refreshed via update().
    val componentId = style.componentId
    val capabilities = style.capabilities
    val colorScheme = if (isSystemInDarkTheme()) WebViewColorScheme.DARK else WebViewColorScheme.LIGHT
    val locale = state.locale.toLanguageTag()
    val customVariables = state.mergedCustomVariables
    val messageHandler = state.webViewMessageHandler

    // A plain holder (not snapshot state) so the bridge created in factory can be refreshed in update
    // and torn down in onRelease without triggering recomposition.
    val bridgeHolder = remember { WebViewBridgeHolder() }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                val bridge = componentId?.let {
                    WebViewJavaScriptBridge(
                        webView = this,
                        componentId = it,
                        expectedUrl = resolvedUrl,
                        locale = locale,
                        colorScheme = colorScheme,
                        customVariables = customVariables,
                        messageHandler = messageHandler,
                    ).also { bridge -> bridge.attach() }
                }
                bridgeHolder.bridge = bridge
                configure(bridge, capabilities)
                loadUrl(resolvedUrl.toString())
            }
        },
        update = { webView ->
            bridgeHolder.bridge?.update(
                locale = locale,
                colorScheme = colorScheme,
                customVariables = customVariables,
                messageHandler = messageHandler,
            )
            if (webView.url != resolvedUrl.toString()) {
                webView.loadUrl(resolvedUrl.toString())
            }
        },
        onRelease = { webView ->
            // Stop delivering messages before tearing down, and avoid leaking the WebView.
            bridgeHolder.bridge?.release()
            bridgeHolder.bridge = null
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.webChromeClient = null
            webView.destroy()
        },
        modifier = modifier.size(style.size),
    )
}

/** Mutable holder for the per-WebView bridge instance, shared across factory/update/onRelease. */
private class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

@OptIn(InternalRevenueCatAPI::class)
private fun WebView.configure(bridge: WebViewJavaScriptBridge?, capabilities: WebViewComponent.Capabilities?) {
    setBackgroundColor(Color.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    settings.allowContentAccess = false
    settings.allowFileAccess = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.domStorageEnabled = true
    settings.javaScriptEnabled = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    // Explicitly off by default: geolocation is only enabled when the schema grants the capability.
    settings.setGeolocationEnabled(shouldGrantGeolocation(capabilities))
    // clipboard_write / clipboard_read are decode-only for v1: on Android both clipboard operations
    // are engine-handled and user-gesture-gated, with no WebChromeClient hook to wire up. Revisit
    // clipboard enforcement if a future Android API exposes per-WebView control.
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // Install the window.RevenueCatWebView shim before the page's own scripts run.
            bridge?.injectBridgeScript()
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return request.url.scheme != HTTPS_SCHEME
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            // Called on a background thread. Only immutable, read-only data is touched here.
            val networkAccess = capabilities?.networkAccess
            if (networkAccess != null &&
                !isRequestAllowed(request.url.host ?: "", networkAccess.allowedDomains)
            ) {
                return blockedResponse()
            }
            return super.shouldInterceptRequest(view, request)
        }
    }
    webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            val granted = grantedResources(request.resources, capabilities)
            if (granted.isNotEmpty()) request.grant(granted) else request.deny()
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback,
        ) {
            // retain = false so the decision is not persisted to disk.
            callback.invoke(origin, shouldGrantGeolocation(capabilities), false)
        }
    }
}

private fun blockedResponse() = WebResourceResponse(
    "text/plain",
    "utf-8",
    HTTP_STATUS_OK,
    "OK",
    emptyMap(),
    ByteArrayInputStream(ByteArray(0)),
)

private const val HTTPS_SCHEME = "https"
private const val HTTP_STATUS_OK = 200
