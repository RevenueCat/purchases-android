package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.os.CancellationSignal
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
internal class PrewarmedWebViewTest {

    private companion object {
        const val URL = "https://example.com/index.html"
        const val COMPONENT_ID = "component-1"
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val bridge = mockk<WebViewJavaScriptBridge>(relaxed = true)
    private val callbacks = PrewarmBridgeCallbacks()

    private lateinit var webView: PaywallWebView
    private lateinit var prewarmed: PrewarmedWebView

    @Before
    fun setUp() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class, ProfileStore::class)
        every { WebViewFeature.isFeatureSupported(any()) } returns true
        every { ProfileStore.getInstance() } returns mockk(relaxed = true)
        every { WebViewCompat.removeWebMessageListener(any(), any()) } returns Unit
        webView = PaywallWebView(context)
        prewarmed = PrewarmedWebView(
            webView = webView,
            bridge = bridge,
            callbacks = callbacks,
            identity = WebViewIdentity(URL, COMPONENT_ID, false, false),
            cancellationSignal = CancellationSignal(),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun activate(
        onContentResize: (Int?, Int?) -> Unit = { _, _ -> },
        onLoadFailed: () -> Unit = {},
    ) = prewarmed.activateIn(onContentResize, onDocumentReset = {}, onLoadFailed = onLoadFailed)

    @Test
    fun `activation navigates the prewarmed view to the prerendered url`() {
        activate()

        assertThat(shadowOf(webView).lastLoadedUrl).isEqualTo(URL)
    }

    @Test
    fun `exposes its bridge so the adopting composition can release it`() {
        assertThat(prewarmed.bridge).isSameAs(bridge)
    }

    @Test
    fun `activation hosts the view in a frame layout`() {
        val container = activate()

        assertThat(container.childCount).isEqualTo(1)
        assertThat(container.getChildAt(0)).isSameAs(webView)
    }

    @Test
    fun `activation replays a content size reported during prewarm`() {
        callbacks.dispatchResize(320, 240)
        var width: Int? = null
        var height: Int? = null

        activate(onContentResize = { w, h -> width = w; height = h })

        assertThat(width).isEqualTo(320)
        assertThat(height).isEqualTo(240)
    }

    @Test
    fun `activation replays a failure reported during prewarm`() {
        callbacks.dispatchLoadFailed()
        var failed = false

        activate(onLoadFailed = { failed = true })

        assertThat(failed).isTrue()
    }

    @Test
    fun `destroy releases the bridge and the view`() {
        prewarmed.destroy()

        verify { bridge.release() }
        assertThat(shadowOf(webView).wasDestroyCalled()).isTrue()
    }
}
