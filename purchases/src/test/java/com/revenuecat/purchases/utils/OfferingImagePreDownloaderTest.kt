package com.revenuecat.purchases.utils

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
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

    @Before
    fun setUp() {
        coilImageDownloader = mockk<CoilImageDownloader>().apply {
            every { downloadImage(any()) } just Runs
        }

        preDownloader = OfferingImagePreDownloader(shouldPredownloadImages = true, coilImageDownloader)
    }

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
    fun `if no paywall data, it does not download anything`() {
        preDownloader.preDownloadOfferingImages(mockk<Offering>().apply { every { paywall } returns null })

        verify(exactly = 0) {
            coilImageDownloader.downloadImage(any())
        }
    }

    @Test
    fun `if no images, it does not download anything`() {
        preDownloader.preDownloadOfferingImages(createOfferings(null, null, null))

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
                zeroDecimalPlaceCountries = listOf("PH", "KZ", "TW", "MX", "TH"),
            )
        }
    }
}
