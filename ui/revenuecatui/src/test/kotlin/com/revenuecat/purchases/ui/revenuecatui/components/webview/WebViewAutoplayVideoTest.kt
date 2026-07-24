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
internal class WebViewAutoplayVideoTest {

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
    fun `injects the autoplay reveal script scoped to the bundle origin when supported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns true

        webView.hideAutoplayVideoUntilPlaying("https://bundle.example.com")

        verify {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                match {
                    it.contains("video[autoplay]:not([data-rc-playing])") &&
                        it.contains("'playing'") &&
                        it.contains("data-rc-playing")
                },
                setOf("https://bundle.example.com"),
            )
        }
    }

    @Test
    fun `arms a fallback reveal so a video that never plays cannot stay hidden`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns true

        webView.hideAutoplayVideoUntilPlaying("https://bundle.example.com")

        verify {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                match { it.contains("loadstart") && it.contains("setTimeout") && it.contains("5000") },
                any(),
            )
        }
    }

    @Test
    fun `falls back to any origin when the expected origin is unknown`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns true

        webView.hideAutoplayVideoUntilPlaying(null)

        verify { WebViewCompat.addDocumentStartJavaScript(webView, any(), setOf("*")) }
    }

    @Test
    fun `does nothing when document-start scripts are unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns false

        webView.hideAutoplayVideoUntilPlaying("https://bundle.example.com")

        verify(exactly = 0) { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) }
    }
}
