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
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingFontPreDownloaderTest {

    private val testPackageName = "com.test.app"

    private lateinit var context: Context
    private lateinit var remoteFontLoader: RemoteFontLoader
    private lateinit var preDownloader: OfferingFontPreDownloader

    @Before
    fun setUp() {
        context = mockk {
            every { packageName } returns testPackageName
            every { resources.getIdentifier(any(), "font", testPackageName) } returns 0
            every { resources.assets.list(any()) } returns emptyArray()
        }
        remoteFontLoader = mockk<RemoteFontLoader>().apply {
            every {
                getCachedFontFileOrStartDownload(any(), any())
            } returns null
        }
        preDownloader = OfferingFontPreDownloader(context, remoteFontLoader)
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded does nothing when offerings are empty`() {
        preDownloader.preDownloadOfferingFontsIfNeeded(Offerings(current = null, all = emptyMap()))

        verify(exactly = 0) {
            remoteFontLoader.getCachedFontFileOrStartDownload(any(), any())
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
            remoteFontLoader.getCachedFontFileOrStartDownload(any(), any())
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
            remoteFontLoader.getCachedFontFileOrStartDownload(any(), any())
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded skips bundled fonts and downloads web fonts`() {
        val genericFont = FontsConfig(
            android = FontInfo.Name("sans-serif"),
            web = FontsConfig.WebFontInfo("https://example.com/shouldnotdownloadfont.ttf", "hash123"),
        )
        val resourceFont = FontsConfig(
            android = FontInfo.Name("testFontInResources"),
            web = FontsConfig.WebFontInfo("https://example.com/shouldnotdownloadfont.ttf", "hash123"),
        )
        val assetFont = FontsConfig(
            android = FontInfo.Name("testFontInAssets"),
            web = FontsConfig.WebFontInfo("https://example.com/shouldnotdownloadfont.ttf", "hash123"),
        )
        val downloadableFont = FontsConfig(
            android = FontInfo.Name("notbundledFont"),
            web = FontsConfig.WebFontInfo("https://example.com/downloadable-font.ttf", "hash456"),
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
            remoteFontLoader.getCachedFontFileOrStartDownload(
                url = "https://example.com/downloadable-font.ttf",
                expectedMd5 = "hash456",
            )
        }
    }

    @Test
    fun `preDownloadOfferingFontsIfNeeded skips malformed URLs`() {
        val downloadableFont = FontsConfig(
            android = FontInfo.Name("notbundledFont"),
            web = FontsConfig.WebFontInfo("malformed-url", "hash456"),
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
            remoteFontLoader.getCachedFontFileOrStartDownload(any(), any())
        }
    }
} 
