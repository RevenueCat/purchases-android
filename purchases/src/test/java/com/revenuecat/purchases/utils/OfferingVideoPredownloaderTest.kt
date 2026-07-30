package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.storage.FileRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.SerializationException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class OfferingVideoPredownloaderTest {

    private lateinit var fileRepository: FileRepository
    private lateinit var predownloader: OfferingVideoPredownloader

    @Before
    fun setUp() {
        fileRepository = mockk(relaxed = true)
        predownloader = OfferingVideoPredownloader(
            context = mockk(relaxed = true),
            canShowPaywalls = true,
            fileRepository = fileRepository,
        )
    }

    @Test
    fun `if no paywall components, it does not prefetch anything`() {
        predownloader.downloadVideos(
            mockk<Offering>().apply { every { paywallComponents } returns null },
        )

        verify(exactly = 0) { fileRepository.prefetch(any()) }
    }

    @Test
    fun `if the component tree fails to decode, it does not throw and prefetches nothing`() {
        val offering = mockk<Offering>().apply {
            every { paywallComponents } returns Offering.PaywallComponents(
                uiConfig = mockk(),
                componentsHash = "hash",
            ) {
                throw SerializationException("Malformed component tree")
            }
        }

        // Pre-downloading is best-effort: a lazy-decode failure must be swallowed so it can't abort the
        // offerings success/caching path that invokes this.
        predownloader.downloadVideos(offering)

        verify(exactly = 0) { fileRepository.prefetch(any()) }
    }

    @Test
    fun `prefetches videos found in the component tree`() {
        val videoComponent = VideoComponent(
            source = ThemeVideoUrls(
                light = VideoUrls(
                    width = 200u,
                    height = 200u,
                    url = URL("https://pawwalls.com/test_video.mp4"),
                    urlLowRes = URL("https://pawwalls.com/test_video_low_res.mp4"),
                ),
                dark = null,
            ),
            fallbackSource = null,
            visible = null,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit()),
            fitMode = FitMode.FILL,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = null,
        )
        val offering = mockk<Offering>().apply {
            every { paywallComponents } returns Offering.PaywallComponents(
                uiConfig = mockk(),
                data = PaywallComponentsData(
                    id = "paywall_id",
                    templateName = "template",
                    assetBaseURL = URL("https://assets.pawwalls.com"),
                    componentsConfig = ComponentsConfig(
                        base = PaywallComponentsConfig(
                            stack = StackComponent(components = listOf(videoComponent)),
                            background = Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias("")))),
                            stickyFooter = null,
                        ),
                    ),
                    componentsLocalizations = mapOf(),
                    defaultLocaleIdentifier = LocaleId("en_US"),
                ),
            )
        }

        predownloader.downloadVideos(offering)

        verify(exactly = 1) { fileRepository.prefetch(any()) }
    }
}
