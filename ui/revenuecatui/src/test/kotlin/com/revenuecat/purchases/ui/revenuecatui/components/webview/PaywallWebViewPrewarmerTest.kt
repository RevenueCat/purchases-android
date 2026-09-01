@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
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
import java.time.Duration

@RunWith(AndroidJUnit4::class)
internal class PaywallWebViewPrewarmerTest {

    private companion object {
        const val URL = "https://example.com/index.html"
        const val OTHER_URL = "https://example.com/other.html"
        const val THIRD_URL = "https://example.com/third.html"
        const val DISPLAYED_URL = "https://example.com/displayed.html"
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val warmed = mutableListOf<WebView>()

    private fun prewarmer(maxConcurrent: Int = 1) = PaywallWebViewPrewarmer(maxConcurrent)

    private fun PaywallWebViewPrewarmer.prewarmAll(vararg urls: String) = urls.forEach { prewarm(context, it) }

    private fun assertWarmCount(count: Int) = assertThat(warmed).hasSize(count)

    private fun lastLoadedUrl() = shadowOf(warmed.last()).lastLoadedUrl

    private fun loadedUrls() = warmed.map { shadowOf(it).lastLoadedUrl }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun finishMainFrameLoad(webView: WebView, url: String = URL) {
        shadowOf(webView).webViewClient.onPageFinished(webView, url)
        idle()
    }

    private fun failMainFrameLoad(webView: WebView) {
        val request = mockk<WebResourceRequest>(relaxed = true)
        every { request.isForMainFrame } returns true
        shadowOf(webView).webViewClient.onReceivedError(webView, request, mockk(relaxed = true))
    }

    @Before
    fun setUp() {
        warmed.clear()
        mockkStatic(WebViewFeature::class, WebViewCompat::class, ProfileStore::class)
        every { WebViewFeature.isFeatureSupported(any()) } returns true
        every { ProfileStore.getInstance() } returns mockk(relaxed = true)
        every { WebViewCompat.setProfile(capture(warmed), any()) } returns Unit
        every { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) } returns Unit
        every { WebViewCompat.removeWebMessageListener(any(), any()) } returns Unit
        every { WebViewCompat.addDocumentStartJavaScript(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loads the resolved url`() {
        prewarmer().prewarm(context, URL)

        assertWarmCount(1)
        assertThat(lastLoadedUrl()).isEqualTo(URL)
    }

    @Test
    fun `ignores urls it cannot warm without loading the WebView provider`() {
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, "http://example.com/index.html")
        prewarmer.prewarm(context, "https://example.com/{{variable}}.html")

        assertWarmCount(0)
        assertThat(prewarmer.queuedCount).isZero()
        // Building a WebView loads the WebView provider, which costs main-thread time.
        verify(exactly = 0) { ProfileStore.getInstance() }
    }

    @Test
    fun `warms a url again once the cache it was warmed into is cleared`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL)
        finishMainFrameLoad(warmed.first())
        idleFor(PaywallWebViewPrewarmer.SETTLE_GRACE_MS)

        prewarmer.onCacheCleared()
        prewarmer.prewarm(context, URL)

        assertWarmCount(2)
        assertThat(lastLoadedUrl()).isEqualTo(URL)
    }

    // A warm that straddles the clear cached against storage that is now gone, so it starts over.
    @Test
    fun `clearing the cache restarts the warms in flight ahead of the queue`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        prewarmer.onCacheCleared()
        idle()

        assertWarmCount(2)
        assertThat(lastLoadedUrl()).isEqualTo(URL)
        assertThat(prewarmer.warmingCount).isEqualTo(1)
        assertThat(prewarmer.queuedCount).isEqualTo(1)
    }

    @Test
    fun `clearing the cache does not requeue a url that is already waiting`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.prewarmAll(URL, OTHER_URL)
        prewarmer.onDisplayStarted(THIRD_URL)

        prewarmer.onCacheCleared()
        idle()

        assertThat(prewarmer.queuedCount + prewarmer.warmingCount).isEqualTo(2)
    }

    @Test
    fun `a load that finished before the clear does not settle the restarted warm`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL)
        // Arms settle(URL) without draining it, so it lands on the warm that replaces this one.
        val released = warmed.first()
        shadowOf(released).webViewClient.onPageFinished(released, URL)

        prewarmer.onCacheCleared()
        idle()
        idleFor(PaywallWebViewPrewarmer.SETTLE_GRACE_MS)

        assertWarmCount(2)
        assertThat(prewarmer.warmingCount).isEqualTo(1)
    }

    @Test
    fun `ignores a url already in flight or queued`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        prewarmer.prewarmAll(URL, OTHER_URL)

        assertWarmCount(1)
        assertThat(prewarmer.queuedCount).isEqualTo(1)
    }

    @Test
    fun `moves on to the next url once the main frame finishes`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        finishMainFrameLoad(warmed.first())
        idleFor(PaywallWebViewPrewarmer.SETTLE_GRACE_MS)

        assertWarmCount(2)
        assertThat(lastLoadedUrl()).isEqualTo(OTHER_URL)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // `destroy()` aborts whatever the document is still fetching from script.
    @Test
    fun `holds the view briefly after the main frame finishes`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        finishMainFrameLoad(warmed.first())

        assertThat(prewarmer.warmingCount).isEqualTo(1)
        assertWarmCount(1)
    }

    @Test
    fun `moves on to the next url when a warm stalls`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        idleFor(PaywallWebViewPrewarmer.WARM_STALL_TIMEOUT_MS)

        assertWarmCount(2)
        assertThat(prewarmer.queuedCount).isZero()
    }

    @Test
    fun `a load failure moves on to the next url`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        failMainFrameLoad(warmed.first())
        idle()

        assertWarmCount(2)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // Warming installs no bridge, so it must not inherit the display path's secure-messaging requirement.
    @Test
    fun `warms even where secure messaging is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns false
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL)

        assertThat(lastLoadedUrl()).isEqualTo(URL)
        assertThat(prewarmer.warmingCount).isEqualTo(1)
    }

    // A missing or mid-update WebView package throws Error, not Exception, and warming runs for apps that
    // may never show a paywall.
    @Test
    fun `keeps going when the WebView package cannot be loaded`() {
        every { WebViewCompat.setProfile(capture(warmed), any()) } throws NoClassDefFoundError("no WebView")
        val prewarmer = prewarmer()

        prewarmer.prewarm(context, URL)
        idle()

        assertThat(prewarmer.warmingCount).isZero()
        assertThat(prewarmer.queuedCount).isZero()
        assertThat(shadowOf(warmed.single()).wasDestroyCalled()).isTrue()
    }

    @Test
    fun `a trim destroys every in-flight view and requeues the unfinished urls`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.prewarmAll(URL, OTHER_URL, THIRD_URL)

        (context as Application).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

        assertThat(warmed.map { shadowOf(it).wasDestroyCalled() }).containsExactly(true, true)
        assertThat(prewarmer.warmingCount).isZero()
        assertThat(prewarmer.queuedCount).isEqualTo(3)
    }

