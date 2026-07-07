@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

@JvmSynthetic
@Composable
@Suppress("LongMethod", "ReturnCount")
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    onClick: suspend (PaywallAction) -> Unit = {},
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    if (!style.visible) return

    val resolvedUrl = remember(style.urlTemplate, state, state.locale) {
        WebViewUrlResolver.resolve(style.urlTemplate, state)
    }
    if (resolvedUrl == null) return

    val componentId = style.componentId
    val enforceContentSecurityPolicy = style.protocolVersion != null
    val locale = state.locale.toLanguageTag()
    val sizeToContentWidth = style.size.width is Fit
    val sizeToContentHeight = style.size.height is Fit

    var contentWidthCssPx by remember(resolvedUrl) { mutableIntStateOf(0) }
    var contentHeightCssPx by remember(resolvedUrl) { mutableIntStateOf(0) }
    var loadFailed by remember(resolvedUrl) { mutableStateOf(false) }
    val bridgeHolder = remember { WebViewBridgeHolder() }

    val effectiveSize = remember(style.size, contentWidthCssPx, contentHeightCssPx) {
        webViewEffectiveSize(
            declaredSize = style.size,
            contentWidthCssPx = contentWidthCssPx,
            contentHeightCssPx = contentHeightCssPx,
        )
    }

    val fallbackStyle = style.fallbackStackComponentStyle
    if (loadFailed && fallbackStyle != null) {
        StackComponentView(
            style = fallbackStyle,
            state = state,
            clickHandler = onClick,
            componentInteractionTracker = componentInteractionTracker,
            modifier = modifier.size(effectiveSize),
        )
        return
    }

    key(resolvedUrl) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    val bridge = componentId?.let { id ->
                        WebViewJavaScriptBridge(
                            webView = this,
                            componentId = id,
                            expectedUrl = resolvedUrl,
                            locale = locale,
                            messageHandler = null,
                            protocolVersion = style.protocolVersion ?: WebViewEnvelope.DEFAULT_PROTOCOL_VERSION,
                            sizeToContentWidth = sizeToContentWidth,
                            sizeToContentHeight = sizeToContentHeight,
                            onContentResize = { widthCssPx, heightCssPx ->
                                widthCssPx?.takeIf { it > 0 }?.let { contentWidthCssPx = it }
                                heightCssPx?.takeIf { it > 0 }?.let { contentHeightCssPx = it }
                            },
                        ).also { createdBridge -> createdBridge.attach() }
                    }
                    bridgeHolder.bridge = bridge
                    configure(
                        enforceContentSecurityPolicy = enforceContentSecurityPolicy,
                        onMainFrameLoadFailed = { loadFailed = true },
                    )
                    loadUrl(resolvedUrl.toString())
                }
            },
            update = {
                bridgeHolder.bridge?.update(locale = locale, messageHandler = null)
            },
            onRelease = { webView ->
                bridgeHolder.bridge?.release()
                bridgeHolder.bridge = null
                webView.stopLoading()
                webView.webViewClient = WebViewClient()
                webView.destroy()
            },
            modifier = modifier.size(effectiveSize),
        )
    }
}

private fun webViewEffectiveSize(
    declaredSize: Size,
    contentWidthCssPx: Int,
    contentHeightCssPx: Int,
): Size {
    val width = when (val declaredWidth = declaredSize.width) {
        is Fit -> if (contentWidthCssPx > 0) Fixed(contentWidthCssPx.toUInt()) else declaredWidth
        else -> declaredWidth
    }
    val height = when (val declaredHeight = declaredSize.height) {
        is Fit -> if (contentHeightCssPx > 0) Fixed(contentHeightCssPx.toUInt()) else declaredHeight
        else -> declaredHeight
    }
    return Size(width = width, height = height)
}

/** Mutable holder for the per-WebView bridge instance, shared across factory/update/onRelease. */
private class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

private fun WebView.configure(
    enforceContentSecurityPolicy: Boolean,
    onMainFrameLoadFailed: () -> Unit,
) {
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
            if (enforceContentSecurityPolicy) {
                view.evaluateJavascript(contentSecurityPolicyMetaScript(), null)
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return request.url.scheme != HTTPS_SCHEME
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) {
                onMainFrameLoadFailed()
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            if (request.isForMainFrame && errorResponse.statusCode >= HTTP_ERROR_STATUS_MIN) {
                onMainFrameLoadFailed()
            }
        }
    }
}

private const val HTTPS_SCHEME = "https"
private const val HTTP_ERROR_STATUS_MIN = 400
