package com.revenuecat.purchases.utils

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class OfferingImagePreDownloaderTest {

    private lateinit var coilImageDownloader: CoilImageDownloader

    private lateinit var preDownloader: OfferingImagePreDownloader

    private val zeroDecimalPlaceCountries = listOf("PH", "KZ", "TW", "MX", "TH")

    @Before
    fun setUp() {
        coilImageDownloader = mockk<CoilImageDownloader>().apply {
            every { downloadImage(any()) } just Runs
        }

        preDownloader = OfferingImagePreDownloader(shouldPredownloadImages = true, coilImageDownloader)
    }

    @Test
    fun `if no paywall data or paywall components data, it does not download anything`() {
        preDownloader.preDownloadOfferingImages(
            mockk<Offering>().apply {
                every { paywall } returns null
                every { paywallComponents } returns null
            }
        )

        verify(exactly = 0) {
            coilImageDownloader.downloadImage(any())
        }
    }

    @Test
    fun `if disabled, it does not download anything`() {
        preDownloader = OfferingImagePreDownloader(shouldPredownloadImages = false, coilImageDownloader)
        preDownloader.preDownloadOfferingImages(createOfferings())

        verify(exactly = 0) {
            coilImageDownloader.downloadImage(any())
        }
    }

    // region Paywalls V1

    @Test
    fun `downloads images from offering paywall data`() {
        preDownloader.preDownloadOfferingImages(createOfferings())

        verifyAll {
            coilImageDownloader.downloadImage(Uri.parse("https://www.revenuecat.com/test_header.png"))
            coilImageDownloader.downloadImage(Uri.parse("https://www.revenuecat.com/test_background.png"))
            coilImageDownloader.downloadImage(Uri.parse("https://www.revenuecat.com/test_icon.png"))
        }
    }

    @Test
    fun `if no images, it does not download anything`() {
        preDownloader.preDownloadOfferingImages(createOfferings(null, null, null))

        verify(exactly = 0) {
            coilImageDownloader.downloadImage(any())
        }
    }

    // endregion Paywalls V1

    // region Paywalls V2

    @Test
    fun `paywalls V2 - if no images, it does not download anything`() {
        preDownloader.preDownloadOfferingImages(createOfferingWithV2Paywall())

        verify(exactly = 0) {
            coilImageDownloader.downloadImage(any())
        }
    }

    @Test
    fun `paywalls V2 - if images, it downloads all of them`() {
        val expectedImageDownloads = listOf(
            "https://pawwalls.com/test_stack_light_low_res.webp",
            "https://pawwalls.com/test_stack_dark_low_res.webp",
            "https://pawwalls.com/test_icon_1.webp",
            "https://pawwalls.com/test_image_light_low_res.webp",
            "https://pawwalls.com/test_image_dark_low_res.webp",
            "https://pawwalls.com/test_image_override_light_low_res.webp",
            "https://pawwalls.com/test_image_override_dark_low_res.webp",
            "https://pawwalls.com/test_carousel_light_low_res.webp",
            "https://pawwalls.com/test_carousel_dark_low_res.webp",
            "https://pawwalls.com/test_tabs_light_low_res.webp",
            "https://pawwalls.com/test_background_light_low_res.webp",
            "https://pawwalls.com/test_background_dark_low_res.webp",
            "https://pawwalls.com/test_sticky_footer_low_res.webp",
            "https://pawwalls.com/test_sticky_footer_override_light_low_res.webp",
            "https://pawwalls.com/test_sticky_footer_override_dark_low_res.webp",
            "https://pawwalls.com/test_sticky_footer_override_2_light_low_res.webp",
            "https://pawwalls.com/test_sticky_footer_override_2_dark_low_res.webp",
            "https://pawwalls.com/test_icon_2.webp",
            "https://pawwalls.com/test_icon_3.webp",
            "https://pawwalls.com/test_icon_4.webp",
        )

        preDownloader.preDownloadOfferingImages(createOfferingWithV2Paywall(
            paywallComponentsConfig = PaywallComponentsConfig(
                stack = StackComponent(
                    components = listOf(
                        StackComponent(
                            components = emptyList(),
                            background = Background.Image(
                                value = ThemeImageUrls(
                                    light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_stack_light_low_res.webp"),
                                    dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_stack_dark_low_res.webp"),
                                ),
                            ),
                        ),
                        TextComponent(
                            text = localizationKey,
                            color = ColorScheme(light = ColorInfo.Alias(ColorAlias(""))),
                        ),
                        IconComponent(
                            baseUrl = "https://pawwalls.com",
                            iconName = "test_icon",
                            formats = IconComponent.Formats(
                                webp = "test_icon_1.webp",
                            ),
                        ),
                        ImageComponent(
                            source = ThemeImageUrls(
                                light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_image_light_low_res.webp"),
                                dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_image_dark_low_res.webp"),
                            ),
                            overrides = listOf(
                                ComponentOverride(
                                    conditions = emptyList(),
                                    properties = PartialImageComponent(
                                        source = ThemeImageUrls(
                                            light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_image_override_light_low_res.webp"),
                                            dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_image_override_dark_low_res.webp"),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                        CarouselComponent(
                            pages = emptyList(),
                            pageAlignment = VerticalAlignment.TOP,
                            background = Background.Image(
                                value = ThemeImageUrls(
                                    light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_carousel_light_low_res.webp"),
                                    dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_carousel_dark_low_res.webp"),
                                ),
                            ),
                        ),
                        TabsComponent(
                            control = TabsComponent.TabControl.Toggle(stack = StackComponent(components = emptyList())),
                            tabs = emptyList(),
                            background = Background.Image(
                                value = ThemeImageUrls(
                                    light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_tabs_light_low_res.webp"),
                                ),
                            ),
                        ),
                        TimelineComponent(
                            itemSpacing = 0,
                            textSpacing = 0,
                            columnGutter = 0,
                            iconAlignment = TimelineComponent.IconAlignment.Title,
                            items = listOf(
                                TimelineComponent.Item(
                                    title = TextComponent(
                                        text = localizationKey,
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias(""))),
                                    ),
                                    icon = IconComponent(
                                        baseUrl = "https://pawwalls.com",
                                        iconName = "test_icon",
                                        formats = IconComponent.Formats(
                                            webp = "test_icon_3.webp",
                                        ),
                                    ),
                                ),
                                TimelineComponent.Item(
                                    title = TextComponent(
                                        text = localizationKey,
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias(""))),
                                    ),
                                    icon = IconComponent(
                                        baseUrl = "https://pawwalls.com",
                                        iconName = "test_icon",
                                        formats = IconComponent.Formats(
                                            webp = "test_icon_4.webp",
                                        ),
                                    ),
                                )
                            ),
                        )
                    )
                ),
                background = Background.Image(
                    value = ThemeImageUrls(
                        light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_background_light_low_res.webp"),
                        dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_background_dark_low_res.webp"),
                    ),
                ),
                stickyFooter = StickyFooterComponent(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = localizationKey,
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("")))
                            ),
                            IconComponent(
                                baseUrl = "https://pawwalls.com",
                                iconName = "test_icon",
                                formats = IconComponent.Formats(
                                    webp = "test_icon_2.webp",
                                ),
                            ),
                        ),
                        background = Background.Image(
                            value = ThemeImageUrls(
                                light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_sticky_footer_low_res.webp"),
                                dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_sticky_footer_low_res.webp"),
                            ),
                        ),
                        overrides = listOf(
                            ComponentOverride(
                                conditions = emptyList(),
                                properties = PartialStackComponent(
                                    background = Background.Image(
                                        value = ThemeImageUrls(
                                            light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_sticky_footer_override_light_low_res.webp"),
                                            dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_sticky_footer_override_dark_low_res.webp"),
                                        ),
                                    ),
                                ),
                            ),
                            ComponentOverride(
                                conditions = emptyList(),
                                properties = PartialStackComponent(
                                    background = Background.Image(
                                        value = ThemeImageUrls(
                                            light = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_sticky_footer_override_2_light_low_res.webp"),
                                            dark = createMockImageUrls(webpLowRes = "https://pawwalls.com/test_sticky_footer_override_2_dark_low_res.webp"),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ))

        verifyAll {
            expectedImageDownloads.forEach { url ->
                coilImageDownloader.downloadImage(Uri.parse(url))
            }
        }
    }


    // endregion Paywalls V2

    private fun createOfferings(
        header: String? = "test_header.png",
        background: String? = "test_background.png",
        icon: String? = "test_icon.png",
    ): Offering {
        return mockk<Offering>().apply {
            every { paywall } returns PaywallData(
                templateName = "mock-template",
                config = mockk<PaywallData.Configuration>().apply {
                    every { images } returns PaywallData.Configuration.Images(
                        header = header,
                        background = background,
                        icon = icon,
                    )
                },
                assetBaseURL = URL("https://www.revenuecat.com/"),
                revision = 0,
                localization = mockk(),
                zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
            )
            every { paywallComponents } returns null
        }
    }


    private val localizationKey = LocalizationKey("hello-world")

    private fun createOfferingWithV2Paywall(
        paywallComponentsConfig: PaywallComponentsConfig = PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = localizationKey,
                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias("")))
                    )
                )
            ),
            background = Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias("")))),
            stickyFooter = null,
        ),
    ): Offering {
        return mockk<Offering>().apply {
            every { paywall } returns null
            every { paywallComponents } returns Offering.PaywallComponents(
                uiConfig = mockk(),
                data = PaywallComponentsData(
                    id = "paywall_id",
                    templateName = "template",
                    assetBaseURL = URL("https://assets.pawwalls.com"),
                    componentsConfig = ComponentsConfig(
                        base = paywallComponentsConfig,
                    ),
                    componentsLocalizations = mapOf(),
                    defaultLocaleIdentifier = LocaleId("en_US"),
                )
            )
        }
    }

    private fun createMockImageUrls(
        original: String = "https://www.revenuecat.com/test_image.png",
        webp: String = "https://www.revenuecat.com/test_image.webp",
        webpLowRes: String = "https://www.revenuecat.com/test_image_low_res.webp",
    ): ImageUrls {
        return ImageUrls(
            original = URL(original),
            webp = URL(webp),
            webpLowRes = URL(webpLowRes),
            width = 200u,
            height = 200u,
        )
    }
}
