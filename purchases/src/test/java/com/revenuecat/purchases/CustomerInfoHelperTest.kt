package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerInfoHelperTest {

    private val mockCache = mockk<DeviceCache>()
    private val mockBackend = mockk<Backend>()
    private val mockIdentityManager = mockk<IdentityManager>()
    private val mockHandler = mockk<Handler>()
    private val mockLooper = mockk<Looper>()
    private val mockThread = mockk<Thread>()

    private val mockInfo = mockk<CustomerInfo>()

    private val appUserId = "fakeUserId"

    private lateinit var customerInfoHelper: CustomerInfoHelper

    @Before
    fun setup() {
        setupCacheMock()
        setupHandlerMock()
        setupIdentityManagerMock()

        customerInfoHelper = CustomerInfoHelper(
            mockCache,
            mockBackend,
            mockIdentityManager,
            mockHandler
        )
    }

    @After
    fun tearDown() {
        clearMocks(mockCache, mockBackend, mockIdentityManager, mockHandler, mockLooper, mockThread, mockInfo)
    }

    // region updatedCustomerInfoListener

    @Test
    fun `setting listener sends cached value if it exists`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) }
    }

    @Test
    fun `setting listener does not send cached value if it does not exists`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        every { mockCache.getCachedCustomerInfo(any()) } returns null
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        verify(exactly = 0) { listenerMock.onReceived(mockInfo) }
    }

    // endregion

    // region cacheCustomerInfo

    @Test
    fun `caching customer info calls device cache with correct parameters`() {
        customerInfoHelper.cacheCustomerInfo(mockInfo)

        verify(exactly = 1) { mockCache.cacheCustomerInfo(appUserId, mockInfo) }
    }

    // endregion

    // region sendUpdatedCustomerInfoToDelegateIfChanged

    @Test
    fun `does nothing if listener is null`() {
        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(mockInfo)
        assertNull(customerInfoHelper.updatedCustomerInfoListener)
    }

    @Test
    fun `does not update listener if customer info same as previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(mockInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

    @Test
    fun `updates listener if customer info different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(newCustomerInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    @Test
    fun `does not update listener if customer info same one in several calls`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(newCustomerInfo)
        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(newCustomerInfo)
        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(newCustomerInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    // endregion

    // region retrieveCustomerInfo tests

    // region CACHE_ONLY fetch policy

    @Test
    fun `retrieving customer info from CACHE_ONLY does nothing if callback is null`() {
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false)
        verify(exactly = 0) { mockCache.getCachedCustomerInfo(any()) }
    }

    @Test
    fun `retrieving customer info from CACHE_ONLY gets info from cache`() {
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false, callbackMock)
        verify(exactly = 1) { mockCache.getCachedCustomerInfo(any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
        verify(exactly = 0) { mockCache.cacheCustomerInfo(any(), any()) }
    }

    @Test
    fun `retrieving customer info from CACHE_ONLY fails if cant be found in cache`() {
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false, callbackMock)
        verify(exactly = 1) { callbackMock.onError(any()) }
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `retrieving customer info from CACHE_ONLY does not update listener`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        customerInfoHelper.updatedCustomerInfoListener = listenerMock
        every { mockCache.getCachedCustomerInfo(appUserId) } returns mockInfo

        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHE_ONLY, false, callbackMock)
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { listenerMock.onReceived(any()) }
    }

    // endregion

    // region FETCH_CURRENT fetch policy

    @Test
    fun `retrieving customer info with FETCH_CURRENT sets cache timestamp to now`() {
        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false)
        verify(exactly = 1) {
            mockCache.setCustomerInfoCacheTimestampToNow(appUserId)
        }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT caches customer info if successful`() {
        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false)
        verify(exactly = 1) { mockCache.cacheCustomerInfo(appUserId, mockInfo) }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT updates listener if successful and different value`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { mockCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just runs
        setupBackendMock(customerInfo = newCustomerInfo)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false)
        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // Called while setting up the listener.
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT does not update listener if successful and same value`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock
        every { mockCache.getCachedCustomerInfo(appUserId) } returns mockInfo

        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false)
        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // Called while setting up the listener.
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT calls success callback if successful`() {
        setupBackendMock()
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, callbackMock)
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { callbackMock.onError(any()) }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT does not cache customer info if error`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, callbackMock)
        verify(exactly = 1) { callbackMock.onError(error) }
        verify(exactly = 0) { mockCache.cacheCustomerInfo(any(), any()) }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT does not call listener if error`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock
        every { mockCache.getCachedCustomerInfo(appUserId) } returns mockInfo

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, callbackMock)
        verify(exactly = 1) { callbackMock.onError(error) }
        verify(exactly = 1) { listenerMock.onReceived(any()) } // Called while setting up the listener.
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT calls error callback if error`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, callbackMock)
        verify(exactly = 0) { callbackMock.onReceived(any()) }
        verify(exactly = 1) { callbackMock.onError(error) }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT error clears cache timestamp`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false)
        verify(exactly = 1) { mockCache.clearCustomerInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `retrieving customer info with FETCH_CURRENT success does not clear cache timestamp`() {
        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false)
        verify(exactly = 0) { mockCache.clearCustomerInfoCacheTimestamp(any()) }
    }

    // endregion

    // region CACHED_OR_FETCHED fetch policy

    @Test
    fun `retrieving customer info with CACHED_OR_FETCHED gets info from cache if exists`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns false
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.CACHED_OR_FETCHED,
            false,
            callbackMock
        )
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { mockCache.cacheCustomerInfo(any(), any()) }
    }

    @Test
    fun `retrieving customer info with CACHED_OR_FETCHED initiates fetch of info if cache stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        val newCustomerInfo = mockk<CustomerInfo>()
        setupBackendMock(null, newCustomerInfo)
        every { mockCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just runs
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.CACHED_OR_FETCHED,
            false,
            callbackMock
        )
        verify(exactly = 1) { mockBackend.getCustomerInfo(appUserId, false, any(), any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 1) { mockCache.setCustomerInfoCacheTimestampToNow(appUserId) }
        verify(exactly = 1) { mockCache.cacheCustomerInfo(appUserId, newCustomerInfo) }
    }

    @Test
    fun `retrieving customer info with CACHED_OR_FETCHED updates listener if fetch successful and different value`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { mockCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just runs
        setupBackendMock(customerInfo = newCustomerInfo)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false)
        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // Called while setting up the listener.
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    @Test
    fun `retrieving info with CACHED_OR_FETCHED and no cache sets cache timestamp to now`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false)
        verify(exactly = 1) {
            mockCache.setCustomerInfoCacheTimestampToNow(appUserId)
        }
    }

    @Test
    fun `retrieving info with CACHED_OR_FETCHED and no cache calls success callback if successful`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, callbackMock)
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { callbackMock.onError(any()) }
    }

    @Test
    fun `retrieving info with CACHED_OR_FETCHED and no cache calls error callback if error`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false, callbackMock)
        verify(exactly = 0) { callbackMock.onReceived(any()) }
        verify(exactly = 1) { callbackMock.onError(error) }
    }

    @Test
    fun `retrieving info with CACHED_OR_FETCHED and no cache and error backend request clears cache`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false)
        verify(exactly = 1) { mockCache.clearCustomerInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `retrieving info with CACHED_OR_FETCHED and no cache fetch success does not clear cache timestamp`() {
        every { mockCache.getCachedCustomerInfo(appUserId) } returns null
        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.CACHED_OR_FETCHED, false)
        verify(exactly = 0) { mockCache.clearCustomerInfoCacheTimestamp(any()) }
    }

    // endregion

    // region NOT_STALE_CACHED_OR_CURRENT fetch policy

    @Test
    fun `retrieving info with NOT_STALE_CACHED_OR_CURRENT does not use cache if stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        setupBackendMock()
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT, false)
        verify(exactly = 0) { mockCache.getCachedCustomerInfo(any()) }
    }

    @Test
    fun `retrieving info with NOT_STALE_CACHED_OR_CURRENT does not use cache if stale even if error fetching`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        setupBackendMock(error)
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            false,
            callbackMock
        )
        verify(exactly = 0) { mockCache.getCachedCustomerInfo(any()) }
        verify(exactly = 1) { callbackMock.onError(error) }
        verify(exactly = 1) { mockCache.clearCustomerInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `retrieving info with NOT_STALE_CACHED_OR_CURRENT fetches from backend if stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        setupBackendMock()
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            false,
            callbackMock
        )
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 1) { mockCache.setCustomerInfoCacheTimestampToNow(appUserId) }
    }

    @Test
    fun `retrieving info with NOT_STALE_CACHED_OR_CURRENT uses cache if not stale`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns false
        val callbackMock = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        customerInfoHelper.retrieveCustomerInfo(
            appUserId,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            false,
            callbackMock
        )
        verify(exactly = 1) { mockCache.getCachedCustomerInfo(any()) }
        verify(exactly = 1) { callbackMock.onReceived(mockInfo) }
        verify(exactly = 0) { mockBackend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `retrieving customer info with NOT_STALE_CACHED_OR_CURRENT updates listener if fetch success and other value`() {
        every { mockCache.isCustomerInfoCacheStale(appUserId, false) } returns true
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoHelper.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { mockCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just runs
        setupBackendMock(customerInfo = newCustomerInfo)
        customerInfoHelper.retrieveCustomerInfo(appUserId, CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT, false)
        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // Called while setting up the listener.
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
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
        every { mockCache.cacheCustomerInfo(appUserId, mockInfo) } just runs
    }

    private fun setupIdentityManagerMock() {
        every { mockIdentityManager.currentAppUserID } returns appUserId
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
