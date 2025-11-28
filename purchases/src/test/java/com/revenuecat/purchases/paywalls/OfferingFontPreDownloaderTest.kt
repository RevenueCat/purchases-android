package com.revenuecat.purchases.paywalls

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.UiConfig.AppConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingFontPreDownloaderTest {

    private val testPackageName = "com.test.app"

    private lateinit var context: Context
    private lateinit var fontLoader: FontLoader
    private lateinit var preDownloader: OfferingFontPreDownloader

    @Before
    fun setUp() {
        context = mockk {
            every { packageName } returns testPackageName
            every { resources.getIdentifier(any(), "font", testPackageName) } returns 0
            every { resources.assets.list(any()) } returns emptyArray()
        }
        fontLoader = mockk<FontLoader>().apply {
            every {
                getCachedFontFamilyOrStartDownload(any())
            } returns null
        }
        preDownloader = OfferingFontPreDownloader(context, fontLoader)
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded does nothing when offerings are empty`() {
        preDownloader.preDownloadOfferingFontsIfNeeded(Offerings(current = null, all = emptyMap()))

        verify(exactly = 0) {
            fontLoader.getCachedFontFamilyOrStartDownload(any())
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded does nothing when offering has no paywall components`() {
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = null,
        )
        preDownloader.preDownloadOfferingFontsIfNeeded(Offerings(null, mapOf("offering-id" to offering)))

        verify(exactly = 0) {
            fontLoader.getCachedFontFamilyOrStartDownload(any())
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded does nothing when offering has no fonts`() {
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(
                    app = AppConfig(
                        fonts = emptyMap()
                    )
                ),
                data = mockk(),
            ),
        )
        preDownloader.preDownloadOfferingFontsIfNeeded(Offerings(null, mapOf("offering-id" to offering)))

        verify(exactly = 0) {
            fontLoader.getCachedFontFamilyOrStartDownload(any())
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded skips bundled fonts and downloads web fonts`() {
        val genericFont = FontsConfig(
            android = FontInfo.Name(
                value = "sans-serif",
                family = "test-family",
                weight = 400,
                style = FontStyle.NORMAL,
                url = "https://example.com/shouldnotdownloadfont.ttf",
                hash = "hash123",
            ),
        )
        val resourceFont = FontsConfig(
            android = FontInfo.Name(
                value = "testFontInResources",
                family = "test-family",
                weight = 400,
                style = FontStyle.NORMAL,
                url = "https://example.com/shouldnotdownloadfont.ttf",
                hash = "hash123",
            ),
        )
        val assetFont = FontsConfig(
            android = FontInfo.Name(
                value = "testFontInAssets",
                family = "test-family",
                weight = 400,
                style = FontStyle.NORMAL,
                url = "https://example.com/shouldnotdownloadfont.ttf",
                hash = "hash123",
            ),
        )
        val downloadableFont = FontsConfig(
            android = FontInfo.Name(
                value = "notbundledFont",
                family = "test-family",
                weight = 400,
                style = FontStyle.NORMAL,
                url = "https://example.com/shoulddownload.ttf",
                hash = "hash123",
            ),
        )

        every {
            context.resources.getIdentifier("testFontInResources", "font", testPackageName)
        } returns 1
        every { context.assets.list(any()) } returns arrayOf("testFontInAssets")

        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(
                    app = AppConfig(
                        fonts = mapOf(
                            FontAlias("genericFont") to genericFont,
                            FontAlias("resourceFont") to resourceFont,
                            FontAlias("assetFont") to assetFont,
                            FontAlias("downloadableFont") to downloadableFont,
                        )
                    )
                ),
                data = mockk(),
            ),
        )
        preDownloader.preDownloadOfferingFontsIfNeeded(Offerings(null, mapOf("offering" to offering)))

        verify(exactly = 1) {
            fontLoader.getCachedFontFamilyOrStartDownload(
                fontInfo = FontInfo.Name(
                    value = "notbundledFont",
                    family = "test-family",
                    weight = 400,
                    style = FontStyle.NORMAL,
                    url = "https://example.com/shoulddownload.ttf",
                    hash = "hash123",
                )
            )
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded skips malformed URLs`() {
        val downloadableFont = FontsConfig(
            FontInfo.Name(
                value = "notbundledFont",
                family = "test-family",
                weight = 400,
                style = FontStyle.NORMAL,
                url = "invalid-url", // Malformed URL
                hash = "hash123",
            )
        )

        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(
                    app = AppConfig(
                        fonts = mapOf(
                            FontAlias("downloadableFont") to downloadableFont,
                        )
                    )
                ),
                data = mockk(),
            ),
        )
        preDownloader.preDownloadOfferingFontsIfNeeded(Offerings(null, mapOf("offering" to offering)))

        verify(exactly = 0) {
            fontLoader.getCachedFontFamilyOrStartDownload(any())
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded still downloads when first offering lacks paywall components`() {
        val downloadableFont = FontsConfig(
            android = FontInfo.Name(
                value = "remoteFont",
                family = "test-family",
                weight = 400,
                style = FontStyle.NORMAL,
                url = "https://example.com/font.ttf",
                hash = "hash123",
            ),
        )
        val offeringWithPaywall = Offering(
            identifier = "with-paywall",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(
                    app = AppConfig(
                        fonts = mapOf(FontAlias("downloadableFont") to downloadableFont)
                    )
                ),
                data = mockk(),
            ),
        )
        val offeringWithoutPaywall = Offering(
            identifier = "no-paywall",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = null,
        )

        preDownloader.preDownloadOfferingFontsIfNeeded(
            Offerings(
                current = null,
                all = linkedMapOf(
                    "no-paywall" to offeringWithoutPaywall,
                    "with-paywall" to offeringWithPaywall,
                ),
            )
        )

        verify(exactly = 1) {
            fontLoader.getCachedFontFamilyOrStartDownload(downloadableFont.android as FontInfo.Name)
        }
    }
} 
