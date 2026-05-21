@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle

@JvmSynthetic
@Composable
internal fun WebViewComponentView(
    style: WebViewComponentStyle,
    modifier: Modifier = Modifier,
) {
    if (!style.visible || !style.url.isHttps) return

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                configure()
                loadUrl(style.url.toString())
            }
        },
        update = { webView ->
            if (webView.url != style.url.toString()) {
                webView.loadUrl(style.url.toString())
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

private val java.net.URL.isHttps: Boolean
    get() = protocol == HTTPS_SCHEME

private const val HTTPS_SCHEME = "https"
