package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.PrerenderException
import androidx.webkit.PrerenderOperationCallback
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(AndroidJUnit4::class)
internal class PaywallWebViewPrewarmerTest {

    private companion object {
        const val URL = "https://example.com/index.html"
        const val COMPONENT_ID = "component-1"
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun prewarmer() = PaywallWebViewPrewarmer()

    private fun assertNotPrerendered() =
        verify(exactly = 0) { WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any()) }

    private fun identity(
        url: String = URL,
        componentId: String = COMPONENT_ID,
        sizeToContentWidth: Boolean = false,
        sizeToContentHeight: Boolean = false,
    ) = WebViewIdentity(url, componentId, sizeToContentWidth, sizeToContentHeight)

    @Before
    fun setUp() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class, ProfileStore::class)
        every { WebViewFeature.isFeatureSupported(any()) } returns true
        every { ProfileStore.getInstance() } returns mockk(relaxed = true)
        every { WebViewCompat.setProfile(any(), any()) } returns Unit
        every { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) } returns Unit
        every { WebViewCompat.removeWebMessageListener(any(), any()) } returns Unit
        every { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) } returns mockk(relaxed = true)
        every { WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `prerenders the resolved url when supported`() {
        prewarmer().prewarm(context, URL, COMPONENT_ID)

        verify { WebViewCompat.prerenderUrlAsync(any(), URL, any(), any(), any()) }
    }

    @Test
    fun `holds the prewarmed view for a matching identity`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(identity())).isNotNull()
    }

    @Test
    fun `does nothing when prerendering is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.PRERENDER_WITH_URL) } returns false
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertNotPrerendered()
        assertThat(prewarmer.take(identity())).isNull()
    }

    @Test
    fun `does not evaluate feature support until a prewarm is actually attempted`() {
        val prewarmer = prewarmer()

        // An unresolvable URL must bail before the expensive support check.
        prewarmer.prewarm(context, "http://example.com", COMPONENT_ID)

        verify(exactly = 0) { WebViewFeature.isFeatureSupported(WebViewFeature.PRERENDER_WITH_URL) }
    }

    @Test
    fun `ignores non-https urls`() {
        prewarmer().prewarm(context, "http://example.com/index.html", COMPONENT_ID)

        assertNotPrerendered()
    }

    @Test
    fun `ignores urls with unsubstituted variable markers`() {
        prewarmer().prewarm(context, "https://example.com/{{variable}}.html", COMPONENT_ID)

        assertNotPrerendered()
    }

    @Test
    fun `ignores a blank component id`() {
        prewarmer().prewarm(context, URL, "")

        assertNotPrerendered()
    }

    @Test
    fun `does not prerender when secure messaging is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns false
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertNotPrerendered()
        assertThat(prewarmer.take(identity())).isNull()
    }

    @Test
    fun `holds nothing when prerendering throws`() {
        every {
            WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any())
        } throws UnsupportedOperationException("not supported")
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(identity())).isNull()
    }

    @Test
    fun `releases the slot when the async prerender reports an error`() {
        val callback = slot<PrerenderOperationCallback>()
        every {
            WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), capture(callback))
        } returns Unit
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        callback.captured.onError(mockk<PrerenderException>(relaxed = true))

        assertThat(prewarmer.take(identity())).isNull()
    }

    @Test
    fun `does not hand the view to a different component`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(identity(componentId = "other"))).isNull()
    }

    @Test
    fun `does not hand the view to a component with different fit axes`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID, sizeToContentHeight = true)

        assertThat(prewarmer.take(identity(sizeToContentHeight = false))).isNull()
    }

    @Test
    fun `clears the slot once taken`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(identity())).isNotNull()
        assertThat(prewarmer.take(identity())).isNull()
    }

    @Test
    fun `keeps the first prewarm when a second component is requested`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        prewarmer.prewarm(context, "https://example.com/other.html", "component-2")

        verify(exactly = 1) { WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any()) }
        assertThat(prewarmer.take(identity())).isNotNull()
    }

    @Test
    fun `releases the prewarmed view when it is not displayed in time`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PaywallWebViewPrewarmer.HOLD_TIMEOUT_MS))

        assertThat(prewarmer.take(identity())).isNull()
    }

    @Test
    fun `keeps the prewarmed view until the timeout elapses`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PaywallWebViewPrewarmer.HOLD_TIMEOUT_MS - 1))

        assertThat(prewarmer.take(identity())).isNotNull()
    }

    @Test
    fun `releaseAll drops the prewarmed view`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        prewarmer.releaseAll()

        assertThat(prewarmer.take(identity())).isNull()
    }
}
