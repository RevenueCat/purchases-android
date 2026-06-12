package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.storage.FileRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.SerializationException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
            every { paywallComponents } returns Offering.PaywallComponents(uiConfig = mockk()) {
                throw SerializationException("Malformed component tree")
            }
        }

        // Pre-downloading is best-effort: a lazy-decode failure must be swallowed so it can't abort the
        // offerings success/caching path that invokes this.
        predownloader.downloadVideos(offering)

        verify(exactly = 0) { fileRepository.prefetch(any()) }
    }
}
