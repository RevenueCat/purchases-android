package com.revenuecat.purchases.galaxy.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorVoUtilsTest {

    @Test
    fun `isError returns false when error code indicates success`() {
        val errorVo = mockErrorVo(GalaxyErrorCode.IAP_ERROR_NONE.code)

        assertThat(errorVo.isError()).isFalse
    }

    @Test
    fun `isError returns true when error code matches a known error`() {
        val errorVo = mockErrorVo(GalaxyErrorCode.IAP_PAYMENT_IS_CANCELED.code)

        assertThat(errorVo.isError()).isTrue
    }

    @Test
    fun `isError treats unknown error codes as errors`() {
        val unexpectedCode = 999
        val errorVo = mockErrorVo(unexpectedCode)

        assertThat(errorVo.isError()).isTrue
    }

    @Test
    fun `toPurchasesError maps known galaxy error codes to purchases error codes`() {
        val expectedMappings = mapOf(
            GalaxyErrorCode.IAP_PAYMENT_IS_CANCELED to PurchasesErrorCode.PurchaseCancelledError,
            GalaxyErrorCode.IAP_ERROR_ALREADY_PURCHASED to PurchasesErrorCode.ProductAlreadyPurchasedError,
            GalaxyErrorCode.IAP_ERROR_PRODUCT_DOES_NOT_EXIST to PurchasesErrorCode.ProductNotAvailableForPurchaseError,
            GalaxyErrorCode.IAP_ERROR_ITEM_GROUP_DOES_NOT_EXIST to PurchasesErrorCode.ProductNotAvailableForPurchaseError,
            GalaxyErrorCode.IAP_ERROR_NOT_EXIST_LOCAL_PRICE to PurchasesErrorCode.ProductNotAvailableForPurchaseError,
            GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE to PurchasesErrorCode.NetworkError,
            GalaxyErrorCode.IAP_ERROR_IOEXCEPTION_ERROR to PurchasesErrorCode.NetworkError,
            GalaxyErrorCode.IAP_ERROR_SOCKET_TIMEOUT to PurchasesErrorCode.NetworkError,
            GalaxyErrorCode.IAP_ERROR_CONNECT_TIMEOUT to PurchasesErrorCode.NetworkError,
            GalaxyErrorCode.IAP_ERROR_WHILE_RUNNING to PurchasesErrorCode.PurchaseInvalidError,
            GalaxyErrorCode.IAP_ERROR_NEED_APP_UPGRADE to PurchasesErrorCode.StoreProblemError,
            GalaxyErrorCode.IAP_ERROR_INITIALIZATION to PurchasesErrorCode.StoreProblemError,
            GalaxyErrorCode.IAP_ERROR_COMMON to PurchasesErrorCode.StoreProblemError,
            GalaxyErrorCode.IAP_ERROR_CONFIRM_INBOX to PurchasesErrorCode.StoreProblemError,
            GalaxyErrorCode.IAP_ERROR_NOT_AVAILABLE_SHOP to PurchasesErrorCode.PurchaseNotAllowedError,
            GalaxyErrorCode.IAP_ERROR_INVALID_ACCESS_TOKEN to PurchasesErrorCode.InvalidCredentialsError,
            GalaxyErrorCode.IAP_ERROR_NONE to PurchasesErrorCode.UnknownError,
        )

        GalaxyErrorCode.values().forEach { galaxyCode ->
            val purchasesErrorCode = expectedMappings.getValue(galaxyCode)
            val message = "message for ${galaxyCode.name}"
            val errorVo = mockErrorVo(galaxyCode.code, message)

            val purchasesError = errorVo.toPurchasesError()

            assertThat(purchasesError)
                .describedAs("Galaxy code ${galaxyCode.name} should map to $purchasesErrorCode")
                .isNotNull
            assertThat(purchasesError.code).isEqualTo(purchasesErrorCode)
            assertThat(purchasesError.underlyingErrorMessage).isEqualTo(message)
        }
    }

    @Test
    fun `toPurchasesError defaults to UnknownError for unknown codes`() {
        val errorVo = mockErrorVo(999, "Unexpected")

        val purchasesError = errorVo.toPurchasesError()

        assertThat(purchasesError?.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(purchasesError?.underlyingErrorMessage).isEqualTo("Unexpected")
    }

    private fun mockErrorVo(errorCode: Int, errorString: String = ""): ErrorVo =
        mockk<ErrorVo>().apply {
            every { this@apply.errorCode } returns errorCode
            every { this@apply.errorString } returns errorString
        }
}
