@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

@JvmSynthetic
@Composable
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

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                configure()
                loadUrl(resolvedUrl.toString())
            }
        },
        update = { webView ->
            if (webView.url != resolvedUrl.toString()) {
                webView.loadUrl(resolvedUrl.toString())
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.destroy()
        },
        modifier = modifier.size(style.size),
    )
}

private fun WebView.configure() {
    setBackgroundColor(Color.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    settings.allowContentAccess = false
    settings.allowFileAccess = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.domStorageEnabled = true
    settings.javaScriptEnabled = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return request.url.scheme != HTTPS_SCHEME
        }
    }
}

private const val HTTPS_SCHEME = "https"
