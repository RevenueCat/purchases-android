package com.revenuecat.purchases.paywalls

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaywallAssetWarmingTest {

    private val context = mockk<Context>(relaxed = true)
    private val uri = Uri.parse("https://example.com/image.webp")

    private fun warming(warmer: PaywallAssetWarmer?) =
        PaywallAssetWarming(context, warmerProvider = { warmer })

    private class RecordingWarmer : PaywallAssetWarmer {
        val warmed = mutableListOf<Uri>()
        var prebootCount = 0
        override fun warmImages(context: Context, imageUris: List<Uri>) {
            warmed.addAll(imageUris)
        }

        override fun prebootWebView(context: Context) {
            prebootCount++
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

    // Warming runs inline on the offerings success path, before the offerings are cached and handed to the
    // app, so a misbehaving implementation must not take that path down with it.
    @Test
    fun `does not propagate a failure from the warmer`() {
        val throwingWarmer = object : PaywallAssetWarmer {
            override fun warmImages(context: Context, imageUris: List<Uri>) = throw RuntimeException("boom")
            override fun prebootWebView(context: Context) = throw RuntimeException("boom")
        }

        warming(throwingWarmer).warmImages(listOf(uri))
        warming(throwingWarmer).prebootWebView()
    }

    // The ServiceLoader scan is not free, and configure() constructs this before any paywall exists.
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