    @Test
    fun `warms up to the concurrency bound and refills a freed slot`() {
        val prewarmer = PaywallWebViewPrewarmer()

        prewarmer.prewarmAll(URL, OTHER_URL, THIRD_URL)

        assertWarmCount(PaywallWebViewPrewarmer.MAX_CONCURRENT_WARMS)
        assertThat(prewarmer.queuedCount).isEqualTo(3 - PaywallWebViewPrewarmer.MAX_CONCURRENT_WARMS)

        finishMainFrameLoad(warmed.first())
        idleFor(PaywallWebViewPrewarmer.SETTLE_GRACE_MS)

        assertWarmCount(3)
        assertThat(lastLoadedUrl()).isEqualTo(THIRD_URL)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // The displayed url is gone for good; a warm that merely shared its slot is only deferred.
    @Test
    fun `displaying a queued url drops it and defers the warms in flight`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.prewarmAll(URL, OTHER_URL, THIRD_URL)

        prewarmer.onDisplayStarted(THIRD_URL)
        prewarmer.onDisplayEnded()
        idle()

        assertThat(loadedUrls()).doesNotContain(THIRD_URL)
        assertThat(prewarmer.warmingCount).isEqualTo(2)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // Leaving it in flight would race the display load it was warming for, on the same main thread and
    // renderer process. Nor may it hand that process straight to the next queued url.
    @Test
    fun `displaying a url abandons its in-flight warm without starting the next one`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)

