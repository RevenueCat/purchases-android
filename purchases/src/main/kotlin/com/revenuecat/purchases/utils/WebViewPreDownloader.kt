package com.revenuecat.purchases.utils

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.revenuecat.purchases.common.verboseLog
import java.net.URL

internal interface WebViewPreDownloader {
    fun preDownloadWebView(url: URL)
}

internal object NoOpWebViewPreDownloader : WebViewPreDownloader {
    override fun preDownloadWebView(url: URL) = Unit
}

internal class DefaultWebViewPreDownloader(
    private val applicationContext: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val webViewFactory: (Context) -> WebView = { WebView(it) },
) : WebViewPreDownloader {

    override fun preDownloadWebView(url: URL) {
        if (url.protocol != HTTPS_SCHEME) {
            verboseLog { "Skipping Paywall V2 web view pre-download for non-HTTPS URL: $url" }
            return
        }

        mainHandler.post {
            val webView = webViewFactory(applicationContext)
            var destroyed = false
            fun destroyWebView() {
                if (destroyed) return
                destroyed = true
                webView.stopLoading()
                webView.webViewClient = WebViewClient()
                webView.destroy()
            }

            webView.configureForPreDownload(onFinished = {
                mainHandler.postDelayed(
                    ::destroyWebView,
                    POST_LOAD_DESTROY_DELAY_MILLIS,
                )
            })
            webView.loadUrl(url.toString())
            mainHandler.postDelayed(
                ::destroyWebView,
                MAX_PRE_DOWNLOAD_DURATION_MILLIS,
            )
        }
    }

    private fun WebView.configureForPreDownload(onFinished: () -> Unit) {
        setBackgroundColor(Color.TRANSPARENT)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowContentAccess = false
        settings.allowFileAccess = false
        settings.domStorageEnabled = true
        settings.javaScriptEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return request.url.scheme != HTTPS_SCHEME
            }

            override fun onPageFinished(view: WebView, url: String?) {
                onFinished()
            }
        }
    }

    private companion object {
        const val HTTPS_SCHEME = "https"
        const val POST_LOAD_DESTROY_DELAY_MILLIS = 2_000L
        const val MAX_PRE_DOWNLOAD_DURATION_MILLIS = 30_000L
    }
}
