package com.revenuecat.purchases.ui.revenuecatui.components.webview

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

    private lateinit var webView: WebView
    private val expectedOrigin = "https://assets.example.com"
    private val navigations = mutableListOf<String?>()
    private var failureCount = 0

    private lateinit var client: PaywallWebViewClient

    @Before
    fun setUp() {
        webView = WebView(ApplicationProvider.getApplicationContext())
        navigations.clear()
        failureCount = 0
        client = PaywallWebViewClient(
            expectedOrigin = expectedOrigin,
            onMainFrameNavigationStarted = { navigations.add(it) },
            onMainFrameLoadFailed = { failureCount += 1 },
        )
    }

    @Test
    fun `onPageStarted notifies main-frame document start`() {
        client.onPageStarted(webView, "https://assets.example.com/promo/index.html", null)

        assertThat(navigations).containsExactly("https://assets.example.com/promo/index.html")
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
