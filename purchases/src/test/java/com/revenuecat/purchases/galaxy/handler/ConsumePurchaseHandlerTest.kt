package com.revenuecat.purchases.galaxy.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
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
class ConsumePurchaseHandlerTest {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var consumePurchaseHandler: ConsumePurchaseHandler

    private val unexpectedOnSuccess: (ConsumeVo) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onSuccess to be called") }

    @BeforeTest
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        consumePurchaseHandler = ConsumePurchaseHandler(iapHelperProvider)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumePurchase errors when another request is in flight`() {
        every { iapHelperProvider.consumePurchaseItems(any(), any()) } returns true

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("first"),
            onSuccess = unexpectedOnSuccess,
            onError = unexpectedOnError,
        )

        var receivedError: PurchasesError? = null
        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("second"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) { iapHelperProvider.consumePurchaseItems(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumePurchase errors when Galaxy Store rejects request`() {
        every { iapHelperProvider.consumePurchaseItems(any(), any()) } returnsMany listOf(false, true)
        var receivedError: PurchasesError? = null

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo("The Galaxy Store failed to accept the purchase request.")
        verify(exactly = 1) { iapHelperProvider.consumePurchaseItems(any(), any()) }

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("next-token"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        verify(exactly = 2) { iapHelperProvider.consumePurchaseItems(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful consume forwards results, clears in-flight request, and sends purchase tokens to be consumed`() {
        val purchaseIdsSlot = slot<String>()
        every { iapHelperProvider.consumePurchaseItems(capture(purchaseIdsSlot), any()) } returns true
        val onSuccess = mockk<(ConsumeVo) -> Unit>(relaxed = true)
        val transaction = transactionWithToken("token1")

        consumePurchaseHandler.consumePurchase(
            transaction = transaction,
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        val consumptionResult = mockk<ConsumeVo>()
        val consumptionResults = arrayListOf(consumptionResult)
        consumePurchaseHandler.onConsumePurchasedItems(successErrorVo, consumptionResults)

        verify(exactly = 1) { onSuccess(consumptionResult) }
        assertThat(purchaseIdsSlot.captured).isEqualTo("token1")

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("token3"),
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.consumePurchaseItems(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful consume with empty results forwards store problem error`() {
        every { iapHelperProvider.consumePurchaseItems(any(), any()) } returns true
        var receivedError: PurchasesError? = null

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        consumePurchaseHandler.onConsumePurchasedItems(successErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.CONSUMPTION_REQUEST_RETURNED_SUCCESS_BUT_NO_CONSUMPTION_RESULTS)

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.consumePurchaseItems(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `failed consume maps error and clears in-flight`() {
        every { iapHelperProvider.consumePurchaseItems(any(), any()) } returns true
        var receivedError: PurchasesError? = null

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "no network"
        }
        consumePurchaseHandler.onConsumePurchasedItems(failingErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("no network")

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.consumePurchaseItems(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful consume with multiple results forwards store problem error`() {
        every { iapHelperProvider.consumePurchaseItems(any(), any()) } returns true
        var receivedError: PurchasesError? = null

        consumePurchaseHandler.consumePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        consumePurchaseHandler.onConsumePurchasedItems(
            successErrorVo,
            arrayListOf(mockk<ConsumeVo>(), mockk<ConsumeVo>()),
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.CONSUMPTION_REQUEST_RETURNED_MORE_THAN_ONE_RESULT)
    }

    private fun transactionWithToken(token: String) = mockk<StoreTransaction> {
        every { purchaseToken } returns token
    }
}
