@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Color
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
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@JvmSynthetic
@Composable
// `state` is unused in v1 (no variables passed into the content) but kept for the uniform
// ComponentView signature; it carries locale/variables again when that lands.
@Suppress("LongMethod", "ReturnCount", "UnusedParameter")
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    if (!style.visible) return

    val resolvedUrl = remember(style.url) {
        WebViewUrlResolver.resolve(style.url)
    }
    if (resolvedUrl == null) return

    val componentId = style.componentId
    // workflow-web-components-sdk requires a host-assigned component id for the handshake.
    if (componentId.isBlank()) return
    val sizeToContentWidth = style.size.width is Fit
    val sizeToContentHeight = style.size.height is Fit

    val identity = WebViewIdentity(
        resolvedUrl = resolvedUrl,
        componentId = componentId,
        sizeToContentWidth = sizeToContentWidth,
        sizeToContentHeight = sizeToContentHeight,
    )

    // Identity-scoped state: when any immutable bridge field changes, Compose disposes the previous
    // key subtree (WebView + bridge + measured sizes + failure flag) and creates a fresh one.
    key(identity) {
        var contentWidthCssPx by remember { mutableIntStateOf(0) }
        var contentHeightCssPx by remember { mutableIntStateOf(0) }
        var loadFailed by remember { mutableStateOf(false) }
        // Holder is remembered inside the identity key so an old AndroidView.onRelease can only
        // clear/release the bridge that belonged to that specific view instance.
        val bridgeHolder = remember { WebViewBridgeHolder() }
        val failureFlag = remember { LoadFailureFlag() }

        val effectiveSize = remember(style.size, contentWidthCssPx, contentHeightCssPx) {
            webViewEffectiveSize(
                declaredSize = style.size,
                contentWidthCssPx = contentWidthCssPx,
                contentHeightCssPx = contentHeightCssPx,
            )
        }

        if (!loadFailed) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        val bridge = WebViewJavaScriptBridge(
                            webView = this,
                            componentId = componentId,
                            expectedUrl = resolvedUrl,
                            sizeToContentWidth = sizeToContentWidth,
                            sizeToContentHeight = sizeToContentHeight,
                            onContentResize = { widthCssPx, heightCssPx ->
                                widthCssPx?.takeIf { it > 0 }?.let { contentWidthCssPx = it }
                                heightCssPx?.takeIf { it > 0 }?.let { contentHeightCssPx = it }
                            },
                            onDocumentReset = {
                                contentWidthCssPx = 0
                                contentHeightCssPx = 0
                            },
                            onSecureMessagingUnsupported = {
                                if (failureFlag.markFailed()) {
                                    loadFailed = true
                                }
                            },
                        ).also { createdBridge -> createdBridge.attach() }
                        bridgeHolder.bridge = bridge
                        configure(
                            expectedOrigin = resolvedUrl.toOriginOrNull(),
                            onMainFrameNavigationStarted = { url ->
                                bridgeHolder.bridge?.onMainFrameNavigationStarted(url)
                            },
                            onMainFrameLoadFailed = {
                                if (failureFlag.markFailed()) {
                                    loadFailed = true
                                }
                            },
                        )
                        loadUrl(resolvedUrl)
                    }
                },
                onRelease = { webView ->
                    // Only release the bridge that this view installed into this holder.
                    val bridge = bridgeHolder.bridge
                    bridgeHolder.bridge = null
                    bridge?.release()
                    webView.stopLoading()
                    webView.webViewClient = WebViewClient()
                    webView.destroy()
                },
                modifier = modifier.size(effectiveSize),
            )
        }
        // Terminal failure: render nothing rather than retain a dead/unusable WebView.
        // There is intentionally no native fallback stack — apps must use an SDK that
        // supports web_view.
    }
}

/**
 * Default placeholder sizes for a `fit` axis until the content reports its size over the bridge — a
 * WebView has no meaningful intrinsic size, so `fit` would otherwise collapse. Used when the schema
 * omits `fit.default`. 100 (height) matches the iOS implementation; 300 (width) matches the web
 * implementation's `FIT_FALLBACK_SIZE_PX`.
 */
internal const val FIT_PLACEHOLDER_HEIGHT: UInt = 100u
internal const val FIT_PLACEHOLDER_WIDTH: UInt = 300u

internal fun webViewEffectiveSize(
    declaredSize: Size,
    contentWidthCssPx: Int,
    contentHeightCssPx: Int,
): Size {
    val width = when (val widthConstraint = declaredSize.width) {
        is Fit -> Fixed(
            if (contentWidthCssPx > 0) {
                contentWidthCssPx.toUInt()
            } else {
                widthConstraint.default ?: FIT_PLACEHOLDER_WIDTH
            },
        )
        else -> widthConstraint
    }
    val height = when (val heightConstraint = declaredSize.height) {
        is Fit -> Fixed(
            if (contentHeightCssPx > 0) {
                contentHeightCssPx.toUInt()
            } else {
                heightConstraint.default ?: FIT_PLACEHOLDER_HEIGHT
            },
        )
        else -> heightConstraint
    }
    return Size(width = width, height = height)
}

/** Mutable holder for the per-WebView bridge instance, shared across factory/update/onRelease. */
internal class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

/** Idempotent failure latch shared by URL/HTTP/renderer/secure-messaging failure paths. */
internal class LoadFailureFlag {
    private var failed: Boolean = false

    fun markFailed(): Boolean {
        if (failed) return false
        failed = true
        return true
    }
}

private fun WebView.configure(
    expectedOrigin: String?,
    onMainFrameNavigationStarted: (url: String?) -> Unit,
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
    webViewClient = PaywallWebViewClient(
        expectedOrigin = expectedOrigin,
        onMainFrameNavigationStarted = onMainFrameNavigationStarted,
        onMainFrameLoadFailed = onMainFrameLoadFailed,
    )
}
