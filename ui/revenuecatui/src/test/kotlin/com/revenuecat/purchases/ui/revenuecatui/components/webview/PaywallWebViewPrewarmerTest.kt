package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
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
        const val OTHER_URL = "https://example.com/other.html"
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

        assertThat(prewarmer.take(URL)).isNotNull()
    }

    @Test
    fun `does nothing when prerendering is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.PRERENDER_WITH_URL) } returns false
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertNotPrerendered()
        assertThat(prewarmer.take(URL)).isNull()
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
        assertThat(prewarmer.take(URL)).isNull()
    }

    @Test
    fun `holds nothing when prerendering throws`() {
        every {
            WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any())
        } throws UnsupportedOperationException("not supported")
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(URL)).isNull()
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

        assertThat(prewarmer.take(URL)).isNull()
    }

    // Replaying a prewarm-time load failure onto the adopting component would make prewarming worse than
    // no prewarming: the display path renders nothing where a cold load would have succeeded.
    @Test
    fun `refuses a view whose document failed to load while prewarming`() {
        val prewarmer = prewarmer()
        val webView = prewarmedWebView(prewarmer)

        failMainFrameLoad(webView)

        assertThat(prewarmer.take(URL)).isNull()
    }

    @Test
    fun `a failed prewarm releases its slot instead of blocking the next one`() {
        val prewarmer = prewarmer()
        val webView = prewarmedWebView(prewarmer)

        failMainFrameLoad(webView)
        shadowOf(Looper.getMainLooper()).idle()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        verify(exactly = 2) { WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any()) }
    }

    /** Prewarms and returns the WebView it built, captured as it installs its message listener. */
    private fun prewarmedWebView(prewarmer: PaywallWebViewPrewarmer): WebView =
        capturingWebView { prewarmer.prewarm(context, URL, COMPONENT_ID) }

    /** Runs [block] and returns the WebView built inside it, captured as it installs its listener. */
    private fun capturingWebView(block: () -> Unit): WebView {
        val webViewSlot = slot<WebView>()
        every {
            WebViewCompat.addWebMessageListener(capture(webViewSlot), any(), any(), any())
        } returns Unit
        block()
        return webViewSlot.captured
    }

    private fun failMainFrameLoad(webView: WebView) {
        val request = mockk<WebResourceRequest>(relaxed = true)
        every { request.isForMainFrame } returns true
        shadowOf(webView).webViewClient.onReceivedError(webView, request, mockk(relaxed = true))
    }

    // Keyed on URL alone, matching iOS. The component id and Fit axes are rebound at activation, so a
    // view warmed for one component serves any component pointing at the same bundle.
    @Test
    fun `hands the view to a different component with the same url`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(URL)).isNotNull()
    }

    @Test
    fun `hands the view to a component with different fit axes`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID, sizeToContentHeight = true)

        assertThat(prewarmer.take(URL)).isNotNull()
    }

    @Test
    fun `does not hand the view to a different url`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(OTHER_URL)).isNull()
    }

    @Test
    fun `clears the slot once taken`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(URL)).isNotNull()
        assertThat(prewarmer.take(URL)).isNull()
    }

    // A paywall can carry several web_view components, so each distinct url gets its own entry.
    @Test
    fun `holds one entry per distinct url`() {
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "component-2")

        assertThat(prewarmer.take(URL)).isNotNull()
        assertThat(prewarmer.take(OTHER_URL)).isNotNull()
    }

    @Test
    fun `does not prerender a url that is already held`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        prewarmer.prewarm(context, URL, "component-2")

        verify(exactly = 1) { WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), any()) }
    }

    // A held entry is worth more than a queued one, so the pool is never evicted to make room. The
    // overflow is still loaded, for the http cache alone.
    @Test
    fun `warms urls beyond the pool for the cache instead of evicting`() {
        val prewarmer = prewarmer()
        fillPool(prewarmer)

        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.heldCount).isEqualTo(PaywallWebViewPrewarmer.MAX_PREWARMED)
        assertThat(prewarmer.take("https://example.com/0.html")).isNotNull()
        assertThat(prewarmer.isWarmingCache).isTrue()
    }

    @Test
    fun `warms one overflow url at a time`() {
        val prewarmer = prewarmer()
        fillPool(prewarmer)

        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "overflow-2")

        assertThat(prewarmer.isWarmingCache).isTrue()
        assertThat(prewarmer.queuedCacheWarmCount).isEqualTo(1)
    }

    @Test
    fun `starts the next cache warm once the previous document finishes`() {
        val prewarmer = prewarmer()
        fillPool(prewarmer)
        val warming = capturingWebView { prewarmer.prewarm(context, URL, COMPONENT_ID) }
        prewarmer.prewarm(context, OTHER_URL, "overflow-2")

        shadowOf(warming).webViewClient.onPageFinished(warming, URL)

        assertThat(prewarmer.queuedCacheWarmCount).isZero()
        assertThat(prewarmer.isWarmingCache).isTrue()
    }

    // Whether a prerendered document reports onPageFinished before activation is undocumented, so a warm
    // that never signals must not stall the ones behind it.
    @Test
    fun `abandons a cache warm that never finishes and moves on`() {
        val prewarmer = prewarmer()
        fillPool(prewarmer)
        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "overflow-2")

        shadowOf(Looper.getMainLooper())
            .idleFor(Duration.ofMillis(PaywallWebViewPrewarmer.CACHE_WARM_BUDGET_MS))

        assertThat(prewarmer.queuedCacheWarmCount).isZero()
        assertThat(prewarmer.isWarmingCache).isTrue()
    }

    // The display path's own load fills the http cache, so a queued warm for that url is pure waste.
    @Test
    fun `drops a queued warm for a url the display path asks for`() {
        val prewarmer = prewarmer()
        fillPool(prewarmer)
        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "overflow-2")

        prewarmer.take(OTHER_URL)

        assertThat(prewarmer.queuedCacheWarmCount).isZero()
    }

    private fun fillPool(prewarmer: PaywallWebViewPrewarmer) {
        repeat(PaywallWebViewPrewarmer.MAX_PREWARMED) { index ->
            prewarmer.prewarm(context, "https://example.com/$index.html", "component-$index")
        }
    }

    @Test
    fun `an async error only drops the entry it belongs to`() {
        val callbacks = mutableListOf<PrerenderOperationCallback>()
        every {
            WebViewCompat.prerenderUrlAsync(any(), any(), any(), any(), capture(callbacks))
        } returns Unit
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "component-2")

        callbacks.first().onError(mockk<PrerenderException>(relaxed = true))

        assertThat(prewarmer.take(URL)).isNull()
        assertThat(prewarmer.take(OTHER_URL)).isNotNull()
    }

    // Siblings belong to other components on the same paywall and are very likely about to be taken.
    @Test
    fun `a miss leaves the other entries held`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        assertThat(prewarmer.take(OTHER_URL)).isNull()

        assertThat(prewarmer.take(URL)).isNotNull()
    }

    @Test
    fun `releases the prewarmed view when it is not displayed in time`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PaywallWebViewPrewarmer.HOLD_TIMEOUT_MS))

        assertThat(prewarmer.take(URL)).isNull()
    }

    @Test
    fun `keeps the prewarmed view until the timeout elapses`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PaywallWebViewPrewarmer.HOLD_TIMEOUT_MS - 1))

        assertThat(prewarmer.take(URL)).isNotNull()
    }

    @Test
    fun `abandons every tier when the app is asked to trim memory`() {
        val prewarmer = prewarmer()
        fillPool(prewarmer)
        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "overflow-2")

        (context as Application).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

        assertThat(prewarmer.heldCount).isZero()
        assertThat(prewarmer.isWarmingCache).isFalse()
        assertThat(prewarmer.queuedCacheWarmCount).isZero()
    }

    @Test
    fun `each entry expires on its own schedule`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)
        val half = PaywallWebViewPrewarmer.HOLD_TIMEOUT_MS / 2
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(half))
        prewarmer.prewarm(context, OTHER_URL, "component-2")

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PaywallWebViewPrewarmer.HOLD_TIMEOUT_MS - half))

        assertThat(prewarmer.take(URL)).isNull()
        assertThat(prewarmer.take(OTHER_URL)).isNotNull()
    }

    @Test
    fun `releaseAll drops every prewarmed view`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL, COMPONENT_ID)
        prewarmer.prewarm(context, OTHER_URL, "component-2")

        prewarmer.releaseAll()

        assertThat(prewarmer.heldCount).isZero()
    }
}
