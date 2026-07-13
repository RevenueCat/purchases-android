@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * [WebViewClient] for Paywalls V2 `web_view` components. Owns navigation policy, main-frame document
 * lifecycle notifications, and a single terminal failure path shared by URL errors, HTTP errors, and
 * renderer termination.
 */
internal class PaywallWebViewClient(
    private val expectedOrigin: String?,
    private val onMainFrameNavigationStarted: (url: String?) -> Unit,
    private val onMainFrameLoadFailed: () -> Unit,
) : WebViewClient() {

    @Volatile
    private var failed: Boolean = false

    private fun markFailed() {
        if (failed) return
        failed = true
        onMainFrameLoadFailed()
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // onPageStarted is main-frame only and marks a new JavaScript document.
        onMainFrameNavigationStarted(url)
    }

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
            markFailed()
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame && errorResponse.statusCode >= HTTP_ERROR_STATUS_MIN) {
            markFailed()
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        markFailed()
        // Returning true tells the platform we handled the dead renderer; the dead WebView must not
        // be reused. Composition removes it via the failure path + AndroidView.onRelease.
        return true
    }

    private companion object {
        private const val HTTP_ERROR_STATUS_MIN = 400
    }
}
