@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

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

    val resolvedUrl = remember(style.url) {
        WebViewUrlResolver.resolve(style.url)
    }
    if (resolvedUrl == null) return

    val componentId = style.componentId
    val locale = state.locale.toLanguageTag()
    val messageHandler = state.webViewMessageHandler
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
                            messageHandler = messageHandler,
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
                        expectedOrigin = resolvedUrl.toOriginOrNull(),
                        onMainFrameLoadFailed = { loadFailed = true },
                    )
                    loadUrl(resolvedUrl)
                }
            },
            update = {
                bridgeHolder.bridge?.update(
                    locale = locale,
                    messageHandler = messageHandler,
                )
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

/**
 * Placeholder sizes for a `fit` axis until the content reports its size over the bridge — a
 * WebView has no meaningful intrinsic size, so `fit` would otherwise collapse. 100 (height)
 * matches the iOS implementation; 300 (width) matches the web implementation's
 * `FIT_FALLBACK_SIZE_PX`.
 */
internal const val FIT_PLACEHOLDER_HEIGHT: UInt = 100u
internal const val FIT_PLACEHOLDER_WIDTH: UInt = 300u

internal fun webViewEffectiveSize(
    declaredSize: Size,
    contentWidthCssPx: Int,
    contentHeightCssPx: Int,
): Size {
    val width = when (declaredSize.width) {
        is Fit -> Fixed(if (contentWidthCssPx > 0) contentWidthCssPx.toUInt() else FIT_PLACEHOLDER_WIDTH)
        else -> declaredSize.width
    }
    val height = when (declaredSize.height) {
        is Fit -> Fixed(if (contentHeightCssPx > 0) contentHeightCssPx.toUInt() else FIT_PLACEHOLDER_HEIGHT)
        else -> declaredSize.height
    }
    return Size(width = width, height = height)
}

/** Mutable holder for the per-WebView bridge instance, shared across factory/update/onRelease. */
private class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

private fun WebView.configure(
    expectedOrigin: String?,
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
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return shouldBlockWebViewNavigation(
                url = request.url?.toString(),
                isMainFrame = request.isForMainFrame,
                expectedOrigin = expectedOrigin,
            )
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

private const val HTTP_ERROR_STATUS_MIN = 400
