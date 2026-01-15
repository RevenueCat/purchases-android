package com.revenuecat.purchases.galaxy.handler

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.vo.AcknowledgeVo
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
class AcknowledgePurchaseHandlerTest {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var acknowledgePurchaseHandler: AcknowledgePurchaseHandler
    private lateinit var context: Context

    private val unexpectedOnSuccess: (AcknowledgeVo) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onSuccess to be called") }

    @BeforeTest
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        context = mockk(relaxed = true)
        acknowledgePurchaseHandler = AcknowledgePurchaseHandler(iapHelperProvider, context)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `acknowledgePurchase errors when another request is in flight`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns true
        every { iapHelperProvider.acknowledgePurchases(any(), any()) } returns true

        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("first"),
            onSuccess = unexpectedOnSuccess,
            onError = unexpectedOnError,
        )

        var receivedError: PurchasesError? = null
        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("second"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) { iapHelperProvider.acknowledgePurchases(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `acknowledgePurchase returns early when acknowledgement unavailable`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns false

        var receivedError: PurchasesError? = null
        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.WARNING_ACKNOWLEDGING_PURCHASES_UNAVAILABLE)
        verify(exactly = 0) { iapHelperProvider.acknowledgePurchases(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `acknowledgePurchase errors when Galaxy Store rejects request`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns true
        every { iapHelperProvider.acknowledgePurchases(any(), any()) } returnsMany listOf(false, true)

        var receivedError: PurchasesError? = null
        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_ACKNOWLEDGE_REQUEST)
        verify(exactly = 1) { iapHelperProvider.acknowledgePurchases(any(), any()) }

        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("next-token"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.acknowledgePurchases(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful acknowledge forwards results, clears in-flight request, and sends purchase tokens to be acknowledged`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns true
        val purchaseIdsSlot = slot<String>()
        every { iapHelperProvider.acknowledgePurchases(capture(purchaseIdsSlot), any()) } returns true
        val onSuccess = mockk<(AcknowledgeVo) -> Unit>(relaxed = true)
        val transaction = transactionWithToken("token1")

        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transaction,
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        val acknowledgementResult = mockk<AcknowledgeVo>()
        acknowledgePurchaseHandler.onAcknowledgePurchases(successErrorVo, arrayListOf(acknowledgementResult))

        verify(exactly = 1) { onSuccess(acknowledgementResult) }
        assertThat(purchaseIdsSlot.captured).isEqualTo("token1")

        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("token2"),
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.acknowledgePurchases(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful acknowledge with empty results forwards store problem error`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns true
        every { iapHelperProvider.acknowledgePurchases(any(), any()) } returns true

        var receivedError: PurchasesError? = null
        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        acknowledgePurchaseHandler.onAcknowledgePurchases(successErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS)

        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.acknowledgePurchases(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `failed acknowledge maps error and clears in-flight`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns true
        every { iapHelperProvider.acknowledgePurchases(any(), any()) } returns true

        var receivedError: PurchasesError? = null
        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "no network"
        }
        acknowledgePurchaseHandler.onAcknowledgePurchases(failingErrorVo, arrayListOf())
        verify(exactly = 1) { iapHelperProvider.acknowledgePurchases(any(), any()) }

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("no network")

        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.acknowledgePurchases(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful acknowledge with multiple results forwards store problem error`() {
        every { iapHelperProvider.isAcknowledgeAvailable(context) } returns true
        every { iapHelperProvider.acknowledgePurchases(any(), any()) } returns true

        var receivedError: PurchasesError? = null
        acknowledgePurchaseHandler.acknowledgePurchase(
            transaction = transactionWithToken("token"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        acknowledgePurchaseHandler.onAcknowledgePurchases(
            successErrorVo,
            arrayListOf(mockk<AcknowledgeVo>(), mockk<AcknowledgeVo>()),
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_MORE_THAN_ONE_RESULT)
    }

    private fun transactionWithToken(token: String) = mockk<StoreTransaction> {
        every { purchaseToken } returns token
    }
}
