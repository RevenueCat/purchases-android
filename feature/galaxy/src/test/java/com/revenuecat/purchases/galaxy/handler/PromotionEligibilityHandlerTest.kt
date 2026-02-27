package com.revenuecat.purchases.galaxy.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo
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
class PromotionEligibilityHandlerTest {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var promotionEligibilityHandler: PromotionEligibilityHandler

    private val unexpectedOnSuccess: (List<PromotionEligibilityVo>) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onSuccess to be called") }

    @BeforeTest
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        promotionEligibilityHandler = PromotionEligibilityHandler(iapHelperProvider)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getPromotionEligibilities returns empty list for empty request`() {
        var receivedEligibilities: List<PromotionEligibilityVo>? = null

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = emptyList(),
            onSuccess = { receivedEligibilities = it },
            onError = unexpectedOnError,
        )

        assertThat(receivedEligibilities).isEmpty()
        verify(exactly = 0) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getPromotionEligibilities errors when another request is in flight`() {
        every { iapHelperProvider.getPromotionEligibility(any(), any()) } returns true

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("first"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        var receivedError: PurchasesError? = null
        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("second"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `empty request succeeds even with in-flight request and does not clear it`() {
        every { iapHelperProvider.getPromotionEligibility(any(), any()) } returns true

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("in_flight"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )

        var receivedEligibilities: List<PromotionEligibilityVo>? = null
        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = emptyList(),
            onSuccess = { receivedEligibilities = it },
            onError = unexpectedOnError,
        )
        assertThat(receivedEligibilities).isEmpty()

        var receivedError: PurchasesError? = null
        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("should_error"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)

        verify(exactly = 1) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getPromotionEligibilities errors when Galaxy Store rejects request and clears in-flight`() {
        every { iapHelperProvider.getPromotionEligibility(any(), any()) } returnsMany listOf(false, true)

        var receivedError: PurchasesError? = null
        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("product"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_PROMOTION_ELIGIBILITY_REQUEST)
        verify(exactly = 1) { iapHelperProvider.getPromotionEligibility(any(), any()) }

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful response forwards results, clears in-flight, and sends item IDs`() {
        val itemIdsSlot = slot<String>()
        every { iapHelperProvider.getPromotionEligibility(capture(itemIdsSlot), any()) } returns true

        val eligibilities = arrayListOf(mockk<PromotionEligibilityVo>(), mockk<PromotionEligibilityVo>())
        val onSuccess = mockk<(List<PromotionEligibilityVo>) -> Unit>(relaxed = true)

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("a", "b", "c"),
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> { every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code }
        promotionEligibilityHandler.onGetPromotionEligibility(successErrorVo, eligibilities)

        verify(exactly = 1) { onSuccess(eligibilities) }
        assertThat(itemIdsSlot.captured).isEqualTo("a,b,c")

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("next"),
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful response clears in-flight before invoking onSuccess`() {
        every { iapHelperProvider.getPromotionEligibility(any(), any()) } returns true

        var nestedError: PurchasesError? = null
        val onSuccess: (List<PromotionEligibilityVo>) -> Unit = {
            promotionEligibilityHandler.getPromotionEligibilities(
                productIds = listOf("nested"),
                onSuccess = { },
                onError = { nestedError = it },
            )
        }

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("first"),
            onSuccess = onSuccess,
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> { every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code }
        promotionEligibilityHandler.onGetPromotionEligibility(successErrorVo, arrayListOf(mockk()))

        assertThat(nestedError).isNull()
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful response with empty results forwards store problem error and clears in-flight`() {
        every { iapHelperProvider.getPromotionEligibility(any(), any()) } returns true

        var receivedError: PurchasesError? = null
        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("a"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> { every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code }
        promotionEligibilityHandler.onGetPromotionEligibility(successErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(GalaxyStrings.PROMOTION_ELIGIBILITY_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS)

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `failed response maps error and clears in-flight`() {
        every { iapHelperProvider.getPromotionEligibility(any(), any()) } returns true

        var receivedError: PurchasesError? = null
        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("a"),
            onSuccess = unexpectedOnSuccess,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "no network"
        }
        promotionEligibilityHandler.onGetPromotionEligibility(failingErrorVo, arrayListOf(mockk()))

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("no network")

        promotionEligibilityHandler.getPromotionEligibilities(
            productIds = listOf("next"),
            onSuccess = mockk(relaxed = true),
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }
}
