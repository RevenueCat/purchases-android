package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaywallWebViewClientTest {

    private lateinit var webView: TrackingWebView
    private val expectedOrigin = "https://assets.example.com"
    private var navigationStartedCount = 0
    private var failureCount = 0

    private lateinit var client: PaywallWebViewClient

    private class TrackingWebView(context: Context) : WebView(context) {
        var stopLoadingCount: Int = 0
            private set

        override fun stopLoading() {
            stopLoadingCount += 1
            super.stopLoading()
        }
    }

    @Before
    fun setUp() {
        webView = TrackingWebView(ApplicationProvider.getApplicationContext())
        navigationStartedCount = 0
        failureCount = 0
        client = PaywallWebViewClient(
            expectedOrigin = expectedOrigin,
            onMainFrameNavigationStarted = { navigationStartedCount += 1 },
            onMainFrameLoadFailed = { failureCount += 1 },
        )
    }

    @Test
    fun `onPageStarted notifies main-frame document start`() {
        client.onPageStarted(webView, "https://assets.example.com/promo/index.html", null)

        assertThat(navigationStartedCount).isEqualTo(1)
        assertThat(webView.stopLoadingCount).isEqualTo(0)
    }

    @Test
    fun `onPageStarted stops blocked cross-origin loads that bypass shouldOverrideUrlLoading`() {
        // POST navigations skip shouldOverrideUrlLoading; onPageStarted is the backstop.
        client.onPageStarted(webView, "https://evil.example.org/phish.html", null)

        assertThat(navigationStartedCount).isEqualTo(0)
        assertThat(webView.stopLoadingCount).isEqualTo(1)
        assertThat(failureCount).isEqualTo(1)
    }

    @Test
    fun `onPageStarted stops blocked non-https loads`() {
        client.onPageStarted(webView, "http://assets.example.com/promo/insecure.html", null)

        assertThat(navigationStartedCount).isEqualTo(0)
        assertThat(webView.stopLoadingCount).isEqualTo(1)
        assertThat(failureCount).isEqualTo(1)
    }

    @Test
    fun `onPageStarted ignores about blank without failing`() {
        client.onPageStarted(webView, "about:blank", null)

        assertThat(navigationStartedCount).isEqualTo(0)
        assertThat(webView.stopLoadingCount).isEqualTo(0)
        assertThat(failureCount).isEqualTo(0)
    }

    @Test
    fun `onPageStarted ignores a null url without failing`() {
        client.onPageStarted(webView, null, null)

        assertThat(navigationStartedCount).isEqualTo(0)
        assertThat(webView.stopLoadingCount).isEqualTo(0)
        assertThat(failureCount).isEqualTo(0)
    }

    @Test
    fun `onPageStarted suppresses document start after a terminal failure`() {
        val request = mockk<WebResourceRequest>()
        every { request.isForMainFrame } returns true
        client.onReceivedError(webView, request, mockk(relaxed = true))
        assertThat(failureCount).isEqualTo(1)

        client.onPageStarted(webView, "https://assets.example.com/promo/index.html", null)

        assertThat(navigationStartedCount).isEqualTo(0)
        assertThat(failureCount).isEqualTo(1)
    }

    @Test
    fun `onRenderProcessGone returns true and activates failure once`() {
        val detail = mockk<RenderProcessGoneDetail>(relaxed = true)

        val first = client.onRenderProcessGone(webView, detail)
        val second = client.onRenderProcessGone(webView, detail)

        assertThat(first).isTrue()
        assertThat(second).isTrue()
        assertThat(failureCount).isEqualTo(1)
    }

    @Test
    fun `main-frame URL and HTTP errors share the terminal failure path`() {
        val request = mockk<WebResourceRequest>()
        every { request.isForMainFrame } returns true
        val error = mockk<WebResourceError>(relaxed = true)
        val response = mockk<WebResourceResponse>()
        every { response.statusCode } returns 500

        client.onReceivedError(webView, request, error)
        client.onReceivedHttpError(webView, request, response)
        client.onRenderProcessGone(webView, mockk(relaxed = true))

        assertThat(failureCount).isEqualTo(1)
    }
}
