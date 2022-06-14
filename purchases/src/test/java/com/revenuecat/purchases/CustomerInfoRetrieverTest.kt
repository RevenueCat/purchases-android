package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerInfoRetrieverTest {

    private val mockCache = mockk<DeviceCache>()
    private val mockBackend = mockk<Backend>()
    private val mockHandler = mockk<Handler>()
    private val mockLooper = mockk<Looper>()
    private val mockThread = mockk<Thread>()

    private val mockInfo = mockk<CustomerInfo>()

    private val appUserId = "fakeUserId"

    private lateinit var retriever: CustomerInfoRetriever

    @Before
    fun setup() {
        clearMocks(mockCache, mockBackend, mockHandler, mockLooper, mockThread, mockInfo)

        setupCacheMock()
        setupHandlerMock()

        retriever = CustomerInfoRetriever(
            mockCache,
            mockBackend,
            mockHandler
        )
    }

    // region retrieveCustomerInfo tests

    // region CACHE_ONLY fetch policy

    @Test
    fun `retrieving customer info from cache only does nothing if callback is null`() {
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false, {})
        verify(exactly = 0) { mockCache.getCachedCustomerInfo(any()) }
    }

    @Test
    fun `retrieving customer info from cache only gets info from cache`() {
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false, {}, callbackMock)
        verify(exactly = 1) { mockCache.getCachedCustomerInfo(any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `retrieving customer info from cache only fails if cant be found in cache`() {
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false, {}, callbackMock)
        verify(exactly = 1) { callbackMock.onError(any()) }
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `retrieving customer info from cache only does not call cache mock after fetch callback`() {
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        var returnedInfo: CustomerInfo? = null
        retriever.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.CACHE_ONLY,
            false,
            { returnedInfo = it },
            callbackMock
        )
        assertNull(returnedInfo)
    }

    // endregion

    // region FETCH_CURRENT fetch policy

    @Test
    fun `retrieving customer info with fetch current policy sets cache timestamp to now`() {
        setupBackendMock()
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, {})
        verify(exactly = 1) {
            mockCache.setCustomerInfoCacheTimestampToNow(appUserId)
        }
    }

    @Test
    fun `retrieving customer info with fetch current policy executes cache customer info callback if successful`() {
        setupBackendMock()
        var receivedInfo: CustomerInfo? = null
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, { receivedInfo = it })
        assertEquals(mockInfo, receivedInfo)
    }

    @Test
    fun `retrieving customer info with fetch current policy does not execute cache customer info callback if error`() {
        setupBackendMock(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))
        var receivedInfo: CustomerInfo? = null
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, { receivedInfo = it })
        assertNull(receivedInfo)
    }

    @Test
    fun `retrieving customer info with fetch current policy calls success callback if successful`() {
        setupBackendMock()
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, {}, callbackMock)
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { callbackMock.onError(any()) }
    }

    @Test
    fun `retrieving customer info with fetch current policy calls error callback if error`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, {}, callbackMock)
        verify(exactly = 0) { callbackMock.onReceived(any()) }
        verify(exactly = 1) { callbackMock.onError(error) }
    }

    @Test
    fun `retrieving customer info with fetch current policy error clears cache timestamp`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, {})
        verify(exactly = 1) { mockCache.clearCustomerInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `retrieving customer info with fetch current policy success does not clear cache timestamp`() {
        setupBackendMock()
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, {})
        verify(exactly = 0) { mockCache.clearCustomerInfoCacheTimestamp(any()) }
    }

    // endregion

    // region CACHED_OR_FETCHED fetch policy

    @Test
    fun `retrieving customer info with cached or fetch policy gets cache from cache if exists`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns false
        var returnedInfo: CustomerInfo? = null
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.CACHED_OR_FETCHED,
            false,
            { returnedInfo = it },
            callbackMock
        )
        assertNull(returnedInfo)
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
    }

    @Test
    fun `retrieving customer info with cached or fetch policy initiates fetch of info if cache stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        val newMockInfo = mockk<CustomerInfo>()
        setupBackendMock(null, newMockInfo)
        var returnedInfo: CustomerInfo? = null
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.CACHED_OR_FETCHED,
            false,
            { returnedInfo = it },
            callbackMock
        )
        verify(exactly = 1) { mockBackend.getCustomerInfo(appUserId, false, any(), any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 1) { mockCache.setCustomerInfoCacheTimestampToNow(appUserId) }
        assertEquals(newMockInfo, returnedInfo)
    }

    @Test
    fun `retrieving info with cache or fetch policy and no cache sets cache timestamp to now`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, {})
        verify(exactly = 1) {
            mockCache.setCustomerInfoCacheTimestampToNow(appUserId)
        }
    }

    @Test
    fun `retrieving info with cache or fetch policy and no cache executes cache info callback if successful`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        var receivedInfo: CustomerInfo? = null
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, { receivedInfo = it })
        assertEquals(mockInfo, receivedInfo)
    }

    @Test
    fun `retrieving info with cache or fetch policy and no cache does not execute cache info callback if error`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))
        var receivedInfo: CustomerInfo? = null
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, { receivedInfo = it })
        assertNull(receivedInfo)
    }

    @Test
    fun `retrieving info with cache or fetch policy and no cache calls success callback if successful`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, {}, callbackMock)
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { callbackMock.onError(any()) }
    }

    @Test
    fun `retrieving info with cache or fetch policy and no cache calls error callback if error`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, {}, callbackMock)
        verify(exactly = 0) { callbackMock.onReceived(any()) }
        verify(exactly = 1) { callbackMock.onError(error) }
    }

    @Test
    fun `retrieving info with cache or fetch policy and no cache error clears cache`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, {})
        verify(exactly = 1) { mockCache.clearCustomerInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `retrieving info with cache or fetch policy and no data success does not clear cache`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, {})
        verify(exactly = 0) { mockCache.clearCustomerInfoCacheTimestamp(any()) }
    }

    // endregion

    // region NOT_STALE_CACHED_OR_CURRENT fetch policy

    @Test
    fun `retrieving info with not staled cached or fetch policy does not use cache if stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        setupBackendMock()
        retriever.retrieveCustomerInfo(appUserId, CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT, false, {})
        verify(exactly = 0) { mockCache.getCachedCustomerInfo(any()) }
    }

    @Test
    fun `retrieving info with not staled cached or fetch policy does not use cache if stale even if error fetching`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            false,
            {},
            callbackMock
        )
        verify(exactly = 0) { mockCache.getCachedCustomerInfo(any()) }
        verify(exactly = 1) { callbackMock.onError(error) }
        verify(exactly = 1) { mockCache.clearCustomerInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `retrieving info with not staled cached or fetch policy fetches from backend if stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        setupBackendMock()
        var returnedInfo: CustomerInfo? = null
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        retriever.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            false,
            { returnedInfo = it },
            callbackMock
        )
        assertEquals(mockInfo, returnedInfo)
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 1) { mockCache.setCustomerInfoCacheTimestampToNow(appUserId) }
    }

    @Test
    fun `retrieving info with not staled cached or fetch policy uses cache if not stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns false
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        var returnedInfo: CustomerInfo? = null
        retriever.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            false,
            { returnedInfo = it },
            callbackMock
        )
        assertNull(returnedInfo)
        verify(exactly = 1) { mockCache.getCachedCustomerInfo(any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
    }

    // endregion

    // endregion

    private fun setupBackendMock(
        errorGettingCustomerInfo: PurchasesError? = null,
        customerInfo: CustomerInfo = mockInfo
    ) {
        with(mockBackend) {
            if (errorGettingCustomerInfo != null) {
                every {
                    getCustomerInfo(any(), any(), any(), captureLambda())
                } answers {
                    lambda<(PurchasesError) -> Unit>().captured.invoke(errorGettingCustomerInfo)
                }
            } else {
                every {
                    getCustomerInfo(any(), any(), captureLambda(), any())
                } answers {
                    lambda<(CustomerInfo) -> Unit>().captured.invoke(customerInfo)
                }
            }
        }
    }

    private fun setupCacheMock() {
        every { mockCache.setCustomerInfoCacheTimestampToNow(appUserId) } just runs
        every { mockCache.clearCustomerInfoCacheTimestamp(appUserId) } just runs
        every { mockCache.getCachedCustomerInfo(appUserId) } returns mockInfo
    }

    private fun setupHandlerMock() {
        every { mockHandler.post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }
        every { mockHandler.looper } returns mockLooper
        every { mockLooper.thread } returns mockThread
    }
}
