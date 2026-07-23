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
internal class WebViewTapHighlightTest {

    private val webView = mockk<WebView>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class)
        every { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `injects the tap-highlight override scoped to the bundle origin when supported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns true

        webView.disableTapHighlight("https://bundle.example.com")

        verify {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                match { it.contains("webkitTapHighlightColor") && it.contains("transparent") },
                setOf("https://bundle.example.com"),
            )
        }
    }

    @Test
    fun `falls back to any origin when the expected origin is unknown`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns true

        webView.disableTapHighlight(null)

        verify { WebViewCompat.addDocumentStartJavaScript(webView, any(), setOf("*")) }
    }

    @Test
    fun `does nothing when document-start scripts are unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns false

        webView.disableTapHighlight("https://bundle.example.com")

        verify(exactly = 0) { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) }
    }
}
