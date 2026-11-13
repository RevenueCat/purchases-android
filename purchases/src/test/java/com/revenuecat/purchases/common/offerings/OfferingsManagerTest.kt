package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackagePurchaseReference
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.GetOfferingsErrorHandlingBehavior
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.workflows.WorkflowManager
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.MockHandlerFactory
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.OfferingImagePreDownloader
import com.revenuecat.purchases.utils.OfferingWebViewPrewarmer
import com.revenuecat.purchases.utils.prewarmTargetOfferings
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(InternalRevenueCatAPI::class)
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
    private lateinit var mockDiagnosticsTracker: DiagnosticsTracker
    private lateinit var mockOfferingFontPreDownloader: OfferingFontPreDownloader
    private lateinit var mockOfferingWebViewPrewarmer: OfferingWebViewPrewarmer
    private lateinit var mockWorkflowManager: WorkflowManager

    private lateinit var offeringsManager: OfferingsManager

    @Before
    fun setUp() {
        cache = mockk<OfferingsCache>().apply { every { epoch } returns 0 }
        backend = mockk()
        offeringsFactory = mockk()
        offeringImagePreDownloader = mockk<OfferingImagePreDownloader>().apply {
            every { preDownloadOfferingImages(any()) } just Runs
        }
        mockDiagnosticsTracker = mockk()
        mockOfferingFontPreDownloader = mockk<OfferingFontPreDownloader>().apply {
            every { preDownloadOfferingFontsIfNeeded(any()) } just Runs
        }
        mockOfferingWebViewPrewarmer = mockk<OfferingWebViewPrewarmer>().apply {
            every { prewarmWebViews(any()) } just Runs
        }
        mockWorkflowManager = mockk(relaxed = true)
        every { mockWorkflowManager.onPaywallConfigReady(onComplete = any()) } answers {
            firstArg<() -> Unit>().invoke()
        }

        mockBackendResponseSuccess()
        mockDiagnosticsTracker()

        offeringsManager = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = SyncDispatcher(),
            workflowManager = mockWorkflowManager,
        )
    }

    // region clearInMemoryOfferingsCache

    @Test
    fun `clearInMemoryOfferingsCache delegates to cache`() {
        every { cache.clearInMemoryOfferingsCache() } just Runs
        offeringsManager.clearInMemoryOfferingsCache()
        verify(exactly = 1) { cache.clearInMemoryOfferingsCache() }
    }

    // endregion clearInMemoryOfferingsCache

    @Test
    fun `a network fetch caches the response body as received`() {
        mockDeviceCache()
        mockOfferingsFactory()
        mockBackendResponseSuccess()

        offeringsManager.fetchAndCacheOfferings(appUserId, appInBackground = false)

        // Verbatim, not a re-serialization of the parsed JSON: that allocation is what OOMed.
        verify(exactly = 1) { cache.cacheOfferings(testOfferings, ONE_OFFERINGS_RESPONSE, any()) }
    }

    @Test
    fun `in-flight fetch write lands after a clear`() {
        every { cache.clearInMemoryOfferingsCache() } just Runs
        mockDeviceCache()
        val onSuccessSlot = captureFactoryOnSuccess()

        offeringsManager.fetchAndCacheOfferings(appUserId, appInBackground = false)
        // A clear (e.g. the locale override path) must not drop the late write.
        offeringsManager.clearInMemoryOfferingsCache()
        onSuccessSlot.captured.invoke(OfferingsResultData(testOfferings, setOf(productId), emptySet()))

        verify(exactly = 1) { cache.cacheOfferings(any(), any(), any()) }
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
            cache.cacheOfferings(any(), any(), any())
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
            cache.cacheOfferings(any(), any(), any())
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
            cache.cacheOfferings(any(), any(), any())
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
            cache.cacheOfferings(any(), any(), any())
        }
    }

    @Test
    fun `products are populated when getting offerings`() {
        mockOfferingsFactory()

        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any(), any())
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
            cache.cacheOfferings(any(), any(), any())
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
            cache.cacheOfferings(offerings, any(), any())
        }
    }

    @Test
    fun `get offerings error is called if behavior should fallback and no cached response`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any(), any())
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
    fun `get offerings success is called if behavior should fallback and cached response`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any(), any())
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

        verify(exactly = 1) { cache.cacheOfferings(testOfferings, null, any()) }
        verify(exactly = 1) {
            offeringsFactory.createOfferings(
                offeringsJSON = backendResponse,
                originalDataSource = null,
                loadedFromDiskCache = true,
                onError = any(),
                onSuccess = any(),
            )
        }
    }

    @Test
    fun `get offerings error is called if cached response but behavior should not fallback`() {
        every {
            cache.cachedOfferings
        } returns null
        every {
            cache.cacheOfferings(any(), any(), any())
        } just Runs

        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)
        mockBackendResponseError(
            error = expectedError,
            errorBehavior = GetOfferingsErrorHandlingBehavior.SHOULD_NOT_FALLBACK,
        )
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
            cache.cacheOfferings(any(), any(), any())
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
            cache.cacheOfferings(any(), any(), any())
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

    // A SyncDispatcher runs the block inline, so this holds the command instead: without the enqueue the
    // fan-out would decode on the backend response thread.
    @Test
    fun `getOfferings enqueues placement target warming rather than running it on the response thread`() {
        val commands = mutableListOf<Runnable>()
        val holdingDispatcher = object : Dispatcher(mockk(), MockHandlerFactory.createMockHandler()) {
            override fun enqueue(command: Runnable, delay: Delay) {
                commands += command
            }
        }
        val other = mockk<Offering>(relaxed = true).apply { every { identifier } returns "onboarding" }
        val offerings = testOfferings.copy(
            all = testOfferings.all + ("onboarding" to other),
            placements = Offerings.Placements(
                fallbackOfferingId = null,
                offeringIdsByPlacement = mapOf("onboarding" to "onboarding"),
            ),
        )
        val manager = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = holdingDispatcher,
            workflowManager = mockWorkflowManager,
        )
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory(offerings)
        mockDeviceCache()
        val warmed = mutableListOf<String>()
        every { offeringImagePreDownloader.preDownloadOfferingImages(any()) } answers {
            warmed += firstArg<Offering>().identifier
        }

        manager.getOfferings(appUserId, appInBackground = false, onError = { fail("should be a success") })

        assertThat(warmed).containsExactly(offerings.current!!.identifier)
        assertThat(commands).hasSize(1)

        commands.forEach { it.run() }

        assertThat(warmed).containsExactly(offerings.current!!.identifier, "onboarding")
    }

    // Each placement target decodes its own component tree, so the fan-out must stay off this path.
    @Test
    fun `getOfferings pre downloads placement target images only after delivering offerings`() {
        val other = mockk<Offering>(relaxed = true).apply { every { identifier } returns "onboarding" }
        val offerings = testOfferings.copy(
            all = testOfferings.all + ("onboarding" to other),
            placements = Offerings.Placements(
                fallbackOfferingId = null,
                offeringIdsByPlacement = mapOf("onboarding" to "onboarding"),
            ),
        )
        every { cache.cachedOfferings } returns null
        mockOfferingsFactory(offerings)
        mockDeviceCache()
        val warmed = mutableListOf<String>()
        every { offeringImagePreDownloader.preDownloadOfferingImages(any()) } answers {
            warmed += firstArg<Offering>().identifier
        }
        var warmedAtDelivery: List<String>? = null

        offeringsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be a success") },
            onSuccess = { warmedAtDelivery = warmed.toList() },
        )

        assertThat(warmedAtDelivery).containsExactly(offerings.current!!.identifier)
        assertThat(warmed).containsExactly(offerings.current!!.identifier, "onboarding")
    }

    // endregion pre download offering images

    // region prewarm web_view bundles

    @Test
    fun `getOfferings prewarms web_view bundles`() {
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
            mockOfferingWebViewPrewarmer.prewarmWebViews(testOfferings.prewarmTargetOfferings())
        }
    }

    // endregion prewarm web_view bundles

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
        mockBackendResponseError(
            error = expectedError,
            errorBehavior = GetOfferingsErrorHandlingBehavior.SHOULD_NOT_FALLBACK,
        )
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
        mockBackendResponseError(
            error = expectedError,
            errorBehavior = GetOfferingsErrorHandlingBehavior.SHOULD_NOT_FALLBACK,
        )
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

    // region UI Preview Mode

    @Test
    fun `getOfferings returns empty Offerings and does not call backend when uiPreviewMode is true`() {
        val previewOfferingsManager = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = SyncDispatcher(),
            uiPreviewMode = true,
            workflowManager = mockWorkflowManager,
        )

        var receivedOfferings: Offerings? = null
        previewOfferingsManager.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("Expected success but got error: $it") },
            onSuccess = { receivedOfferings = it },
        )

        assertThat(receivedOfferings).isNotNull
        val offerings = receivedOfferings!!
        assertThat(offerings.current).isNull()
        assertThat(offerings.all).isEmpty()
        verify(exactly = 0) {
            backend.getOfferings(any(), any(), any(), any())
        }
    }

    @Test
    fun `onAppForeground does not fetch when uiPreviewMode is true`() {
        val previewOfferingsManager = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = SyncDispatcher(),
            uiPreviewMode = true,
            workflowManager = mockWorkflowManager,
        )
        mockCacheStale(offeringsStale = true)
        mockDeviceCache()

        previewOfferingsManager.onAppForeground(appUserId)

        verify(exactly = 0) {
            backend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `fetchAndCacheOfferings does not call backend and invokes onSuccess with empty result when uiPreviewMode is true`() {
        val previewOfferingsManager = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = SyncDispatcher(),
            uiPreviewMode = true,
            workflowManager = mockWorkflowManager,
        )

        var receivedResult: OfferingsResultData? = null
        previewOfferingsManager.fetchAndCacheOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("Expected no error: $it") },
            onSuccess = { receivedResult = it },
        )

        verify(exactly = 0) {
            backend.getOfferings(any(), any(), any(), any())
        }
        assertThat(receivedResult).isNotNull
        val result = receivedResult!!
        assertThat(result.offerings.current).isNull()
        assertThat(result.offerings.all).isEmpty()
        assertThat(result.requestedProductIds).isEmpty()
        assertThat(result.notFoundProductIds).isEmpty()
    }

    // endregion UI Preview Mode

    @Test
    fun `purchase lookup returns cached offerings while ordinary lookup waits for config`() {
        every { cache.cachedOfferings } returns testOfferings
        mockCacheStale()
        val pendingReadiness = mutableListOf<() -> Unit>()
        every { mockWorkflowManager.onPaywallConfigReady(capture(pendingReadiness)) } just Runs
        var ordinaryCallbacks = 0
        var purchaseCallbacks = 0
        offeringsManager.getOfferings(appUserId, false, onSuccess = { ordinaryCallbacks++ })

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference(STUB_OFFERING_IDENTIFIER, "\$rc_monthly"),
            emptyList(),
            onError = { fail("Expected cached offerings: $it") },
            onSuccess = { main, addOns ->
                assertThat(main).isSameAs(testOfferings.current!!.availablePackages.first())
                assertThat(addOns).isEmpty()
                purchaseCallbacks++
            },
        )

        assertThat(purchaseCallbacks).isEqualTo(1)
        assertThat(ordinaryCallbacks).isZero()
        pendingReadiness.forEach { it() }
        assertThat(ordinaryCallbacks).isEqualTo(1)
        assertThat(purchaseCallbacks).isEqualTo(1)
    }

    @Test
    fun `purchase lookup keeps cold cache readiness behavior`() {
        every { cache.cachedOfferings } returns null
        mockDeviceCache()
        mockOfferingsFactory()
        val pendingReadiness = slot<() -> Unit>()
        every { mockWorkflowManager.onPaywallConfigReady(capture(pendingReadiness)) } just Runs
        var callbacks = 0

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference(STUB_OFFERING_IDENTIFIER, "\$rc_monthly"),
            emptyList(),
            onError = { fail("Expected fetched offerings: $it") },
            onSuccess = { main, addOns ->
                assertThat(main).isSameAs(testOfferings.current!!.availablePackages.first())
                assertThat(addOns).isEmpty()
                callbacks++
            },
        )

        assertThat(callbacks).isZero()
        pendingReadiness.captured()
        assertThat(callbacks).isEqualTo(1)
    }

    @Test
    fun `purchase lookup propagates cold cache fetch failure`() {
        every { cache.cachedOfferings } returns null
        every { cache.cachedOfferingsResponse } returns null
        mockDeviceCache(wasSuccessful = false)
        val expected = PurchasesError(PurchasesErrorCode.NetworkError, "lookup failed")
        mockBackendResponseError(expected)
        var receivedError: PurchasesError? = null

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference(STUB_OFFERING_IDENTIFIER, "\$rc_monthly"),
            emptyList(),
            onError = { receivedError = it },
            onSuccess = { _, _ -> fail("Expected fetch error") },
        )

        assertThat(receivedError).isSameAs(expected)
    }

    @Test
    fun `purchase lookup still refreshes stale offerings without replaying callback`() {
        every { cache.cachedOfferings } returns testOfferings
        mockCacheStale(offeringsStale = true)
        mockDeviceCache()
        mockOfferingsFactory()
        val pendingReadiness = mutableListOf<() -> Unit>()
        every { mockWorkflowManager.onPaywallConfigReady(capture(pendingReadiness)) } just Runs
        var callbacks = 0

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference(STUB_OFFERING_IDENTIFIER, "\$rc_monthly"),
            emptyList(),
            onError = { fail("Expected cached offerings: $it") },
            onSuccess = { _, _ -> callbacks++ },
        )

        assertThat(callbacks).isEqualTo(1)
        verify(exactly = 1) { backend.getOfferings(appUserId, false, any(), any()) }
        pendingReadiness.forEach { it() }
        assertThat(callbacks).isEqualTo(1)
    }

    @Test
    fun `package batch resolves main and ordered duplicate add ons from one cached snapshot`() {
        val main = purchasePackage("main-offering", "monthly", "main-product")
        val sameIdentifierElsewhere = purchasePackage("other-offering", "monthly", "other-product")
        val addOn = purchasePackage("main-offering", "Extra", "extra-product")
        val snapshot = purchaseOfferings(sameIdentifierElsewhere, main, addOn)
        every { cache.cachedOfferings } returnsMany listOf(snapshot, testOfferings)
        mockCacheStale()
        val pending = mutableListOf<() -> Unit>()
        every { mockWorkflowManager.onPaywallConfigReady(capture(pending)) } just Runs
        var callbacks = 0
        var receivedMain: Package? = null
        var receivedAddOns: List<Package>? = null

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference("main-offering", "MONTHLY"),
            listOf(
                PackagePurchaseReference("other-offering", "monthly"),
                PackagePurchaseReference("main-offering", "Extra"),
                PackagePurchaseReference("other-offering", "monthly"),
            ),
            onError = { fail("Expected resolved packages: $it") },
            onSuccess = { base, addOns ->
                receivedMain = base
                receivedAddOns = addOns
                callbacks++
            },
        )

        assertThat(receivedMain).isSameAs(main)
        assertThat(receivedAddOns).containsExactly(sameIdentifierElsewhere, addOn, sameIdentifierElsewhere)
        assertThat(receivedAddOns!![0]).isSameAs(sameIdentifierElsewhere)
        assertThat(callbacks).isEqualTo(1)
        assertThat(pending).isEmpty()
        verify(exactly = 1) { cache.cachedOfferings }
        verify(exactly = 0) { backend.getOfferings(any(), any(), any(), any()) }
    }

    @Test
    fun `package batch does not return partial result when later add on is missing`() {
        val main = purchasePackage("offering", "monthly", "main-product")
        val addOn = purchasePackage("offering", "Extra", "extra-product")
        every { cache.cachedOfferings } returns purchaseOfferings(main, addOn)
        mockCacheStale()
        var received: PurchasesError? = null
        var successCalls = 0

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference("offering", "monthly"),
            listOf(
                PackagePurchaseReference("offering", "Extra"),
                PackagePurchaseReference("offering", "missing"),
            ),
            onError = { received = it },
            onSuccess = { _, _ -> successCalls++ },
        )

        assertThat(successCalls).isZero()
        assertThat(received?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(received?.underlyingErrorMessage)
            .isEqualTo("Could not find package with identifier missing in offering with identifier offering")
    }

    @Test
    fun `package batch preserves exact matching for add ons`() {
        val main = purchasePackage("offering", "monthly", "main-product")
        val addOn = purchasePackage("offering", "Extra", "extra-product")
        every { cache.cachedOfferings } returns purchaseOfferings(main, addOn)
        mockCacheStale()
        var received: PurchasesError? = null

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference("offering", "MONTHLY"),
            listOf(PackagePurchaseReference("offering", "extra")),
            onError = { received = it },
            onSuccess = { _, _ -> fail("Case-mismatched add on must not resolve") },
        )

        assertThat(received?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
    }

    @Test
    fun `package batch reports missing add on offering`() {
        val main = purchasePackage("offering", "monthly", "main-product")
        every { cache.cachedOfferings } returns purchaseOfferings(main)
        mockCacheStale()
        var received: PurchasesError? = null

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference("offering", "monthly"),
            listOf(PackagePurchaseReference("missing-offering", "Extra")),
            onError = { received = it },
            onSuccess = { _, _ -> fail("Missing offering must fail the complete lookup") },
        )

        assertThat(received?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(received?.underlyingErrorMessage)
            .isEqualTo("Could not find offering with identifier missing-offering for add-on package Extra")
    }

    @Test
    fun `package batch distinguishes missing main package from fetch failure`() {
        val main = purchasePackage("offering", "monthly", "main-product")
        every { cache.cachedOfferings } returns purchaseOfferings(main)
        mockCacheStale()
        var received: PurchasesError? = null

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference("offering", "missing"),
            emptyList(),
            onError = { received = it },
            onSuccess = { _, _ -> fail("Missing main package must fail") },
        )

        assertThat(received?.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
        assertThat(received?.underlyingErrorMessage).isEqualTo("Couldn't find product for package missing")
    }

    @Test
    fun `cold package batch resolves all packages from fetched snapshot after readiness`() {
        val main = purchasePackage("main-offering", "monthly", "main-product")
        val addOn = purchasePackage("other-offering", "Extra", "extra-product")
        every { cache.cachedOfferings } returns null
        mockDeviceCache()
        mockOfferingsFactory(purchaseOfferings(main, addOn))
        val pending = slot<() -> Unit>()
        every { mockWorkflowManager.onPaywallConfigReady(capture(pending)) } just Runs
        var callbacks = 0

        offeringsManager.getPackagesForPurchase(
            appUserId, false,
            PackagePurchaseReference("main-offering", "MONTHLY"),
            listOf(PackagePurchaseReference("other-offering", "Extra")),
            onError = { fail("Expected fetched packages: $it") },
            onSuccess = { base, addOns ->
                assertThat(base).isSameAs(main)
                assertThat(addOns).containsExactly(addOn)
                callbacks++
            },
        )

        assertThat(callbacks).isZero()
        pending.captured()
        assertThat(callbacks).isEqualTo(1)
        verify(exactly = 1) { backend.getOfferings(any(), any(), any(), any()) }
    }

    private fun purchasePackage(offeringId: String, packageId: String, productId: String) = Package(
        identifier = packageId,
        packageType = PackageType.CUSTOM,
        product = stubStoreProduct(productId),
        presentedOfferingContext = PresentedOfferingContext(offeringId),
    )

    private fun purchaseOfferings(vararg packages: Package): Offerings {
        val all = packages.groupBy { it.presentedOfferingContext.offeringIdentifier }.mapValues { (id, items) ->
            Offering(id, "Description", emptyMap(), items)
        }
        return Offerings(all.values.first(), all)
    }

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
                    originalDataSource = any(),
                    loadedFromDiskCache = any(),
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
                    originalDataSource = any(),
                    loadedFromDiskCache = any(),
                    onError = captureLambda(),
                    onSuccess = any(),
                )
            } answers {
                lambda<(PurchasesError) -> Unit>().captured.invoke(error)
            }
        }
    }

    /**
     * Captures the offerings factory `onSuccess` lambda without invoking it, so a test can drive the parse
     * completion manually (simulating a fetch still in flight while the cache is cleared).
     */
    private fun captureFactoryOnSuccess(): CapturingSlot<(OfferingsResultData) -> Unit> {
        val onSuccessSlot = slot<(OfferingsResultData) -> Unit>()
        every {
            offeringsFactory.createOfferings(
                offeringsJSON = any(),
                originalDataSource = any(),
                loadedFromDiskCache = any(),
                onError = any(),
                onSuccess = capture(onSuccessSlot),
            )
        } just Runs
        return onSuccessSlot
    }

    private fun mockBackendResponseSuccess(response: String = ONE_OFFERINGS_RESPONSE) {
        every {
            backend.getOfferings(any(), any(), captureLambda(), any())
        } answers {
            lambda<(JSONObject, HTTPResponseOriginalSource, String) -> Unit>().captured.invoke(
                JSONObject(response),
                HTTPResponseOriginalSource.MAIN,
                response,
            )
        }
    }

    private fun mockBackendResponseError(
        error: PurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError),
        errorBehavior: GetOfferingsErrorHandlingBehavior = GetOfferingsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_OFFERINGS,
    ) {
        every {
            backend.getOfferings(any(), any(), any(), captureLambda())
        } answers {
            lambda<(PurchasesError, GetOfferingsErrorHandlingBehavior) -> Unit>().captured.invoke(error, errorBehavior)
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
            every { cache.cacheOfferings(any(), any(), any()) } just Runs
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

    // region workflowManager onComplete integration

    @Test
    fun `getOfferings does not call onSuccess until onPaywallConfigReady completes`() {
        val mockWorkflowManager = mockk<WorkflowManager>()
        val onCompleteSlot = slot<() -> Unit>()
        every {
            mockWorkflowManager.onPaywallConfigReady(onComplete = capture(onCompleteSlot))
        } just Runs

        val managerWithWorkflow = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = SyncDispatcher(),
            workflowManager = mockWorkflowManager,
        )

        every { cache.cachedOfferings } returns null
        mockOfferingsFactory()
        mockDeviceCache()

        var receivedOfferings: Offerings? = null
        managerWithWorkflow.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be success") },
            onSuccess = { receivedOfferings = it },
        )

        assertThat(receivedOfferings).isNull()

        onCompleteSlot.captured.invoke()

        assertThat(receivedOfferings).isNotNull()
    }

    @Test
    fun `vendCachedOfferingsAndMaybeRefresh does not call onSuccess until onPaywallConfigReady completes`() {
        val mockWorkflowManager = mockk<WorkflowManager>()
        val onCompleteSlot = slot<() -> Unit>()
        every {
            mockWorkflowManager.onPaywallConfigReady(onComplete = capture(onCompleteSlot))
        } just Runs

        val managerWithWorkflow = OfferingsManager(
            offeringsCache = cache,
            backend = backend,
            offeringsFactory = offeringsFactory,
            offeringImagePreDownloader = offeringImagePreDownloader,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            offeringFontPreDownloader = mockOfferingFontPreDownloader,
            offeringWebViewPrewarmer = mockOfferingWebViewPrewarmer,
            dispatcher = SyncDispatcher(),
            workflowManager = mockWorkflowManager,
        )

        mockCacheStale(offeringsStale = false)
        every { cache.cachedOfferings } returns testOfferings

        var receivedOfferings: Offerings? = null
        managerWithWorkflow.getOfferings(
            appUserId,
            appInBackground = false,
            onError = { fail("should be success") },
            onSuccess = { receivedOfferings = it },
        )

        assertThat(receivedOfferings).isNull()

        onCompleteSlot.captured.invoke()

        assertThat(receivedOfferings).isEqualTo(testOfferings)
    }

    // endregion workflowManager onComplete integration
}
