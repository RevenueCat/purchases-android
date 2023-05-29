package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsManagerTest {

    private val appUserId = "fakeUserID"
    private val productId = STUB_PRODUCT_IDENTIFIER
    private val testOfferings = stubOfferings(productId).second

    private lateinit var cache: OfferingsCache
    private lateinit var backend: Backend
    private lateinit var offeringsFactory: OfferingsFactory

    private lateinit var offeringsManager: OfferingsManager

    @Before
    fun setUp() {
        cache = mockk()
        backend = mockk()
        offeringsFactory = mockk()

        mockBackendResponse()

        offeringsManager = OfferingsManager(
            cache,
            backend,
            offeringsFactory
        )
    }

    // region onAppForeground

    @Test
    fun `fetch offerings on app foreground if it's stale`() {
        mockCacheStale(offeringsStale = true)
        mockDeviceCache()
        mockOfferingsFactory()
        offeringsManager.onAppForeground(appUserId)
        verify(exactly = 1) {
            backend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            cache.isOfferingsCacheStale(appInBackground = false)
        }
    }

    @Test
    fun `does not fetch offerings on app foreground if it's not stale`() {
        mockCacheStale(offeringsStale = false)
        offeringsManager.onAppForeground(appUserId)
        verify(exactly = 0) {
            backend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            cache.isOfferingsCacheStale(appInBackground = false)
        }
    }

    // endregion onAppForeground

    // region getOfferings

    @Test
    fun `if cached offerings are not stale`() {
        mockOfferingsFactory()

        every {
            cache.cachedOfferings
        } returns testOfferings
        mockCacheStale(offeringsStale = false)
        mockDeviceCache()

        var receivedOfferings: Offerings? = null

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("Expected success but got error: $it") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isEqualTo(testOfferings)
    }

    @Test
    fun `if cached offerings are not stale in background`() {
        mockOfferingsFactory()

        every {
            cache.cachedOfferings
        } returns testOfferings
        mockCacheStale(offeringsStale = true, appInBackground = true)
        mockDeviceCache()

        var receivedOfferings: Offerings? = null

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = true,
            onError = { fail("Expected success but got error: $it") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isEqualTo(testOfferings)
    }

    @Test
    fun `if no cached offerings, backend is hit when getting offerings`() {
        mockOfferingsFactory()

        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockDeviceCache()

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { Assert.fail("should be a success") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isNotNull

        verify(exactly = 1) {
            backend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            cache.cacheOfferings(any(), any())
        }
    }

    @Test
    fun `if no cached offerings, backend is hit when getting offerings when on background`() {
        mockDeviceCache()
        mockOfferingsFactory()

        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = true,
            onError = { Assert.fail("should be a success") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isNotNull

        verify(exactly = 1) {
            backend.getOfferings(appUserId, appInBackground = true, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            cache.cacheOfferings(any(), any())
        }
    }

    @Test
    fun `products are populated when getting offerings`() {
        mockOfferingsFactory()

        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockDeviceCache()

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { Assert.fail("should be a success") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isNotNull
        assertThat(receivedOfferings!!.all.size).isEqualTo(1)
        assertThat(receivedOfferings!![STUB_OFFERING_IDENTIFIER]!!.monthly!!.product).isNotNull
    }

    @Test
    fun `if cached offerings are stale, call backend`() {
        val (_, offerings) = stubOfferings(productId)
        mockOfferingsFactory(offerings)

        every {
            cache.cachedOfferings
        } returns offerings
        mockCacheStale(offeringsStale = true)
        mockDeviceCache()

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { Assert.fail("should be a success") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isEqualTo(offerings)
        verify(exactly = 1) {
            backend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `if cached offerings are stale when on background, call backend`() {
        val (_, offerings) = stubOfferings(productId)
        mockOfferingsFactory(offerings)

        every {
            cache.cachedOfferings
        } returns offerings
        mockCacheStale(offeringsStale = true, appInBackground = true)
        mockDeviceCache()

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = true,
            onError = { fail("should be a success") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isEqualTo(offerings)
        verify(exactly = 1) {
            backend.getOfferings(appUserId, appInBackground = true, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun getOfferingsIsCached() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs
        mockDeviceCache()
        val (_, offerings) = stubOfferings(productId)
        mockOfferingsFactory(offerings)

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { Assert.fail("should be a success") },
            onSuccess = { receivedOfferings = it }
        )

        assertThat(receivedOfferings).isNotNull
        assertThat(receivedOfferings).isEqualTo(offerings)
        verify {
            cache.cacheOfferings(offerings, any())
        }
    }

    @Test
    fun getOfferingsErrorIsCalledIfNoBackendResponse() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        every {
            backend.getOfferings(
                any(),
                any(),
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.StoreProblemError)
            )
        }

        mockDeviceCache(wasSuccessful = false)

        var purchasesError: PurchasesError? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Should be error") }
        )

        assertThat(purchasesError).isNotNull
        verify(exactly = 1) {
            cache.clearOfferingsCacheTimestamp()
        }
    }

    // endregion getOfferings

    // region helpers

    private fun mockOfferingsFactory(
        offerings: Offerings = testOfferings,
        error: PurchasesError? = null
    ) {
        if (error == null) {
            every {
                offeringsFactory.createOfferings(any(), any(), captureLambda())
            } answers {
                lambda<(Offerings) -> Unit>().captured.invoke(offerings)
            }
        } else {
            every {
                offeringsFactory.createOfferings(any(), captureLambda(), any())
            } answers {
                lambda<(PurchasesError) -> Unit>().captured.invoke(error)
            }
        }
    }

    private fun mockBackendResponse(response: String = ONE_OFFERINGS_RESPONSE) {
        every {
            backend.getOfferings(any(), any(), captureLambda(), any())
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(JSONObject(response))
        }
    }

    private fun mockCacheStale(
        offeringsStale: Boolean = false,
        appInBackground: Boolean = false
    ) {
        every {
            cache.isOfferingsCacheStale(appInBackground)
        } returns offeringsStale
    }

    private fun mockDeviceCache(wasSuccessful: Boolean = true) {
        every { cache.setOfferingsCacheTimestampToNow() } just Runs
        if (wasSuccessful) {
            every { cache.cacheOfferings(any(), any()) } just Runs
        } else {
            every { cache.clearOfferingsCacheTimestamp() } just Runs
        }
    }

    // endregion helpers
}
