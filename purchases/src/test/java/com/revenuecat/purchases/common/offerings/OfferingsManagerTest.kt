package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.OfferingImagePreDownloader
import com.revenuecat.purchases.utils.OfferingVideoPredownloader
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsManagerTest {

    private val appUserId = "fakeUserID"
    private val productId = STUB_PRODUCT_IDENTIFIER
    private val testOfferings = stubOfferings(productId).second

    private lateinit var cache: OfferingsCache
    private lateinit var backend: Backend
    private lateinit var offeringsFactory: OfferingsFactory
    private lateinit var offeringImagePreDownloader: OfferingImagePreDownloader
    private lateinit var offeringVideoPreDownloader: OfferingVideoPredownloader
    private lateinit var mockDiagnosticsTracker: DiagnosticsTracker
    private lateinit var mockOfferingFontPreDownloader: OfferingFontPreDownloader

    private lateinit var offeringsManager: OfferingsManager

    @Before
    fun setUp() {
        cache = mockk()
        backend = mockk()
        offeringsFactory = mockk()
        offeringImagePreDownloader = mockk<OfferingImagePreDownloader>().apply {
            every { preDownloadOfferingImages(any()) } just Runs
        }
        mockDiagnosticsTracker = mockk()
        mockOfferingFontPreDownloader = mockk<OfferingFontPreDownloader>().apply {
            every { preDownloadOfferingFontsIfNeeded(any()) } just Runs
        }

        offeringVideoPreDownloader = mockk {
            every { downloadVideos(any()) } just Runs
        }

        mockBackendResponseSuccess()
        mockDiagnosticsTracker()

        offeringsManager = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            offeringVideoPreDownloader = offeringVideoPreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
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
            onSuccess = { receivedOfferings = it },
        )

        assertThat(receivedOfferings).isNotNull
        assertThat(receivedOfferings).isEqualTo(offerings)
        verify {
            cache.cacheOfferings(offerings, any())
        }
    }

    @Test
    fun `get offerings error is called if server error and no cached response`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockBackendResponseError()
        every { cache.cachedOfferingsResponse } returns null
        mockDeviceCache(wasSuccessful = false)

        var purchasesError: PurchasesError? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Should be error") },
        )

        assertThat(purchasesError).isNotNull
        verify(exactly = 1) {
            cache.forceCacheStale()
        }
    }

    @Test
    fun `get offerings success is called if server error and cached response`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockBackendResponseError()
        val backendResponse = JSONObject(ONE_OFFERINGS_RESPONSE)
        every { cache.cachedOfferingsResponse } returns backendResponse
        mockDeviceCache(wasSuccessful = false)
        mockOfferingsFactory()

        var receivedOfferings: Offerings? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("Should be success") },
            onSuccess = { receivedOfferings = it },
        )

        assertThat(receivedOfferings).isEqualTo(testOfferings)

        verify(exactly = 1) { cache.cacheOfferings(testOfferings, backendResponse) }
        verify(exactly = 1) {
            offeringsFactory.createOfferings(
                offeringsJSON = backendResponse,
                onError = any(),
                onSuccess = any(),
            )
        }
    }

    @Test
    fun `get offerings error is called if cached response but not a server error`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)
        mockBackendResponseError(error = expectedError, isServerError = false)
        val backendResponse = JSONObject(ONE_OFFERINGS_RESPONSE)
        every { cache.cachedOfferingsResponse } returns backendResponse
        mockDeviceCache(wasSuccessful = false)
        mockOfferingsFactory()

        var receivedError: PurchasesError? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { receivedError = it },
            onSuccess = { fail("Should be success") },
        )

        assertThat(receivedError).isEqualTo(expectedError)
        verify(exactly = 1) { cache.forceCacheStale() }
    }

    // This situation shouldn't happen normally since we only cache when we have loaded the offerings at least once,
    // but it's possible something changed in the store. So better to handle it.
    @Test
    fun `get offerings error is called if backend error and cached response with invalid offerings`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockBackendResponseError()
        val backendResponse = JSONObject(ONE_OFFERINGS_RESPONSE)
        every { cache.cachedOfferingsResponse } returns backendResponse
        mockDeviceCache(wasSuccessful = false)
        val expectedError = PurchasesError(PurchasesErrorCode.StoreProblemError)
        mockOfferingsFactory(error = expectedError)

        var receivedError: PurchasesError? = null
        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { receivedError = it },
            onSuccess = { fail("Should be error") },
        )

        assertThat(receivedError).isEqualTo(expectedError)

        verify(exactly = 1) { cache.forceCacheStale() }
    }

    // endregion getOfferings

    // region pre download offering videos

    @Test
    fun `getOfferings pre downloads offering videos for current offering`() {
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory()
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 1) {
            offeringVideoPreDownloader.downloadVideos(testOfferings.current!!)
        }
    }

    @Test
    fun `getOfferings does not pre download offering videos if current offering is null`() {
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory(testOfferings.copy(current = null))
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 0) {
            offeringVideoPreDownloader.downloadVideos(any())
        }
    }

    @Test
    fun `getOfferings pre downloads offering videos for current offering when request fails and served from cache`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockBackendResponseError()
        val backendResponse = JSONObject(ONE_OFFERINGS_RESPONSE)
        every { cache.cachedOfferingsResponse } returns backendResponse
        mockDeviceCache(wasSuccessful = false)
        mockOfferingsFactory()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 1) {
            offeringVideoPreDownloader.downloadVideos(testOfferings.current!!)
        }
    }

    // endregion pre download offering videos

    // region pre download offering images

    @Test
    fun `getOfferings pre downloads offering images for current offering`() {
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory()
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 1) {
            offeringImagePreDownloader.preDownloadOfferingImages(testOfferings.current!!)
        }
    }

    @Test
    fun `getOfferings does not pre download offering images if current offering is null`() {
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory(testOfferings.copy(current = null))
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 0) {
            offeringImagePreDownloader.preDownloadOfferingImages(any())
        }
    }

    @Test
    fun `getOfferings pre downloads offering images for current offering when request fails and served from cache`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any())
        } just Runs

        mockBackendResponseError()
        val backendResponse = JSONObject(ONE_OFFERINGS_RESPONSE)
        every { cache.cachedOfferingsResponse } returns backendResponse
        mockDeviceCache(wasSuccessful = false)
        mockOfferingsFactory()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 1) {
            offeringImagePreDownloader.preDownloadOfferingImages(testOfferings.current!!)
        }
    }

    // endregion pre download offering images

    // region pre download font files

    @Test
    fun `getOfferings pre downloads font files for offerings`() {
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory()
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = {},
        )

        verify(exactly = 1) {
            mockOfferingFontPreDownloader.preDownloadOfferingFontsIfNeeded(testOfferings)
        }
    }

    // endregion pre download font files

    // region Get Offerings diagnostics

    @Test
    fun `getOfferings tracks start event`() {
        mockOfferingsFactory()
        every { cache.cachedOfferings } returns null
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = { },
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsStarted()
        }
    }

    @Test
    fun `getOfferings tracks result event on success with NOT_FOUND cache`() {
        mockOfferingsFactory(
            requestedProductIds = setOf(productId),
            notFoundProductIds = emptySet(),
        )
        every { cache.cachedOfferings } returns null
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = { },
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                requestedProductIds = setOf(productId),
                notFoundProductIds = emptySet(),
                errorMessage = null,
                errorCode = null,
                verificationResult = null,
                cacheStatus = DiagnosticsTracker.CacheStatus.NOT_FOUND,
                responseTime = any(),
            )
        }
    }

    @Test
    fun `getOfferings tracks result event on error with NOT_FOUND cache`() {
        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)
        mockBackendResponseError(error = expectedError, isServerError = false)
        every { cache.cachedOfferings } returns null
        every { cache.cachedOfferingsResponse } returns null
        mockDeviceCache(wasSuccessful = false)

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { },
            onSuccess = { fail("should be error") },
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                requestedProductIds = null,
                notFoundProductIds = null,
                errorMessage = expectedError.message,
                errorCode = expectedError.code.code,
                verificationResult = null,
                cacheStatus = DiagnosticsTracker.CacheStatus.NOT_FOUND,
                responseTime = any(),
            )
        }
    }

    @Test
    fun `getOfferings with fetchCurrent tracks result event on success`() {
        mockOfferingsFactory(
            requestedProductIds = setOf(productId),
            notFoundProductIds = emptySet(),
        )
        every { cache.cachedOfferings } returns null
        mockDeviceCache()

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = { },
            fetchCurrent = true,
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                requestedProductIds = setOf(productId),
                notFoundProductIds = emptySet(),
                errorMessage = null,
                errorCode = null,
                verificationResult = null,
                cacheStatus = DiagnosticsTracker.CacheStatus.NOT_CHECKED,
                responseTime = any(),
            )
        }
    }

    @Test
    fun `getOfferings with fetchCurrent tracks result event on error`() {
        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)
        mockBackendResponseError(error = expectedError, isServerError = false)
        every { cache.cachedOfferings } returns null
        mockDeviceCache(wasSuccessful = false)

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { },
            onSuccess = { fail("should be error") },
            fetchCurrent = true,
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                requestedProductIds = null,
                notFoundProductIds = null,
                errorMessage = expectedError.message,
                errorCode = expectedError.code.code,
                verificationResult = null,
                cacheStatus = DiagnosticsTracker.CacheStatus.NOT_CHECKED,
                responseTime = any(),
            )
        }
    }

    @Test
    fun `getOfferings tracks result event on success from STALE cache`() {
        mockOfferingsFactory(
            requestedProductIds = setOf(productId),
            notFoundProductIds = emptySet(),
        )
        every { cache.cachedOfferings } returns testOfferings
        mockDeviceCache()
        mockCacheStale(offeringsStale = true)

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = { },
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                requestedProductIds = null,
                notFoundProductIds = null,
                errorMessage = null,
                errorCode = null,
                verificationResult = null,
                cacheStatus = DiagnosticsTracker.CacheStatus.STALE,
                responseTime = any(),
            )
        }
    }

    @Test
    fun `getOfferings tracks result event on success from VALID cache`() {
        mockOfferingsFactory(
            requestedProductIds = setOf(productId),
            notFoundProductIds = emptySet(),
        )
        every { cache.cachedOfferings } returns testOfferings
        mockDeviceCache()
        mockCacheStale(offeringsStale = false)

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = { },
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                requestedProductIds = null,
                notFoundProductIds = null,
                errorMessage = null,
                errorCode = null,
                verificationResult = null,
                cacheStatus = DiagnosticsTracker.CacheStatus.VALID,
                responseTime = any(),
            )
        }
    }

    // endregion

    // region helpers

    private fun mockOfferingsFactory(
        offerings: Offerings = testOfferings,
        requestedProductIds: Set<String> = setOf(productId),
        notFoundProductIds: Set<String> = emptySet(),
        error: PurchasesError? = null,
    ) {
        if (error == null) {
            every {
                offeringsFactory.createOfferings(
                    offeringsJSON = any(),
                    onError = any(),
                    onSuccess = captureLambda(),
                )
            } answers {
                lambda<(OfferingsResultData) -> Unit>().captured.invoke(
                    OfferingsResultData(
                        offerings,
                        requestedProductIds,
                        notFoundProductIds,
                    ),
                )
            }
        } else {
            every {
                offeringsFactory.createOfferings(
                    offeringsJSON = any(),
                    onError = captureLambda(),
                    onSuccess = any(),
                )
            } answers {
                lambda<(PurchasesError) -> Unit>().captured.invoke(error)
            }
        }
    }

    private fun mockBackendResponseSuccess(response: String = ONE_OFFERINGS_RESPONSE) {
        every {
            backend.getOfferings(any(), any(), captureLambda(), any())
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(JSONObject(response))
        }
    }

    private fun mockBackendResponseError(
        error: PurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError),
        isServerError: Boolean = true,
    ) {
        every {
            backend.getOfferings(any(), any(), any(), captureLambda())
        } answers {
            lambda<(PurchasesError, Boolean) -> Unit>().captured.invoke(error, isServerError)
        }
    }

    private fun mockCacheStale(
        offeringsStale: Boolean = false,
        appInBackground: Boolean = false,
    ) {
        every {
            cache.isOfferingsCacheStale(appInBackground)
        } returns offeringsStale
    }

    private fun mockDeviceCache(wasSuccessful: Boolean = true) {
        if (wasSuccessful) {
            every { cache.cacheOfferings(any(), any()) } just Runs
        } else {
            every { cache.forceCacheStale() } just Runs
        }
    }

    // endregion helpers

    private fun mockDiagnosticsTracker() {
        every { mockDiagnosticsTracker.trackGetOfferingsStarted() } just runs
        every {
            mockDiagnosticsTracker.trackGetOfferingsResult(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs
    }

    private fun Offerings.copy(current: Offering?): Offerings =
        Offerings(
            current = current,
            all = this.all,
        )
}
