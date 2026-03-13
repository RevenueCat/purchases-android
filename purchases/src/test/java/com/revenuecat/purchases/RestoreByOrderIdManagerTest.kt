package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.RestoreByOrderIdManager
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.RestoreByOrderIdListener
import com.revenuecat.purchases.utils.RateLimiter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class RestoreByOrderIdManagerTest {

    private val userId = "test-user-id"
    private val orderId = "test-order-id"

    private lateinit var customerInfo: CustomerInfo

    private lateinit var backend: Backend
    private lateinit var identityManager: IdentityManager
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var customerInfoUpdateHandler: CustomerInfoUpdateHandler
    private lateinit var rateLimiter: RateLimiter

    private lateinit var restoreByOrderIdManager: RestoreByOrderIdManager

    @Before
    fun setUp() {
        customerInfo = mockk()
        backend = mockk()
        identityManager = mockk()
        offlineEntitlementsManager = mockk()
        customerInfoUpdateHandler = mockk()
        rateLimiter = RateLimiter(maxCallsInPeriod = 5, periodSeconds = 60.seconds)

        every { identityManager.currentAppUserID } returns userId
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(customerInfo) } just Runs

        restoreByOrderIdManager = RestoreByOrderIdManager(
            backend = backend,
            identityManager = identityManager,
            offlineEntitlementsManager = offlineEntitlementsManager,
            customerInfoUpdateHandler = customerInfoUpdateHandler,
            mainHandler = null,
            rateLimiter = rateLimiter,
        )
    }

    @Test
    fun `restoreByOrderId calls backend and returns success`() {
        mockBackendResult()
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.Success)
        assertThat((result as RestoreByOrderIdListener.Result.Success).customerInfo).isEqualTo(customerInfo)
    }

    @Test
    fun `restoreByOrderId resets offline entitlements cache on success`() {
        mockBackendResult()
        restoreByOrderIdManager.restoreByOrderId(orderId) {}
        verify(exactly = 1) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
    }

    @Test
    fun `restoreByOrderId caches and notifies listeners on success`() {
        mockBackendResult()
        restoreByOrderIdManager.restoreByOrderId(orderId) {}
        verify(exactly = 1) { customerInfoUpdateHandler.cacheAndNotifyListeners(customerInfo) }
    }

    @Test
    fun `restoreByOrderId returns error without updating cache`() {
        val expectedError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockBackendResult(result = RestoreByOrderIdListener.Result.Error(expectedError))
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.Error)
        assertThat((result as RestoreByOrderIdListener.Result.Error).error).isEqualTo(expectedError)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `restoreByOrderId returns OrderIdNotFound without updating cache`() {
        mockBackendResult(result = RestoreByOrderIdListener.Result.OrderIdNotFound)
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.OrderIdNotFound)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `restoreByOrderId returns OrderNotEligible without updating cache`() {
        mockBackendResult(result = RestoreByOrderIdListener.Result.OrderNotEligible)
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.OrderNotEligible)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `restoreByOrderId returns FeatureNotEnabled without updating cache`() {
        mockBackendResult(result = RestoreByOrderIdListener.Result.FeatureNotEnabled)
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.FeatureNotEnabled)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `restoreByOrderId returns PurchaseBelongsToAuthenticatedUser without updating cache`() {
        mockBackendResult(result = RestoreByOrderIdListener.Result.PurchaseBelongsToAuthenticatedUser)
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.PurchaseBelongsToAuthenticatedUser)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `restoreByOrderId returns RateLimitExceeded from backend without updating cache`() {
        mockBackendResult(result = RestoreByOrderIdListener.Result.RateLimitExceeded)
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }
        assertTrue(result is RestoreByOrderIdListener.Result.RateLimitExceeded)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `restoreByOrderId returns RateLimitExceeded when client rate limit exceeded`() {
        // Exhaust the rate limiter (5 calls per 60 seconds)
        repeat(5) { mockBackendResultAndCall() }

        // The 6th call should be rate limited without calling the backend
        var result: RestoreByOrderIdListener.Result? = null
        restoreByOrderIdManager.restoreByOrderId(orderId) { result = it }

        assertTrue(result is RestoreByOrderIdListener.Result.RateLimitExceeded)
        // Backend should only have been called 5 times (not 6)
        verify(exactly = 5) { backend.postRestoreByOrderId(userId, orderId, any()) }
    }

    private fun mockBackendResultAndCall() {
        mockBackendResult()
        restoreByOrderIdManager.restoreByOrderId(orderId) {}
    }

    private fun mockBackendResult(
        result: RestoreByOrderIdListener.Result = RestoreByOrderIdListener.Result.Success(customerInfo),
    ) {
        every { backend.postRestoreByOrderId(userId, orderId, captureLambda()) } answers {
            lambda<(RestoreByOrderIdListener.Result) -> Unit>().captured.invoke(result)
        }
    }
}
