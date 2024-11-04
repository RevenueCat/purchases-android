package com.revenuecat.purchases.deeplinks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class WebPurchaseRedemptionHelperTest {

    private val userId = "test-user-id"
    private val redemptionToken = "test-redemption-token"
    private val deepLink = Purchases.DeepLink.WebPurchaseRedemption(redemptionToken)

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
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(customerInfo) } just Runs

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
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {
            result = it
        }
        assertTrue(result is RedeemWebPurchaseListener.Result.Success)
        assertThat((result as RedeemWebPurchaseListener.Result.Success).customerInfo).isEqualTo(customerInfo)
    }

    @Test
    fun `handleRedeemWebPurchase posts token and resets offline entitlements cache on success`() {
        mockBackendResult()
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {}
        verify(exactly = 1) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and notifies listener on success`() {
        mockBackendResult()
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {}
        verify(exactly = 1) { customerInfoUpdateHandler.cacheAndNotifyListeners(customerInfo) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns error`() {
        val expectedError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockBackendResult(result = RedeemWebPurchaseListener.Result.Error(expectedError))
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {
            result = it
        }
        assertTrue(result is RedeemWebPurchaseListener.Result.Error)
        assertThat((result as RedeemWebPurchaseListener.Result.Error).error).isEqualTo(expectedError)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns already redeemed`() {
        mockBackendResult(result = RedeemWebPurchaseListener.Result.AlreadyRedeemed)
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {
            result = it
        }
        assertTrue(result is RedeemWebPurchaseListener.Result.AlreadyRedeemed)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns token expired`() {
        val expectedResult = RedeemWebPurchaseListener.Result.Expired("test-email", wasEmailSent = false)
        mockBackendResult(expectedResult)
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {
            result = it
        }
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    @Test
    fun `handleRedeemWebPurchase posts token and returns invalid token`() {
        val expectedResult = RedeemWebPurchaseListener.Result.InvalidToken
        mockBackendResult(expectedResult)
        var result: RedeemWebPurchaseListener.Result? = null
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(deepLink) {
            result = it
        }
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 0) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
    }

    private fun mockBackendResult(
        result: RedeemWebPurchaseListener.Result = RedeemWebPurchaseListener.Result.Success(customerInfo),
    ) {
        every { backend.postRedeemWebPurchase(userId, redemptionToken, captureLambda()) } answers {
            lambda<(RedeemWebPurchaseListener.Result) -> Unit>().captured.invoke(result)
        }
    }
}
