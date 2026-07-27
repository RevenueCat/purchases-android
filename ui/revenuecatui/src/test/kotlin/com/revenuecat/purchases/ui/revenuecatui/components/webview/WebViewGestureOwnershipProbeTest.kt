package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WebViewGestureOwnershipProbeTest {

    private val webView = mockk<WebView>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns true
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns true
        every { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) } returns Unit
        every { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `installs the probe scoped to the bundle origin when supported`() {
        webView.installGestureOwnershipProbe("https://bundle.example.com") {}

        verify {
            WebViewCompat.addWebMessageListener(webView, any(), setOf("https://bundle.example.com"), any())
            WebViewCompat.addDocumentStartJavaScript(webView, any(), setOf("https://bundle.example.com"))
        }
    }

    @Test
    fun `does nothing when the expected origin is unknown`() {
        webView.installGestureOwnershipProbe(null) {}

        verify(exactly = 0) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
        verify(exactly = 0) { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) }
    }

    @Test
    fun `does nothing when the web message listener feature is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns false

        webView.installGestureOwnershipProbe("https://bundle.example.com") {}

        verify(exactly = 0) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
    }
}
