@file:JvmSynthetic
// This file is the single home for the paywall WebView extensions; keeping the shared builder here
// is what lets configure() stay private.
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.trackMainAxisUnbounded
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

@JvmSynthetic
@Composable
@Suppress("LongMethod", "ReturnCount", "CyclomaticComplexMethod")
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val webViewState = rememberUpdatedWebViewComponentState(style, state)
    if (!webViewState.visible) return

    val resolvedUrl = remember(style.url) {
        WebViewUrlResolver.resolve(style.url)
    }
    val componentId = style.componentId

    LaunchedEffect(style.url, componentId) {
        when {
            resolvedUrl == null ->
                Logger.w("Paywalls V2 web_view not rendered: URL must be https with no '{{' markers: '${style.url}'")
            componentId.isBlank() ->
                Logger.w("Paywalls V2 web_view not rendered: componentId is blank.")
        }
    }

    if (resolvedUrl == null) return
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

    // key(identity): any change to an immutable bridge field disposes this subtree (WebView + bridge
    // + measured sizes) and builds a fresh one.
    key(identity) {
        var contentWidthCssPx by remember { mutableIntStateOf(0) }
        var contentHeightCssPx by remember { mutableIntStateOf(0) }
        var loadFailed by remember { mutableStateOf(false) }
        // Remembered inside key(identity) so a stale onRelease can only release its own view's bridge.
        val bridgeHolder = remember { WebViewBridgeHolder() }

        // A `fill` axis genuinely unbounded at measure time (e.g. an ancestor scrolls, or a `Fit`-sized
        // container sits under one that does) would otherwise collapse to zero — `fillMaxWidth/Height`
        // passes an unbounded constraint straight through, and a bare WebView has no intrinsic size to
        // fall back on. Tracked here (reusing the same probe StackComponentView uses for its main axis)
        // and fed the same content-size/placeholder fallback `fit` already uses.
        val widthAxisUnboundedState = remember { mutableStateOf(false) }
        val heightAxisUnboundedState = remember { mutableStateOf(false) }
        val widthAxisUnbounded = widthAxisUnboundedState.value
        val heightAxisUnbounded = heightAxisUnboundedState.value

        val effectiveSize = remember(
            style.size,
            contentWidthCssPx,
            contentHeightCssPx,
            widthAxisUnbounded,
            heightAxisUnbounded,
        ) {
            webViewEffectiveSize(
                declaredSize = style.size,
                contentWidthCssPx = contentWidthCssPx,
                contentHeightCssPx = contentHeightCssPx,
                widthAxisUnbounded = widthAxisUnbounded,
                heightAxisUnbounded = heightAxisUnbounded,
            )
        }

        if (!loadFailed) {
            AndroidView(
                factory = { context ->
                    val onContentResize: (Int?, Int?) -> Unit = { widthCssPx, heightCssPx ->
                        widthCssPx?.takeIf { it > 0 }?.let { contentWidthCssPx = it }
                        heightCssPx?.takeIf { it > 0 }?.let { contentHeightCssPx = it }
                    }
                    val onDocumentReset: () -> Unit = {
                        contentWidthCssPx = 0
                        contentHeightCssPx = 0
                    }
                    val onLoadFailed: () -> Unit = { loadFailed = true }
                    val prewarmed = PaywallWebViewPrewarmer.shared.take(identity)
                    if (prewarmed != null) {
                        bridgeHolder.bridge = prewarmed.bridge
                        prewarmed.activateIn(onContentResize, onDocumentReset, onLoadFailed)
                    } else {
                        val configured = createPaywallWebView(
                            context = context,
                            identity = identity,
                            onContentResize = onContentResize,
                            onDocumentReset = onDocumentReset,
                            onLoadFailed = onLoadFailed,
                        )
                        bridgeHolder.bridge = configured?.bridge
                        // A null result means secure messaging is unsupported, which onLoadFailed has
                        // already flagged; never start a JS-enabled load we are about to tear down.
                        configured?.webView?.apply { loadUrl(resolvedUrl) }?.hostedInFrameLayout()
                            ?: FrameLayout(context)
                    }
                },
                onRelease = { container ->
                    // Only release the bridge that this view installed into this holder.
                    val bridge = bridgeHolder.bridge
                    bridgeHolder.bridge = null
                    ((container as FrameLayout).getChildAt(0) as? WebView)?.releasePaywallWebView(bridge)
                },
                // Clip: content can briefly overflow while a fit axis animates placeholder -> measured.
                modifier = modifier
                    // Only Fill axes ever consult these (resolveAxis's Fixed/Fit branches ignore them).
                    .conditional(style.size.width is Fill) {
                        trackMainAxisUnbounded(isHorizontal = true, unboundedState = widthAxisUnboundedState)
                    }
                    .conditional(style.size.height is Fill) {
                        trackMainAxisUnbounded(isHorizontal = false, unboundedState = heightAxisUnboundedState)
                    }
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
    widthAxisUnbounded: Boolean = false,
    heightAxisUnbounded: Boolean = false,
): Size = Size(
    width = resolveAxis(declaredSize.width, contentWidthCssPx, FIT_PLACEHOLDER_WIDTH, widthAxisUnbounded),
    height = resolveAxis(declaredSize.height, contentHeightCssPx, FIT_PLACEHOLDER_HEIGHT, heightAxisUnbounded),
)

/**
 * A `fit` axis always resolves to the content-reported size once known, else the schema's
 * `fit.default`, else [placeholder]. A `fill` axis normally passes through unchanged (it fills its
 * parent), but a bare WebView has no intrinsic size of its own to fall back on, so a `fill` axis
 * that turns out to be [unbounded] at measure time (an ancestor scrolls, or a `Fit`-sized container
 * sits under one that does) gets the same content/placeholder fallback `fit` uses — otherwise it
 * would collapse to zero, since `fillMaxWidth`/`fillMaxHeight` just pass an unbounded constraint
 * through. `fixed` axes, and a bounded `fill` axis, are untouched either way.
 */
internal fun resolveAxis(
    constraint: SizeConstraint,
    contentCssPx: Int,
    placeholder: UInt,
    unbounded: Boolean,
): SizeConstraint =
    when (constraint) {
        is Fit -> Fixed(if (contentCssPx > 0) contentCssPx.toUInt() else constraint.default ?: placeholder)
        is Fill -> if (unbounded) {
            Fixed(if (contentCssPx > 0) contentCssPx.toUInt() else placeholder)
        } else {
            constraint
        }
        is Fixed -> constraint
    }

/** Holds the per-WebView bridge so factory and onRelease share one instance. */
internal class WebViewBridgeHolder {
    var bridge: WebViewJavaScriptBridge? = null
}

internal class ConfiguredPaywallWebView(
    val webView: PaywallWebView,
    val bridge: WebViewJavaScriptBridge,
)

/**
 * Builds a paywall WebView and its bridge in the one order that works, shared by the display path
 * and by prewarming so the two cannot drift: a prewarmed view is adopted on identity alone, so any
 * configuration only one of them applied would silently reach the screen.
 *
 * Returns null when the bridge cannot install secure messaging, which is terminal: [onLoadFailed]
 * has fired and the caller must not start a load. The webView and bridge are already released and
 * destroyed at that point; a null result needs no teardown from the caller.
 */
internal fun createPaywallWebView(
    context: Context,
    identity: WebViewIdentity,
    onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit,
    onDocumentReset: () -> Unit,
    onLoadFailed: () -> Unit,
): ConfiguredPaywallWebView? {
    var terminalFailure = false
    val expectedOrigin = identity.resolvedUrl.toOriginOrNull()
    val webView = PaywallWebView(context)
    webView.applyFullSizeLayoutParams()
    // Must precede attach()/loadUrl: setProfile throws once the WebView has been used.
    webView.applyPaywallProfile()
    val bridge = WebViewJavaScriptBridge(
        webView = webView,
        componentId = identity.componentId,
        expectedUrl = identity.resolvedUrl,
        sizeToContentWidth = identity.sizeToContentWidth,
        sizeToContentHeight = identity.sizeToContentHeight,
        onContentResize = onContentResize,
        onDocumentReset = onDocumentReset,
        onSecureMessagingUnsupported = {
            terminalFailure = true
            onLoadFailed()
        },
    )
    bridge.attach()
    if (terminalFailure) {
        bridge.release()
        webView.destroy()
        return null
    }
    webView.configure(
        expectedOrigin = expectedOrigin,
        onMainFrameNavigationStarted = bridge::onMainFrameNavigationStarted,
        onMainFrameLoadFailed = onLoadFailed,
    )
    webView.installGestureOwnershipProbe(expectedOrigin, webView::onContentGestureVerdict)
    return ConfiguredPaywallWebView(webView, bridge)
}

/** Teardown for a paywall WebView, whether it was displayed or only prewarmed. */
internal fun WebView.releasePaywallWebView(bridge: WebViewJavaScriptBridge?) {
    bridge?.release()
    removeGestureOwnershipProbe()
    stopLoading()
    // Drop PaywallWebViewClient so a late callback cannot fire into a destroyed view.
    webViewClient = WebViewClient()
    destroy()
}

// A hardware WebView drawn while fully off-screen (e.g. a non-visible carousel page) composites its GL
// functor into an offscreen layer that has no surface, crashing the RenderThread in
// SkSurface::getCanvas(). Hosting it inside a FrameLayout rather than directly in the AndroidView
// isolates the functor from Compose's offscreen compositing while keeping hardware acceleration (a
// software layer would avoid the crash but break video and WebGL).
internal fun WebView.hostedInFrameLayout(): FrameLayout =
    FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        addView(this@hostedInFrameLayout)
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
    hideAutoplayVideoUntilPlaying(expectedOrigin)
}

// Fallback reveal for an autoplay <video> that never emits `playing`; long enough that a normal clip plays first.
private const val AUTOPLAY_REVEAL_FALLBACK_MS = 5000

// Android WebView flashes a play-button placeholder over an autoplay <video> until its first frame
// paints. The placeholder isn't a stable UA element across Chromium versions, so rather than target it
// we hide autoplay videos until they emit `playing`, with a fallback reveal so one that never plays
// can't stay hidden.
@Suppress("TooGenericExceptionCaught")
internal fun WebView.hideAutoplayVideoUntilPlaying(expectedOrigin: String?) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
    try {
        WebViewCompat.addDocumentStartJavaScript(
            this,
            """
            (function () {
              var style = document.createElement('style');
              style.textContent = 'video[autoplay]:not([data-rc-playing]){opacity:0!important}';
              document.documentElement.appendChild(style);
              function reveal(video) { video.setAttribute('data-rc-playing', ''); }
              document.addEventListener('playing', function (event) { reveal(event.target); }, true);
              // Armed per element on `loadstart` so videos added after load are covered too.
              document.addEventListener('loadstart', function (event) {
                var video = event.target;
                setTimeout(function () { reveal(video); }, $AUTOPLAY_REVEAL_FALLBACK_MS);
              }, true);
            })();
            """.trimIndent(),
            setOf(expectedOrigin ?: "*"),
        )
    } catch (error: RuntimeException) {
        Logger.w("Failed to install web_view autoplay video reveal: $error")
    }
}

// Android draws a translucent tap-highlight scrim (blue on most themes) over tapped clickable content;
// iOS WKWebView does not. Set `-webkit-tap-highlight-color: transparent` as an inherited default at the
// document root. A bundle can still override it per element (inheritance loses to any explicit value).
@Suppress("TooGenericExceptionCaught")
internal fun WebView.disableTapHighlight(expectedOrigin: String?) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
    val origin = expectedOrigin ?: return
    try {
        WebViewCompat.addDocumentStartJavaScript(
            this,
            "document.documentElement.style.webkitTapHighlightColor = 'transparent';",
            setOf(origin),
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
