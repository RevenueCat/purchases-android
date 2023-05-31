package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CoroutinesTest : BasePurchasesTest() {

    @Test
    fun `retrieve customer info - Success`() = runTest {
        mockCustomerInfoHelper()

        val result = purchases.awaitCustomerInfo()

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        Assertions.assertThat(result).isNotNull
    }

    @Test
    fun `retrieve customer info - Error`() = runTest {
        mockCustomerInfoHelper(PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error"))

        var result: CustomerInfo? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitCustomerInfo()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        Assertions.assertThat(result).isNull()
        Assertions.assertThat(exception).isNotNull
    }

    @Test
    fun `retrieve customer info - CustomerInfoError`() = runTest {
        mockCustomerInfoHelper(PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error"))

        var exception: Throwable? = null

        runCatching {
            purchases.awaitCustomerInfo()
        }.onFailure {
            exception = it
        }

        Assertions.assertThat(exception).isNotNull
        Assertions.assertThat(exception).isInstanceOf(PurchasesException::class.java)
        Assertions.assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }

    // endregion
}