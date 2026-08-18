package com.revenuecat.purchases.utils

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.net.URL

@RunWith(AndroidJUnit4::class)
internal class OfferingWebViewPrewarmerTest {

    private lateinit var warmer: RecordingWarmer
    private lateinit var prewarmer: OfferingWebViewPrewarmer

    @Before
    fun setUp() {
        warmer = RecordingWarmer()
        prewarmer = prewarmerWith(warmer)
    }

    private fun prewarmerWith(warmer: PaywallAssetWarmer?) = OfferingWebViewPrewarmer(
        PaywallAssetWarming(context = mockk(relaxed = true), warmerProvider = { warmer }),
    )

    // The actual warm is posted to the main thread, so tests need a pump to see it.
    private fun OfferingWebViewPrewarmer.prewarmAndIdle(offerings: Offerings) {
        prewarmWebViews(offerings.prewarmTargetOfferings())
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class RecordingWarmer : PaywallAssetWarmer {
        val warmedWebViewUrls = mutableListOf<String>()
        var prebootCount = 0

        override fun warmImages(context: Context, imageUris: List<Uri>) = Unit

        override fun prebootWebView(context: Context) {
            prebootCount++
        }

        override fun warmWebViewUrls(context: Context, urls: List<String>) {
            warmedWebViewUrls.addAll(urls)
        }
    }

    @Test
    fun `warms the current offering's web_view`() {
        prewarmer.prewarmAndIdle(offeringsWith(current = offeringWithWebViews("a", "https://a.example.com/i.html")))

        assertThat(warmer.warmedWebViewUrls).containsExactly("https://a.example.com/i.html")
    }

    @Test
    fun `warms every web_view in one offering`() {
        val offering = offeringWithWebViews("a", "https://a.example.com/1.html", "https://a.example.com/2.html")

        prewarmer.prewarmAndIdle(offeringsWith(current = offering))

        assertThat(warmer.warmedWebViewUrls)
            .containsExactly("https://a.example.com/1.html", "https://a.example.com/2.html")
    }

    @Test
    fun `warms the offering each placement resolves to`() {
        val current = offeringWithWebViews("current", "https://current.example.com/i.html")
        val onboarding = offeringWithWebViews("onboarding", "https://onboarding.example.com/i.html")
        val settings = offeringWithWebViews("settings", "https://settings.example.com/i.html")

        prewarmer.prewarmAndIdle(
            offeringsWith(
                current = current,
                others = listOf(onboarding, settings),
                placements = Offerings.Placements(
                    fallbackOfferingId = null,
                    offeringIdsByPlacement = mapOf("onboarding" to "onboarding", "settings" to "settings"),
                ),
            ),
        )

        assertThat(warmer.warmedWebViewUrls).containsExactlyInAnyOrder(
            "https://current.example.com/i.html",
            "https://onboarding.example.com/i.html",
            "https://settings.example.com/i.html",
        )
    }

    @Test
    fun `warms the placement fallback offering`() {
        val fallback = offeringWithWebViews("fallback", "https://fallback.example.com/i.html")

        prewarmer.prewarmAndIdle(
            offeringsWith(
                current = null,
                others = listOf(fallback),
                placements = Offerings.Placements(
                    fallbackOfferingId = "fallback",
                    offeringIdsByPlacement = emptyMap(),
                ),
            ),
        )

        assertThat(warmer.warmedWebViewUrls).containsExactly("https://fallback.example.com/i.html")
    }

    // A placement mapped to null is a deliberate "show nothing here".
    @Test
    fun `ignores a placement mapped to no offering`() {
        prewarmer.prewarmAndIdle(
            offeringsWith(
                current = null,
                placements = Offerings.Placements(
                    fallbackOfferingId = null,
                    offeringIdsByPlacement = mapOf("onboarding" to null),
                ),
            ),
        )

        assertThat(warmer.warmedWebViewUrls).isEmpty()
        assertThat(warmer.prebootCount).isZero()
    }

    @Test
    fun `ignores a placement naming an offering that is not in the response`() {
        prewarmer.prewarmAndIdle(
            offeringsWith(
                current = null,
                placements = Offerings.Placements(
                    fallbackOfferingId = null,
                    offeringIdsByPlacement = mapOf("onboarding" to "missing"),
                ),
            ),
        )

        assertThat(warmer.warmedWebViewUrls).isEmpty()
    }

    @Test
    fun `warms a url shared by several offerings only once`() {
        val shared = "https://shared.example.com/i.html"
        val current = offeringWithWebViews("current", shared)
        val other = offeringWithWebViews("other", shared)

        prewarmer.prewarmAndIdle(
            offeringsWith(
                current = current,
                others = listOf(other),
                placements = Offerings.Placements(
                    fallbackOfferingId = null,
                    offeringIdsByPlacement = mapOf("onboarding" to "other"),
                ),
            ),
        )

        assertThat(warmer.warmedWebViewUrls).containsExactly(shared)
    }

    @Test
    fun `does nothing when no offering has a web_view`() {
        prewarmer.prewarmAndIdle(offeringsWith(current = offeringWithoutWebViews("current")))

        assertThat(warmer.warmedWebViewUrls).isEmpty()
        assertThat(warmer.prebootCount).isZero()
    }

    @Test
    fun `preboots the engine when there is something to warm`() {
        prewarmer.prewarmAndIdle(offeringsWith(current = offeringWithWebViews("a", "https://a.example.com/i.html")))

        assertThat(warmer.prebootCount).isEqualTo(1)
    }

    @Test
    fun `collects nothing when no warmer is registered`() {
        val unavailable = prewarmerWith(warmer = null)

        unavailable.prewarmAndIdle(offeringsWith(current = offeringWithWebViews("a", "https://a.example.com/i.html")))

        assertThat(warmer.warmedWebViewUrls).isEmpty()
    }

    @Test
    fun `skips an offering whose component tree fails to decode, and warms the rest`() {
        val broken = mockk<Offering>().apply {
            every { identifier } returns "broken"
            every { paywallComponents } returns Offering.PaywallComponents(
                uiConfig = mockk(),
                componentsHash = "hash",
            ) {
                throw SerializationException("Malformed component tree")
            }
        }
        val healthy = offeringWithWebViews("healthy", "https://healthy.example.com/i.html")

        prewarmer.prewarmAndIdle(
            offeringsWith(
                current = broken,
                others = listOf(healthy),
                placements = Offerings.Placements(
                    fallbackOfferingId = "healthy",
                    offeringIdsByPlacement = emptyMap(),
                ),
            ),
        )

        assertThat(warmer.warmedWebViewUrls).containsExactly("https://healthy.example.com/i.html")
    }

    @Test
    fun `ignores an offering with no paywall components`() {
        val v1Only = mockk<Offering>().apply {
            every { identifier } returns "v1"
            every { paywallComponents } returns null
        }

        prewarmer.prewarmAndIdle(offeringsWith(current = v1Only))

        assertThat(warmer.warmedWebViewUrls).isEmpty()
    }

    private fun offeringWithWebViews(identifier: String, vararg urls: String): Offering =
        offeringWith(
            identifier = identifier,
            components = urls.mapIndexed { index, url ->
                WebViewComponent(
                    url = url,
                    id = "$identifier-component-$index",
                    protocolVersion = WebViewComponent.SUPPORTED_PROTOCOL_VERSION,
                    size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
                )
            },
        )

    private fun offeringWithoutWebViews(identifier: String): Offering = offeringWith(
        identifier = identifier,
        components = listOf(
            TextComponent(
                text = LocalizationKey("key"),
                color = ColorScheme(light = ColorInfo.Alias(ColorAlias(""))),
            ),
        ),
    )

    private fun offeringWith(
        identifier: String,
        components: List<PaywallComponent>,
    ): Offering = mockk<Offering>().apply {
        every { this@apply.identifier } returns identifier
        every { paywallComponents } returns Offering.PaywallComponents(
            uiConfig = mockk(),
            data = PaywallComponentsData(
                id = "paywall_id",
                templateName = "template",
                assetBaseURL = URL("https://assets.example.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = StackComponent(components = components),
                        background = Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias("")))),
                        stickyFooter = null,
                    ),
                ),
                componentsLocalizations = mapOf(),
                defaultLocaleIdentifier = LocaleId("en_US"),
            ),
        )
    }
}
