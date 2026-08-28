@file:JvmSynthetic
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProfileStore
import androidx.webkit.WebStorageCompat
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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.extensions.trackMainAxisUnbounded
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.serialization.json.JsonObject

@JvmSynthetic
@Composable
@Suppress("LongMethod", "ReturnCount", "CyclomaticComplexMethod", "TooGenericExceptionCaught")
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

    // AndroidView's factory captures this lambda once, and the view model hands out a new
    // PaywallState instance on every rebuild, so both reads go through State to stay current.
    val darkMode by rememberUpdatedState(isSystemInDarkTheme())
    val currentState by rememberUpdatedState(state)
    val contextSnapshotProvider: () -> JsonObject = {
        webViewContextSnapshot(
            locale = currentState.locale.toLocaleId().value,
            darkMode = darkMode,
        )
    }

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
                    PaywallWebViewPrewarmer.shared.onDisplayStarted(resolvedUrl)
                    val configured = try {
                        createPaywallWebView(
                            context = context,
                            identity = identity,
                            onContentResize = onContentResize,
                            onDocumentReset = onDocumentReset,
                            onLoadFailed = onLoadFailed,
                            onLoadFinished = { PaywallWebViewPrewarmer.shared.markWarmed(resolvedUrl) },
                            contextSnapshotProvider = contextSnapshotProvider,
                        )
                    } catch (error: Throwable) {
                        // A missing or mid-update WebView package throws Error, not Exception.
                        Logger.w("Paywalls V2 web_view could not be created: $error")
                        loadFailed = true
                        null
                    }
                    bridgeHolder.bridge = configured?.bridge
                    if (configured == null) {
                        FrameLayout(context)
                    } else {
                        try {
                            configured.webView.loadUrl(resolvedUrl)
                            configured.webView.hostedInFrameLayout()
                        } catch (error: Throwable) {
                            Logger.w("Paywalls V2 web_view could not start its load: $error")
                            bridgeHolder.bridge = null
                            configured.webView.releasePaywallWebView(configured.bridge)
                            loadFailed = true
                            FrameLayout(context)
                        }
                    }
                },
                onRelease = { container ->
                    PaywallWebViewPrewarmer.shared.onDisplayEnded()
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

@Suppress("TooGenericExceptionCaught")
private inline fun <T> PaywallWebView.destroyingOnFailure(setUp: PaywallWebView.() -> T): T {
    try {
        return setUp()
    } catch (error: Throwable) {
        destroyPaywallWebView()
        throw error
    }
}

private fun newPaywallWebView(context: Context): PaywallWebView =
    PaywallWebView(context).destroyingOnFailure {
        applyFullSizeLayoutParams()
        applyPaywallProfile()
        this
    }

@Suppress("LongParameterList")
internal fun createPaywallWebView(
    context: Context,
    identity: WebViewIdentity,
    onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit = { _, _ -> },
    onDocumentReset: () -> Unit = {},
    onLoadFailed: () -> Unit,
    onLoadFinished: () -> Unit = {},
    contextSnapshotProvider: () -> JsonObject,
): ConfiguredPaywallWebView? {
    var terminalFailure = false
    val expectedOrigin = identity.resolvedUrl.toOriginOrNull()
    val webView = newPaywallWebView(context)
    return webView.destroyingOnFailure {
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
            contextSnapshotProvider = contextSnapshotProvider,
        )
        bridge.attach()
        if (terminalFailure) {
            releasePaywallWebView(bridge)
            return@destroyingOnFailure null
        }
        configure(
            expectedOrigin = expectedOrigin,
            allowMediaAutoplay = true,
            onMainFrameNavigationStarted = bridge::onMainFrameNavigationStarted,
            onMainFrameLoadFailed = onLoadFailed,
            onMainFrameLoadFinished = onLoadFinished,
        )
        installGestureOwnershipProbe(expectedOrigin, this::onContentGestureVerdict)
        disableTapHighlight(expectedOrigin)
        hideAutoplayVideoUntilPlaying(expectedOrigin)
        ConfiguredPaywallWebView(this, bridge)
    }
}

/**
 * Installs no bridge, so unlike [createPaywallWebView] this works where secure messaging is unsupported.
 *
 * Autoplay is refused because an `<audio autoplay>` really does play from a WebView that was never
 * attached to a window. The media is still fetched and cached.
 */
internal fun createWarmingWebView(
    context: Context,
    resolvedUrl: String,
    onLoadFailed: () -> Unit,
    onLoadFinished: () -> Unit,
): PaywallWebView = newPaywallWebView(context).destroyingOnFailure {
    configure(
        expectedOrigin = resolvedUrl.toOriginOrNull(),
        allowMediaAutoplay = false,
        onMainFrameNavigationStarted = {},
        onMainFrameLoadFailed = onLoadFailed,
        onMainFrameLoadFinished = onLoadFinished,
    )
    this
}

internal fun WebView.destroyPaywallWebView() {
    stopLoading()
    // Drop PaywallWebViewClient so a late callback cannot fire into a destroyed view.
    webViewClient = WebViewClient()
    destroy()
}

internal fun WebView.releasePaywallWebView(bridge: WebViewJavaScriptBridge?) {
    bridge?.release()
    removeGestureOwnershipProbe()
    destroyPaywallWebView()
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

@Suppress("LongParameterList")
private fun WebView.configure(
    expectedOrigin: String?,
    allowMediaAutoplay: Boolean,
    onMainFrameNavigationStarted: () -> Unit,
    onMainFrameLoadFailed: () -> Unit,
    onMainFrameLoadFinished: () -> Unit,
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
    settings.mediaPlaybackRequiresUserGesture = !allowMediaAutoplay
    webViewClient = PaywallWebViewClient(
        expectedOrigin = expectedOrigin,
        onMainFrameNavigationStarted = onMainFrameNavigationStarted,
        onMainFrameLoadFailed = onMainFrameLoadFailed,
        onMainFrameLoadFinished = onMainFrameLoadFinished,
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

@Suppress("TooGenericExceptionCaught")
internal fun clearPaywallProfileStorage() {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
        Logger.d("Paywalls V2 web_view storage was not cleared: this System WebView has no isolated profile.")
        return
    }
    try {
        // getProfile, not getOrCreateProfile: an app that never rendered a web_view has nothing to clear.
        val profile = ProfileStore.getInstance().getProfile(PAYWALL_PROFILE_NAME) ?: return
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)) {
            // Deletion can also drop data written while it runs, so the prewarmer is told once it finishes.
            WebStorageCompat.deleteBrowsingData(profile.webStorage) {
                PaywallWebViewPrewarmer.shared.onCacheCleared()
                Logger.d("Cleared the paywall web_view profile's browsing data.")
            }
        } else {
            // Nothing clears this profile's network cache without DELETE_BROWSING_DATA, and deleteAllData
            // documents only Web SQL and Web Storage, so IndexedDB and CacheStorage survive here.
            // removeAllCookies is asynchronous, so the flush that persists it goes in its callback.
            val cookieManager = profile.cookieManager
            cookieManager.removeAllCookies { cookieManager.flush() }
            profile.webStorage.deleteAllData()
            Logger.d("Cleared the paywall web_view profile's cookies and web storage.")
        }
    } catch (error: RuntimeException) {
        Logger.w("Paywalls V2 web_view storage could not be cleared. $error")
    }
}
