package com.revenuecat.purchases.deeplinks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
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

@RunWith(AndroidJUnit4::class)
class WebPurchaseRedemptionHelperTest {

    private val userId = "test-user-id"
    private val redemptionToken = "test-redemption-token"
    private val webPurchaseRedemption = WebPurchaseRedemption(redemptionToken)

    private lateinit var customerInfo: CustomerInfo

    private lateinit var backend: Backend
    private lateinit var identityManager: IdentityManager
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var customerInfoUpdateHandler: CustomerInfoUpdateHandler

    private lateinit var webPurchaseRedemptionHelper: WebPurchaseRedemptionHelper

    @Before
    fun setUp() {
        customerInfo = mockk()
        backend = mockk()
        identityManager = mockk()
        offlineEntitlementsManager = mockk()
        customerInfoUpdateHandler = mockk()

        every { identityManager.currentAppUserID } returns userId
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(customerInfo, userId) } just Runs

        webPurchaseRedemptionHelper = WebPurchaseRedemptionHelper(
            backend = backend,
            identityManager = identityManager,
            offlineEntitlementsManager = offlineEntitlementsManager,
            customerInfoUpdateHandler = customerInfoUpdateHandler,
        )
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns success`() {
        mockBackendResult()
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {
            result = it
        }
        assertTrue(result is RedeemWebPurchaseListener.Result.Success)
        assertThat((result as RedeemWebPurchaseListener.Result.Success).customerInfo).isEqualTo(customerInfo)
    }

    @Test
    fun `handleRedeemWebPurchase posts token and resets offline entitlements cache on success`() {
        mockBackendResult()
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {}
        verify(exactly = 1) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and notifies listener on success`() {
        mockBackendResult()
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {}
        verify(exactly = 1) { customerInfoUpdateHandler.cacheAndNotifyListeners(customerInfo, userId) }
    }

    @Test
    fun `handleRedeemWebPurchase caches customer info for user that started request`() {
        val newUserId = "new-user-id"
        val deviceCache = mockk<DeviceCache>()
        val appConfig = mockk<AppConfig>()
        val realCustomerInfoUpdateHandler = CustomerInfoUpdateHandler(
            deviceCache,
            identityManager,
            offlineEntitlementsManager,
            appConfig,
            diagnosticsTracker = null,
        )
        val helper = WebPurchaseRedemptionHelper(
            backend,
            identityManager,
            offlineEntitlementsManager,
            realCustomerInfoUpdateHandler,
        )
        every { deviceCache.cacheCustomerInfo(any(), customerInfo) } just Runs
        every { backend.postRedeemWebPurchase(userId, redemptionToken, captureLambda()) } answers {
            every { identityManager.currentAppUserID } returns newUserId
            lambda<(RedeemWebPurchaseListener.Result) -> Unit>().captured.invoke(
                RedeemWebPurchaseListener.Result.Success(customerInfo),
            )
        }

        helper.handleRedeemWebPurchase(webPurchaseRedemption) {}

        verify(exactly = 1) { deviceCache.cacheCustomerInfo(userId, customerInfo) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns error`() {
        val expectedError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockBackendResult(result = RedeemWebPurchaseListener.Result.Error(expectedError))
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {
            result = it
        }
        assertTrue(result is RedeemWebPurchaseListener.Result.Error)
        assertThat((result as RedeemWebPurchaseListener.Result.Error).error).isEqualTo(expectedError)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any(), any()) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns belongs to other user`() {
        mockBackendResult(result = RedeemWebPurchaseListener.Result.PurchaseBelongsToOtherUser)
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {
            result = it
        }
        assertTrue(result is RedeemWebPurchaseListener.Result.PurchaseBelongsToOtherUser)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any(), any()) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns token expired`() {
        val expectedResult = RedeemWebPurchaseListener.Result.Expired("test-email")
        mockBackendResult(expectedResult)
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {
            result = it
        }
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any(), any()) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns invalid token`() {
        val expectedResult = RedeemWebPurchaseListener.Result.InvalidToken
        mockBackendResult(expectedResult)
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption) {
            result = it
        }
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any(), any()) }
    }

    private fun mockBackendResult(
        result: RedeemWebPurchaseListener.Result = RedeemWebPurchaseListener.Result.Success(customerInfo),
    ) {
        every { backend.postRedeemWebPurchase(userId, redemptionToken, captureLambda()) } answers {
            lambda<(RedeemWebPurchaseListener.Result) -> Unit>().captured.invoke(result)
        }
    }
}
