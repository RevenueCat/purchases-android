package com.revenuecat.purchases.galaxy.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail
import org.assertj.core.api.Assertions.assertThat
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseHandlerTest {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var purchaseHandler: PurchaseHandler
    private val productId = "product_id"

    private val appUserId = "user_id"
    private val onUnexpectedSuccess: (PurchaseVo) -> Unit = { fail("Expected onError to be called") }
    private val onUnexpectedError: (PurchasesError) -> Unit = { fail("Expected onSuccess to be called") }

    @BeforeTest
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        purchaseHandler = PurchaseHandler(iapHelperProvider)
    }

    @OptIn(GalaxySerialOperation::class, InternalRevenueCatAPI::class)
    @Test
    fun `purchase dispatches payment with obfuscated account id`() {
        every {
            iapHelperProvider.startPayment(any(), any(), any(), any())
        } returns true
        val onSuccess = mockk<(PurchaseVo) -> Unit>(relaxed = true)
        val onError = mockk<(PurchasesError) -> Unit>(relaxed = true)
        val itemIdSlot = slot<String>()
        val obfuscatedAccountIdSlot = slot<String>()

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onSuccess,
            onError = onError,
        )

        verify(exactly = 1) {
            iapHelperProvider.startPayment(
                itemId = capture(itemIdSlot),
                obfuscatedAccountId = capture(obfuscatedAccountIdSlot),
                obfuscatedProfileId = null,
                onPaymentListener = purchaseHandler,
            )
        }

        assertThat(itemIdSlot.captured).isEqualTo(productId)
        assertThat(obfuscatedAccountIdSlot.captured).isEqualTo(appUserId.sha256())
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `purchase errors when another request is in flight`() {
        every { iapHelperProvider.startPayment(any(), any(), any(), any()) } returns true

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = onUnexpectedError,
        )

        var receivedError: PurchasesError? = null
        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) { iapHelperProvider.startPayment(any(), any(), any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `purchase errors when Galaxy Store rejects request`() {
        every { iapHelperProvider.startPayment(any(), any(), any(), any()) } returnsMany listOf(false, true)
        var receivedError: PurchasesError? = null

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo("The Galaxy Store failed to accept the purchase request.")
        verify(exactly = 1) { iapHelperProvider.startPayment(any(), any(), any(), any()) }

        val nextOnSuccess = mockk<(PurchaseVo) -> Unit>(relaxed = true)

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = nextOnSuccess,
            onError = onUnexpectedError,
        )

        verify(exactly = 2) { iapHelperProvider.startPayment(any(), any(), any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `onPayment success invokes onSuccess and clears in-flight request`() {
        every { iapHelperProvider.startPayment(any(), any(), any(), any()) } returns true
        val purchase = mockk<PurchaseVo>()
        val onSuccess = mockk<(PurchaseVo) -> Unit>(relaxed = true)

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onSuccess,
            onError = onUnexpectedError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        purchaseHandler.onPayment(successErrorVo, purchase)
        verify(exactly = 1) { onSuccess(purchase) }

        // Ensure the request was cleared by allowing another purchase to be dispatched.
        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onSuccess,
            onError = onUnexpectedError,
        )

        verify(exactly = 2) { iapHelperProvider.startPayment(any(), any(), any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `onPayment success without purchase invokes onError`() {
        every { iapHelperProvider.startPayment(any(), any(), any(), any()) } returns true
        var receivedError: PurchasesError? = null

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        purchaseHandler.onPayment(successErrorVo, purchase = null)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.PURCHASE_RETURNED_SUCCESS_BUT_NO_PURCHASE_RESULT)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `onPayment error invokes onError with store problem`() {
        every { iapHelperProvider.startPayment(any(), any(), any(), any()) } returns true
        var receivedError: PurchasesError? = null

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = { receivedError = it },
        )

        val errorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_PAYMENT_IS_CANCELED.code
            every { errorString } returns "User canceled"
        }

        purchaseHandler.onPayment(errorVo, purchase = null)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.PurchaseCancelledError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("User canceled")

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = onUnexpectedError,
        )

        verify(exactly = 2) { iapHelperProvider.startPayment(any(), any(), any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `onPayment network error maps to NetworkError`() {
        every { iapHelperProvider.startPayment(
            any(), any(),
            any(),
            any())
        } returns true
        var receivedError: PurchasesError? = null

        purchaseHandler.purchase(
            appUserID = appUserId,
            productId = productId,
            onSuccess = onUnexpectedSuccess,
            onError = { receivedError = it },
        )

        val errorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "no network"
        }

        purchaseHandler.onPayment(errorVo, purchase = null)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("no network")
    }
}
