@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

@JvmSynthetic
@Composable
// `state` is unused in v1 but kept for the uniform ComponentView signature (variables land later).
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
    if (resolvedUrl == null) {
        Logger.w("Paywalls V2 web_view not rendered: URL must be https with no '{{' markers: '${style.url}'")
        return
    }

    val componentId = style.componentId
    // workflow-web-components-sdk requires a host-assigned component id for the handshake.
    if (componentId.isBlank()) {
        Logger.w("Paywalls V2 web_view not rendered: componentId is blank.")
        return
    }
    val sizeToContentWidth = style.size.width is Fit
    val sizeToContentHeight = style.size.height is Fit

    val identity = WebViewIdentity(
        resolvedUrl = resolvedUrl,
        componentId = componentId,
        sizeToContentWidth = sizeToContentWidth,
        sizeToContentHeight = sizeToContentHeight,
    )

    // key(identity): any change to an immutable bridge field disposes this subtree (WebView + bridge
    // + measured sizes) and builds a fresh one.
    key(identity) {
        var contentWidthCssPx by remember { mutableIntStateOf(0) }
        var contentHeightCssPx by remember { mutableIntStateOf(0) }
        var loadFailed by remember { mutableStateOf(false) }
        // Remembered inside key(identity) so a stale onRelease can only release its own view's bridge.
        val bridgeHolder = remember { WebViewBridgeHolder() }

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
                        applyFullSizeLayoutParams()
                        // Must precede attach()/loadUrl: setProfile throws once the WebView has been used.
                        applyPaywallProfile()
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
                            onSecureMessagingUnsupported = { loadFailed = true },
                        ).also { createdBridge -> createdBridge.attach() }
                        bridgeHolder.bridge = bridge
                        // attach() may synchronously flag terminal failure (secure messaging
                        // unsupported); don't start a JS-enabled load we're about to tear down.
                        if (!loadFailed) {
                            configure(
                                expectedOrigin = resolvedUrl.toOriginOrNull(),
                                onMainFrameNavigationStarted = {
                                    bridgeHolder.bridge?.onMainFrameNavigationStarted()
                                },
                                onMainFrameLoadFailed = { loadFailed = true },
                            )
                            loadUrl(resolvedUrl)
                        }
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
                // Clip: content can briefly overflow while a fit axis animates placeholder -> measured.
                modifier = modifier
                    .size(effectiveSize)
                    .clipToBounds(),
            )
        }
        // Terminal failure renders nothing; there is intentionally no native fallback.
    }
}

/**
 * Placeholder `fit`-axis sizes used before content reports a size (a WebView has no intrinsic size)
 * and the schema omits `fit.default`. 100 (height) matches iOS; 300 (width) matches web.
 */
internal const val FIT_PLACEHOLDER_HEIGHT: UInt = 100u
internal const val FIT_PLACEHOLDER_WIDTH: UInt = 300u

internal fun webViewEffectiveSize(
    declaredSize: Size,
    contentWidthCssPx: Int,
    contentHeightCssPx: Int,
): Size = Size(
    width = resolveFitAxis(declaredSize.width, contentWidthCssPx, FIT_PLACEHOLDER_WIDTH),
    height = resolveFitAxis(declaredSize.height, contentHeightCssPx, FIT_PLACEHOLDER_HEIGHT),
)

/**
 * A `fit` axis resolves to the content-reported size once known, else the schema's `fit.default`, else
 * [placeholder]. Non-fit axes pass through unchanged.
 */
private fun resolveFitAxis(constraint: SizeConstraint, contentCssPx: Int, placeholder: UInt): SizeConstraint =
    when (constraint) {
        is Fit -> Fixed(if (contentCssPx > 0) contentCssPx.toUInt() else constraint.default ?: placeholder)
        else -> constraint
    }

/** Holds the per-WebView bridge so factory and onRelease share one instance. */
internal class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

// WebView drives Chromium's force_zero_layout_height off its LayoutParams: with the WRAP_CONTENT
// defaults AndroidView assigns, CSS % and vh heights resolve to 0 and content renders blank. Compose
// sizes the view from exact constraints, so MATCH_PARENT is safe and only flips the Chromium flag.
internal fun WebView.applyFullSizeLayoutParams() {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
}

private fun WebView.configure(
    expectedOrigin: String?,
    onMainFrameNavigationStarted: () -> Unit,
    onMainFrameLoadFailed: () -> Unit,
) {
    setBackgroundColor(Color.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    // No overscroll glow/bounce (matches iOS). Native scroll isn't hard-disabled — that would eat
    // touchmove from interactive content; fit axes size to content, so the common case can't overflow.
    overScrollMode = View.OVER_SCROLL_NEVER
    settings.allowContentAccess = false
    settings.allowFileAccess = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.domStorageEnabled = true
    settings.javaScriptEnabled = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.setGeolocationEnabled(false)
    // Lock zoom (parity with iOS `user-scalable=no`); the bundle sets its own viewport.
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.mediaPlaybackRequiresUserGesture = false
    webViewClient = PaywallWebViewClient(
        expectedOrigin = expectedOrigin,
        onMainFrameNavigationStarted = onMainFrameNavigationStarted,
        onMainFrameLoadFailed = onMainFrameLoadFailed,
    )
    // Inspect the bundle from Chrome DevTools in debug builds only; process-global, never in release.
    if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    // Surface the bundle's own JS console in logcat when the SDK is on DEBUG/VERBOSE, so authors can
    // diagnose their content without a debugger attached.
    if (Purchases.logLevel <= LogLevel.DEBUG) {
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Logger.d(
                    "Paywalls V2 web_view console [${message.messageLevel()}] ${message.message()} " +
                        "(${message.sourceId()}:${message.lineNumber()})",
                )
                return true
            }
        }
    }
    disableTapHighlight(expectedOrigin)
}

// Android draws a translucent tap-highlight scrim (blue on most themes) over tapped clickable content;
// iOS WKWebView does not. Set `-webkit-tap-highlight-color: transparent` as an inherited default at the
// document root. A bundle can still override it per element (inheritance loses to any explicit value).
@Suppress("TooGenericExceptionCaught")
internal fun WebView.disableTapHighlight(expectedOrigin: String?) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
    try {
        WebViewCompat.addDocumentStartJavaScript(
            this,
            "document.documentElement.style.webkitTapHighlightColor = 'transparent';",
            setOf(expectedOrigin ?: "*"),
        )
    } catch (error: RuntimeException) {
        Logger.w("Failed to disable webkit tap highlight: $error")
    }
}

// Dedicated persistent profile isolating paywall WebView storage from the host app; shared across paywalls.
internal const val PAYWALL_PROFILE_NAME: String = "com.revenuecat.paywall"

// Isolation is an enhancement: on any failure fall back to the Default profile rather than failing the render.
@Suppress("TooGenericExceptionCaught")
internal fun WebView.applyPaywallProfile() {
    // Unsupported System WebViews (< 113) keep the Default profile; setProfile would otherwise throw.
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return
    try {
        ProfileStore.getInstance().getOrCreateProfile(PAYWALL_PROFILE_NAME)
        WebViewCompat.setProfile(this, PAYWALL_PROFILE_NAME)
    } catch (error: RuntimeException) {
        Logger.w("Paywalls V2 web_view could not use an isolated profile; using the default. $error")
    }
}
