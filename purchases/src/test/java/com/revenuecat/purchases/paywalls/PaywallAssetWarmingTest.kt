package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PaywallAssetWarmingTest {

    private val context = mockk<Context>(relaxed = true)
    private val uri = Uri.parse("https://example.com/image.webp")
    private val webViewUrl = "https://example.com/index.html"

    private fun warming(warmer: PaywallAssetWarmer?) =
        PaywallAssetWarming(context, warmerProvider = { warmer })

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class RecordingWarmer : PaywallAssetWarmer {
        val warmed = mutableListOf<Uri>()
        var prebootCount = 0
        val warmedWebViewUrls = mutableListOf<String>()
        override fun warmImages(context: Context, imageUris: List<Uri>) {
            warmed.addAll(imageUris)
        }

        override fun prebootWebView(context: Context) {
            prebootCount++
        }

        override fun warmWebViewUrls(context: Context, urls: List<String>) {
            warmedWebViewUrls.addAll(urls)
        }
    }

    @Test
    fun `reports unavailable when nothing is registered`() {
        assertThat(warming(warmer = null).isAvailable).isFalse()
    }

    @Test
    fun `reports available when a warmer is registered`() {
        assertThat(warming(RecordingWarmer()).isAvailable).isTrue()
    }

    @Test
    fun `hands the uris to the registered warmer`() {
        val warmer = RecordingWarmer()

        warming(warmer).warmImages(listOf(uri))

        assertThat(warmer.warmed).containsExactly(uri)
    }

    @Test
    fun `does nothing when nothing is registered`() {
        warming(warmer = null).warmImages(listOf(uri))
    }

    @Test
    fun `does not call the warmer with an empty list`() {
        val warmer = RecordingWarmer()

        warming(warmer).warmImages(emptyList())

        assertThat(warmer.warmed).isEmpty()
    }

    // Posted, so nothing warms on the caller's frame.
    @Test
    fun `warms web_view urls after preboot, not inline with it`() {
        val warmer = RecordingWarmer()

        warming(warmer).warmWebViewUrls(listOf(webViewUrl))

        assertThat(warmer.prebootCount).isEqualTo(1)
        assertThat(warmer.warmedWebViewUrls).isEmpty()

        idle()

        assertThat(warmer.warmedWebViewUrls).containsExactly(webViewUrl)
    }

    @Test
    fun `does not warm web_view urls when nothing is registered`() {
        warming(warmer = null).warmWebViewUrls(listOf(webViewUrl))
    }

    @Test
    fun `neither warms nor preboots for an empty url list`() {
        val warmer = RecordingWarmer()

        warming(warmer).warmWebViewUrls(emptyList())
        idle()

        assertThat(warmer.warmedWebViewUrls).isEmpty()
        assertThat(warmer.prebootCount).isZero()
    }

    @Test
    fun `preboots the web view through the registered warmer`() {
        val warmer = RecordingWarmer()

        warming(warmer).prebootWebView()

        assertThat(warmer.prebootCount).isEqualTo(1)
    }

    @Test
    fun `does not preboot when nothing is registered`() {
        warming(warmer = null).prebootWebView()
    }

    @Test
    fun `does not propagate a failure from the warmer`() {
        val throwingWarmer = object : PaywallAssetWarmer {
            override fun warmImages(context: Context, imageUris: List<Uri>) = throw RuntimeException("boom")
            override fun prebootWebView(context: Context) = throw RuntimeException("boom")
            override fun warmWebViewUrls(context: Context, urls: List<String>) = throw RuntimeException("boom")
        }

        warming(throwingWarmer).warmImages(listOf(uri))
        warming(throwingWarmer).prebootWebView()
        warming(throwingWarmer).warmWebViewUrls(listOf("https://example.com/index.html"))
        idle()
    }

    @Test
    fun `does not look for a warmer until it is asked for one`() {
        var lookups = 0
        val warming = PaywallAssetWarming(context, warmerProvider = { lookups++; null })

        assertThat(lookups).isEqualTo(0)

        warming.isAvailable
        warming.isAvailable

        assertThat(lookups).isEqualTo(1)
    }
}
