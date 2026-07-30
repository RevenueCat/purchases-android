@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * [WebViewClient] for Paywalls V2 `web_view` components. Owns navigation policy, main-frame document
 * lifecycle notifications, and a single terminal failure path shared by URL errors, HTTP errors, and
 * renderer termination.
 */
internal class PaywallWebViewClient(
    private val expectedOrigin: String?,
    private val onMainFrameNavigationStarted: () -> Unit,
    private val onMainFrameLoadFailed: () -> Unit,
) : WebViewClient() {

    @Volatile
    private var failed: Boolean = false

    private fun markFailed(reason: String) {
        if (failed) return
        failed = true
        Logger.w("Paywalls V2 web_view load failed terminally, removing the component: $reason")
        onMainFrameLoadFailed()
    }

    @Suppress("ReturnCount")
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Once failed, emit no further document-start signals.
        if (failed) return
        // about:blank / host-less documents aren't paywall loads; ignore them (shouldBlock would
        // otherwise treat them as a failure).
        if (url?.toOriginOrNull() == null) return
        // POST navigations skip shouldOverrideUrlLoading; re-check here to kill any main-frame load
        // that slipped through.
        if (shouldBlockWebViewNavigation(
                url = url,
                isMainFrame = true,
                expectedOrigin = expectedOrigin,
            )
        ) {
            view.stopLoading()
            markFailed("blocked main-frame navigation to '$url' (expectedOrigin=$expectedOrigin)")
            return
        }
        onMainFrameNavigationStarted()
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
            markFailed("resource error code=${error.errorCode} '${error.description}'")
        }
    }

    // The default implementation cancels the load but leaves the component mounted and blank; whether
    // a subsequent onReceivedError fires is undocumented and provider-dependent. Cancel (never show
    // content over a bad certificate) and fail terminally so the WebView is removed deterministically.
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        markFailed("SSL error: $error")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame) {
            markFailed("HTTP error status=${errorResponse.statusCode}")
        }
    }

    // RenderProcessGoneDetail and this callback exist only on API 26+; the framework never invokes it below.
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        markFailed("render process gone (didCrash=${detail.didCrash()})")
        // true = handled; the dead WebView must not be reused (removed via the failure path).
        return true
    }
}
