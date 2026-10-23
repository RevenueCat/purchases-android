package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.GetOfferingsErrorHandlingBehavior
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.PayloadFileStore
import com.revenuecat.purchases.common.workflows.WorkflowManager
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A late offerings response for the previous user mustn't overwrite the cache wiped by an identity change. */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsManagerEpochTest {

    private val offerings = stubOfferings("product").second
    private val deviceCache = mockk<DeviceCache>().apply {
        every { removeLegacyOfferingsResponse() } just Runs
        every { offeringsResponseFileKey } returns "offerings_response_key"
        every { getOfferingsResponseChecksum() } returns null
        every { cacheOfferingsResponseChecksum(any()) } just Runs
        every { clearOfferingsResponseChecksum() } just Runs
    }
    private val responseStore = mockk<PayloadFileStore>(relaxed = true)
    private val cache = OfferingsCache(
        deviceCache,
        responseStore = responseStore,
        dispatcher = SyncDispatcher(),
        localeProvider = DefaultLocaleProvider(),
    )
    private val backend = mockk<Backend>()
    private val offeringsFactory = mockk<OfferingsFactory>().apply {
        every { createOfferings(any(), any(), any(), any(), any()) } answers {
            arg<(OfferingsResultData) -> Unit>(4)(OfferingsResultData(offerings, emptySet(), emptySet()))
        }
    }
    private val workflowManager = mockk<WorkflowManager>(relaxed = true).apply {
        every { onPaywallConfigReady(onComplete = any()) } answers { firstArg<() -> Unit>().invoke() }
    }
    private val manager = OfferingsManager(
        offeringsCache = cache,
        backend = backend,
        offeringsFactory = offeringsFactory,
        offeringImagePreDownloader = mockk(relaxed = true),
        diagnosticsTrackerIfEnabled = null,
        offeringFontPreDownloader = mockk(relaxed = true),
        offeringWebViewPrewarmer = mockk(relaxed = true),
        dispatcher = SyncDispatcher(),
        workflowManager = workflowManager,
    )

    private var respond: (() -> Unit)? = null
    private var respondWithError: (() -> Unit)? = null

    init {
        every { backend.getOfferings(any(), any(), any(), any()) } answers {
            val onSuccess = thirdArg<(JSONObject, HTTPResponseOriginalSource, String) -> Unit>()
            val onError = arg<(PurchasesError, GetOfferingsErrorHandlingBehavior) -> Unit>(3)
            respond = { onSuccess(JSONObject(ONE_OFFERINGS_RESPONSE), HTTPResponseOriginalSource.MAIN, "payload") }
            respondWithError = {
                onError(
                    PurchasesError(PurchasesErrorCode.NetworkError),
                    GetOfferingsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_OFFERINGS,
                )
            }
        }
    }

    @Test
    fun `a response for the current user is cached`() {
        manager.fetchAndCacheOfferings("old-user", appInBackground = false)
        respond!!()

        assertThat(cache.cachedOfferings).isEqualTo(offerings)
        verify { responseStore.write(any(), "payload") }
    }

    @Test
    fun `a response arriving after an identity change is not cached`() {
        manager.fetchAndCacheOfferings("old-user", appInBackground = false)
        cache.clearCache()
        respond!!()

        assertThat(cache.cachedOfferings).isNull()
        verify(exactly = 0) { responseStore.write(any(), any()) }
    }

    @Test
    fun `a late response still reaches the caller that asked for it`() {
        var received: Offerings? = null
        manager.fetchAndCacheOfferings("old-user", appInBackground = false, onSuccess = { received = it.offerings })
        cache.clearCache()
        respond!!()

        assertThat(received).isEqualTo(offerings)
    }

    @Test
    fun `a late disk fallback after an identity change is not cached`() {
        manager.fetchAndCacheOfferings("old-user", appInBackground = false)
        cache.clearCache()
        every { deviceCache.getOfferingsResponseChecksum() } returns 1L
        every { responseStore.read(any(), 1L) } returns ONE_OFFERINGS_RESPONSE
        respondWithError!!()

        assertThat(cache.cachedOfferings).isNull()
    }

    @Test
    fun `a fetch started after the identity change is cached`() {
        manager.fetchAndCacheOfferings("old-user", appInBackground = false)
        val oldResponse = respond!!
        cache.clearCache()
        manager.fetchAndCacheOfferings("new-user", appInBackground = false)
        val newResponse = respond!!

        oldResponse()
        assertThat(cache.cachedOfferings).isNull()
        newResponse()
        assertThat(cache.cachedOfferings).isEqualTo(offerings)
    }
}