        prewarmer.onDisplayStarted(URL)
        idle()

        assertWarmCount(1)
        assertThat(prewarmer.warmingCount).isZero()
        assertThat(prewarmer.queuedCount).isEqualTo(1)
    }

    // The display path's own load fills the http cache, so warming it afterwards is pure waste.
    @Test
    fun `never warms a url that was displayed`() {
        val prewarmer = prewarmer()
        prewarmer.onDisplayStarted(URL)
        prewarmer.markWarmed(URL)
        prewarmer.onDisplayEnded()
        idle()

        prewarmer.prewarm(context, URL)

        assertWarmCount(0)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // Display starting is not proof the url got cached: a construction failure between the two calls must
    // not block this url from ever being warmed.
    @Test
    fun `a url is not marked warmed unless markWarmed is called`() {
        val prewarmer = prewarmer()
        prewarmer.onDisplayStarted(URL)
        prewarmer.onDisplayEnded()
        idle()

        prewarmer.prewarm(context, URL)

        assertThat(lastLoadedUrl()).isEqualTo(URL)
    }

    // Every offerings fetch re-announces the same bundles, and a warmed one is already in the http cache.
    @Test
    fun `never warms a url twice`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL)
        finishMainFrameLoad(warmed.first())
        idleFor(PaywallWebViewPrewarmer.SETTLE_GRACE_MS)

        prewarmer.prewarm(context, URL)

        assertWarmCount(1)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // Its document is already cached, so resuming must not pay for the whole load again.
    @Test
    fun `a warm past its main frame is not requeued when a component is displayed`() {
        val prewarmer = prewarmer()
        prewarmer.prewarm(context, URL)
        finishMainFrameLoad(warmed.first())

        prewarmer.onDisplayStarted(DISPLAYED_URL)
        idle()

        assertThat(prewarmer.warmingCount).isZero()
        assertThat(prewarmer.queuedCount).isZero()
    }

    // Every warm runs script on the one renderer thread a displayed component is using, and that component
    // loads what it needs itself.
    @Test
    fun `stops warming while a web_view component is displayed`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.prewarmAll(URL, OTHER_URL, THIRD_URL)

        prewarmer.onDisplayStarted(DISPLAYED_URL)
        idle()

        assertThat(prewarmer.warmingCount).isZero()
        assertThat(prewarmer.queuedCount).isEqualTo(3)
    }

    @Test
    fun `does not start a newly queued url while a component is displayed`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.onDisplayStarted(DISPLAYED_URL)

        prewarmer.prewarm(context, URL)

        assertWarmCount(0)
        assertThat(prewarmer.queuedCount).isEqualTo(1)
    }

    // Resuming lands on the frame a paywall is being dismissed on, where building a WebView is jank.
    @Test
    fun `resumes warming on a later frame once the last displayed component is gone`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.prewarmAll(URL, OTHER_URL)
        prewarmer.onDisplayStarted(DISPLAYED_URL)
        idle()
        warmed.clear()

        prewarmer.onDisplayEnded()

        assertWarmCount(0)

        idle()

        assertThat(prewarmer.warmingCount).isEqualTo(2)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // A paywall can show several web_view components, so the first one going away must not resume warming.
    @Test
    fun `stays paused until every displayed component is gone`() {
        val prewarmer = prewarmer(maxConcurrent = 2)
        prewarmer.prewarm(context, URL)
        prewarmer.onDisplayStarted(DISPLAYED_URL)
        prewarmer.onDisplayStarted(OTHER_URL)

        prewarmer.onDisplayEnded()
        idle()

        assertThat(prewarmer.warmingCount).isZero()

        prewarmer.onDisplayEnded()
        idle()

        assertThat(prewarmer.warmingCount).isEqualTo(1)

        prewarmer.onDisplayEnded()
        prewarmer.prewarm(context, THIRD_URL)

        assertThat(prewarmer.warmingCount).isEqualTo(2)
    }

    // onPageFinished posts its settle, so a display starting in that window requeues a url that is about
    // to be marked warmed.
    @Test
    fun `does not rewarm a url that settled while a component was displayed`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL)
        val view = warmed.single()
        shadowOf(view).webViewClient.onPageFinished(view, URL)

        prewarmer.onDisplayStarted(DISPLAYED_URL)
        idle()
        prewarmer.onDisplayEnded()
        idle()

        assertWarmCount(1)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // A warm already loading survives the UI going away; the customer may be back within seconds.
    @Test
    fun `a hidden UI or a mild foreground trim does not interrupt warming`() {
        val ignored = listOf(
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
        )

        ignored.forEach { level ->
            warmed.clear()
            val prewarmer = prewarmer()
            prewarmer.prewarmAll(URL, OTHER_URL)

            (context as Application).onTrimMemory(level)

            assertThat(prewarmer.warmingCount).describedAs("level %s warming", level).isEqualTo(1)
            assertThat(shadowOf(warmed.single()).wasDestroyCalled())
                .describedAs("level %s destroyed", level).isFalse()
            assertThat(prewarmer.queuedCount).describedAs("level %s queued", level).isEqualTo(1)
        }
    }

    // The warm still finishes and hands its slot to the next url while the UI is hidden.
    @Test
    fun `a warm started before the UI hid still completes`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)
        (context as Application).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

        finishMainFrameLoad(warmed.first())
        idleFor(PaywallWebViewPrewarmer.SETTLE_GRACE_MS)

        assertWarmCount(2)
        assertThat(lastLoadedUrl()).isEqualTo(OTHER_URL)
        assertThat(prewarmer.queuedCount).isZero()
    }

    // Every level except the ignored two frees the view and keeps the urls queued, including those delivered
    // only below API 34, so no threshold or level list can drift from the platform.
    @Test
    fun `every memory-pressure level frees the view and keeps the urls queued`() {
        val levels = listOf(
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
        )

        levels.forEach { level ->
            warmed.clear()
            val prewarmer = prewarmer()
            prewarmer.prewarmAll(URL, OTHER_URL)

            (context as Application).onTrimMemory(level)

            assertThat(prewarmer.warmingCount).describedAs("level %s warming", level).isZero()
            assertThat(shadowOf(warmed.single()).wasDestroyCalled())
                .describedAs("level %s destroyed", level).isTrue()
            assertThat(prewarmer.queuedCount).describedAs("level %s queued", level).isEqualTo(2)
        }
    }

    // Nothing else pumps the queue when the app returns, and the parked url is alreadyCovered, so prewarm
    // has to restart it rather than bail out.
    @Test
    fun `resumes the deferred queue when the same url is announced again`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)
        (context as Application).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        warmed.clear()

        prewarmer.prewarm(context, URL)

        assertWarmCount(1)
        assertThat(lastLoadedUrl()).isEqualTo(URL)
        assertThat(prewarmer.queuedCount).isEqualTo(1)
    }

    @Test
    fun `resumes the deferred queue once a displayed component goes away`() {
        val prewarmer = prewarmer()
        prewarmer.prewarmAll(URL, OTHER_URL)
        (context as Application).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        warmed.clear()

        prewarmer.onDisplayStarted(DISPLAYED_URL)
        prewarmer.onDisplayEnded()
        idle()

        assertThat(prewarmer.warmingCount).isEqualTo(1)
        assertThat(lastLoadedUrl()).isEqualTo(URL)
    }
}
