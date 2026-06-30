@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
@Suppress("LongMethod")
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    if (!style.visible) return

    // Key on state.locale (a derivedState over the paywall's mutable locale) as well: a locale change
    // mutates the same PaywallState instance in place, so without it the resolved URL — and the
    // key(resolvedUrl) below — would stay stale for a locale-dependent template.
    val resolvedUrl = remember(style.urlTemplate, state, state.locale) {
        WebViewUrlResolver.resolve(style.urlTemplate, state)
    }
    // The web view URL is missing or did not resolve to a valid HTTPS URL with a host. web_view
    // availability is gated by SDK version on the frontend, so a delivered web_view is expected to
    // always resolve; render nothing rather than crashing if it doesn't.
    if (resolvedUrl == null) return

    // Bidirectional messaging is always enabled for web_view (part of protocol_version 1). The bridge
    // is installed for every rendered web_view that has a canonical component id. Values that can change
    // across recompositions (locale, handler) are refreshed via update().
    val componentId = style.componentId
    // For v1, the presence of a protocol_version means the web content is isolated from external
    // sources via a fixed Content-Security-Policy. Legacy configs without it get no policy.
    val enforceContentSecurityPolicy = style.protocolVersion != null
    val locale = state.locale.toLanguageTag()
    val messageHandler = state.webViewMessageHandler

    // A plain holder (not snapshot state) so the bridge created in factory can be refreshed in update
    // and torn down in onRelease without triggering recomposition.
    val bridgeHolder = remember { WebViewBridgeHolder() }

    // Key on the resolved URL so the WebView (and its bridge) are created exactly once per intended URL.
    // We deliberately do NOT reload on every recomposition: in-page navigation changes WebView.url, and
    // reloading whenever it differs from resolvedUrl would reset a multi-step web flow. The WebView and
    // bridge are only recreated when the SDK-resolved URL itself changes (e.g. a locale-dependent
    // template), which also gives the bridge a fresh expected origin. Locale/handler changes that don't
    // change the URL are refreshed in place via update().
    key(resolvedUrl) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    val bridge = componentId?.let {
                        WebViewJavaScriptBridge(
                            webView = this,
                            componentId = it,
                            expectedUrl = resolvedUrl,
                            locale = locale,
                            messageHandler = messageHandler,
                        ).also { bridge -> bridge.attach() }
                    }
                    bridgeHolder.bridge = bridge
                    configure(bridge, enforceContentSecurityPolicy)
                    loadUrl(resolvedUrl.toString())
                }
            },
            update = {
                bridgeHolder.bridge?.update(
                    locale = locale,
                    messageHandler = messageHandler,
                )
            },
            onRelease = { webView ->
                // Stop delivering messages before tearing down, and avoid leaking the WebView.
                bridgeHolder.bridge?.release()
                bridgeHolder.bridge = null
                webView.stopLoading()
                webView.webViewClient = WebViewClient()
                webView.destroy()
            },
            modifier = modifier.size(style.size),
        )
    }
}

/** Mutable holder for the per-WebView bridge instance, shared across factory/update/onRelease. */
private class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

private fun WebView.configure(bridge: WebViewJavaScriptBridge?, enforceContentSecurityPolicy: Boolean) {
    setBackgroundColor(Color.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    settings.allowContentAccess = false
    settings.allowFileAccess = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.domStorageEnabled = true
    settings.javaScriptEnabled = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.setGeolocationEnabled(false)
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // Install the isolation Content-Security-Policy before any of the page's own resources or
            // scripts run, then install the window.RevenueCatWebView shim.
            if (enforceContentSecurityPolicy) {
                view.evaluateJavascript(contentSecurityPolicyMetaScript(), null)
            }
            bridge?.injectBridgeScript()
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return request.url.scheme != HTTPS_SCHEME
        }
    }
}

private const val HTTPS_SCHEME = "https"
