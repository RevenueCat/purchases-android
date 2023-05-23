package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfflineEntitlementsManagerTest {

    private val appUserID = "test-app-user-id"

    private lateinit var backendSuccessSlot: CapturingSlot<(ProductEntitlementMapping) -> Unit>
    private lateinit var backendErrorSlot: CapturingSlot<(PurchasesError) -> Unit>

    private lateinit var backend: Backend
    private lateinit var deviceCache: DeviceCache
    private lateinit var offlineEntitlementsCalculator: OfflineCustomerInfoCalculator
    private lateinit var appConfig: AppConfig

    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager

    @Before
    fun setUp() {
        backendSuccessSlot = slot()
        backendErrorSlot = slot()

        backend = mockk()
        deviceCache = mockk()
        offlineEntitlementsCalculator = mockk()
        appConfig = mockk()

        every {
            deviceCache.getCachedAppUserID()
        } returns appUserID
        every {
            deviceCache.clearCustomerInfoCache(appUserID)
        } just Runs
        every {
            backend.getProductEntitlementMapping(capture(backendSuccessSlot), capture(backendErrorSlot))
        } just Runs
        every {
            appConfig.finishTransactions
        } returns true

        offlineEntitlementsManager = OfflineEntitlementsManager(
            backend,
            offlineEntitlementsCalculator,
            deviceCache,
            appConfig
        )
    }

    // region shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest

    @Test
    fun `shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest returns true if server error and cached customer info is null`() {
        every { deviceCache.getCachedCustomerInfo(appUserID) } returns null
        val isServerError = true
        val result = offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
            isServerError,
            appUserID
        )
        assertThat(result).isTrue
    }

    @Test
    fun `shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest returns false if not finishing transactions`() {
        every { appConfig.finishTransactions } returns false
        every { deviceCache.getCachedCustomerInfo(appUserID) } returns null
        val isServerError = true
        val result = offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
            isServerError,
            appUserID
        )
        assertThat(result).isFalse
    }

    @Test
    fun `shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest returns false if server error and cached customer info is not null`() {
        every { deviceCache.getCachedCustomerInfo(appUserID) } returns mockk()
        val isServerError = true
        val result = offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
            isServerError,
            appUserID
        )
        assertThat(result).isFalse
    }

    @Test
    fun `shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest returns false if not server error and cached customer info is null`() {
        every { deviceCache.getCachedCustomerInfo(appUserID) } returns null
        val isServerError = false
        val result = offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
            isServerError,
            appUserID
        )
        assertThat(result).isFalse
    }

    @Test
    fun `shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest returns false if not server error and cached customer info is not null`() {
        every { deviceCache.getCachedCustomerInfo(appUserID) } returns mockk()
        val isServerError = false
        val result = offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
            isServerError,
            appUserID
        )
        assertThat(result).isFalse
    }

    // endregion

    // region shouldCalculateOfflineCustomerInfoInPostReceipt

    @Test
    fun `shouldCalculateOfflineCustomerInfoInPostReceipt returns true if server error `() {
        val isServerError = true
        assertThat(offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError)).isTrue
    }

    @Test
    fun `shouldCalculateOfflineCustomerInfoInPostReceipt returns true if server error but not finishing transactions`() {
        val isServerError = true
        every { appConfig.finishTransactions } returns false
        assertThat(offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError)).isFalse
    }

    @Test
    fun `shouldCalculateOfflineCustomerInfoInPostReceipt returns false if not server error`() {
        val isServerError = false
        assertThat(offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError)).isFalse
    }

    // endregion

    // region resetOfflineCustomerInfoCache

    @Test
    fun `resetOfflineCustomerInfoCache does nothing if offline customer info is null`() {
        assertThat(offlineEntitlementsManager.offlineCustomerInfo).isNull()
        offlineEntitlementsManager.resetOfflineCustomerInfoCache()
        assertThat(offlineEntitlementsManager.offlineCustomerInfo).isNull()
    }

    @Test
    fun `resetOfflineCustomerInfoCache changes offline customer info cache to null`() {
        val customerInfo = mockk<CustomerInfo>()
        mockCalculateOfflineEntitlements(successCustomerInfo = customerInfo)
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, {}, { fail("Should succeed") })
        assertThat(offlineEntitlementsManager.offlineCustomerInfo).isEqualTo(customerInfo)
        offlineEntitlementsManager.resetOfflineCustomerInfoCache()
        assertThat(offlineEntitlementsManager.offlineCustomerInfo).isNull()
    }

    // endregion

    // region calculateAndCacheOfflineCustomerInfo

    @Test
    fun `calculateAndCacheOfflineCustomerInfo returns customer info on success callback`() {
        val customerInfo = mockk<CustomerInfo>()
        mockCalculateOfflineEntitlements(successCustomerInfo = customerInfo)
        var customerInfoFromCallback: CustomerInfo? = null
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { customerInfoFromCallback = it },
            onError = { fail("Should succeed") }
        )
        assertThat(customerInfoFromCallback).isEqualTo(customerInfo)
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo caches computed customer info on success`() {
        val customerInfo = mockk<CustomerInfo>()
        mockCalculateOfflineEntitlements(successCustomerInfo = customerInfo)
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = {},
            onError = { fail("Should succeed") }
        )
        assertThat(offlineEntitlementsManager.offlineCustomerInfo).isEqualTo(customerInfo)
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo clears customer info cache on success`() {
        val customerInfo = mockk<CustomerInfo>()
        mockCalculateOfflineEntitlements(successCustomerInfo = customerInfo)
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = {},
            onError = { fail("Should succeed") }
        )
        verify(exactly = 1) {
            deviceCache.clearCustomerInfoCache(appUserID)
        }
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo only computes offline customer info one at a time and calls all success callbacks`() {
        val customerInfo = mockk<CustomerInfo>()
        val successCallbackSlot = slot<(CustomerInfo) -> Unit>()
        every {
            offlineEntitlementsCalculator.computeOfflineCustomerInfo(appUserID, capture(successCallbackSlot), any())
        } just Runs
        var callback1CustomerInfo: CustomerInfo? = null
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { callback1CustomerInfo = it },
            onError = { fail("Should succeed") }
        )
        var callback2CustomerInfo: CustomerInfo? = null
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { callback2CustomerInfo = it },
            onError = { fail("Should succeed") }
        )
        assertThat(callback1CustomerInfo).isNull()
        assertThat(callback2CustomerInfo).isNull()
        verify(exactly = 1) { offlineEntitlementsCalculator.computeOfflineCustomerInfo(any(), any(), any()) }
        successCallbackSlot.invoke(customerInfo)
        assertThat(callback1CustomerInfo).isEqualTo(customerInfo)
        assertThat(callback2CustomerInfo).isEqualTo(customerInfo)
        verify(exactly = 1) { offlineEntitlementsCalculator.computeOfflineCustomerInfo(any(), any(), any()) }
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo does not cache computed customer info on error`() {
        mockCalculateOfflineEntitlements(error = PurchasesError(PurchasesErrorCode.UnknownError))
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { fail("Should error") },
            onError = {}
        )
        assertThat(offlineEntitlementsManager.offlineCustomerInfo).isNull()
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo does not clear customer info cache on error`() {
        mockCalculateOfflineEntitlements(error = PurchasesError(PurchasesErrorCode.UnknownError))
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { fail("Should error") },
            onError = {}
        )
        verify(exactly = 0) {
            deviceCache.clearCustomerInfoCache(any())
        }
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo returns error`() {
        val error = PurchasesError(PurchasesErrorCode.UnknownError)
        mockCalculateOfflineEntitlements(error = error)
        var receivedError: PurchasesError? = null
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { fail("Should error") },
            onError = { receivedError = it }
        )
        assertThat(receivedError).isEqualTo(error)
    }

    @Test
    fun `calculateAndCacheOfflineCustomerInfo only computes offline customer info one at a time and calls all error callbacks`() {
        val error = PurchasesError(PurchasesErrorCode.UnknownError)
        val errorCallbackSlot = slot<(PurchasesError) -> Unit>()
        every {
            offlineEntitlementsCalculator.computeOfflineCustomerInfo(appUserID, any(), capture(errorCallbackSlot))
        } just Runs
        var callback1Error: PurchasesError? = null
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { fail("Should error") },
            onError = { callback1Error = it }
        )
        var callback2Error: PurchasesError? = null
        offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserID,
            onSuccess = { fail("Should error") },
            onError = { callback2Error = it }
        )
        assertThat(callback1Error).isNull()
        assertThat(callback2Error).isNull()
        verify(exactly = 1) { offlineEntitlementsCalculator.computeOfflineCustomerInfo(any(), any(), any()) }
        errorCallbackSlot.invoke(error)
        assertThat(callback1Error).isEqualTo(error)
        assertThat(callback2Error).isEqualTo(error)
        verify(exactly = 1) { offlineEntitlementsCalculator.computeOfflineCustomerInfo(any(), any(), any()) }
    }

    // endregion

    // region updateProductEntitlementMappingCacheIfStale

    @Test
    fun `updateProductEntitlementMappingCacheIfStale does nothing if cache not stale`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns false
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 0) { backend.getProductEntitlementMapping(any(), any()) }
    }

    @Test
    fun `updateProductEntitlementMappingCacheIfStale does nothing if not finishing transactions`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns true
        every { appConfig.finishTransactions } returns false
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 0) { backend.getProductEntitlementMapping(any(), any()) }
    }

    @Test
    fun `updateProductEntitlementMappingCacheIfStale does nothing if backend request errors`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns true
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 1) { backend.getProductEntitlementMapping(any(), any()) }
        backendErrorSlot.captured(PurchasesError(PurchasesErrorCode.NetworkError))
        verify(exactly = 0) { deviceCache.cacheProductEntitlementMapping(any()) }
    }

    @Test
    fun `updateProductEntitlementMappingCacheIfStale caches mapping from backend if request successful`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns true
        every { deviceCache.cacheProductEntitlementMapping(any()) } just Runs
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 1) { backend.getProductEntitlementMapping(any(), any()) }
        val expectedMappings = createProductEntitlementMapping()
        backendSuccessSlot.captured(expectedMappings)
        verify(exactly = 1) { deviceCache.cacheProductEntitlementMapping(expectedMappings) }
    }

    // endregion

    // region helpers

    private fun mockCalculateOfflineEntitlements(
        successCustomerInfo: CustomerInfo? = null,
        error: PurchasesError? = null
    ) {
        if (successCustomerInfo != null) {
            every {
                offlineEntitlementsCalculator.computeOfflineCustomerInfo(appUserID, captureLambda(), any())
            } answers {
                lambda<(CustomerInfo) -> Unit>().captured.invoke(successCustomerInfo)
            }
        } else if (error != null) {
            every {
                offlineEntitlementsCalculator.computeOfflineCustomerInfo(appUserID, any(), captureLambda())
            } answers {
                lambda<(PurchasesError) -> Unit>().captured.invoke(error)
            }
        }
    }

    // endregion
}
