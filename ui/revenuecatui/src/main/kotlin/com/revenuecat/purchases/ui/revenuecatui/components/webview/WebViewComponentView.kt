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
@Suppress("UnusedParameter")
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    if (!style.visible) return

    val resolvedUrl = remember(style.url) {
        WebViewUrlResolver.resolve(style.url)
    }
    // The web view URL is missing or did not resolve to a valid HTTPS URL with a host. web_view
    // availability is gated by SDK version on the frontend, so a delivered web_view is expected to
    // always resolve; render nothing rather than crashing if it doesn't.
    if (resolvedUrl == null) return

    // Key on the resolved URL so the WebView is created (and the page loaded) exactly once per intended
    // URL. We deliberately do NOT reload on every recomposition: in-page navigation changes WebView.url,
    // and reloading whenever it differs from resolvedUrl would reset a multi-step web flow. The WebView
    // is only recreated when the SDK-resolved URL itself changes.
    key(resolvedUrl) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    configure()
                    loadUrl(resolvedUrl)
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
    settings.setGeolocationEnabled(false)
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // onPageStarted injection is async; early subresource fetches can precede the policy.
            // Header injection via shouldInterceptRequest is deliberately out of scope.
            view.evaluateJavascript(contentSecurityPolicyMetaScript(), null)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            view.evaluateJavascript(contentSecurityPolicyMetaScript(), null)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return request.url.scheme != HTTPS_SCHEME
        }
    }
}

private const val HTTPS_SCHEME = "https"
