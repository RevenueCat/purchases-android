package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasesCoroutinesTest : BasePurchasesTest() {

    // region awaitCustomerInfo
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
                any(),
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `retrieve customer info - Success - customer info matches expectations`() = runTest {
        mockCustomerInfoHelper()

        val result = purchases.awaitCustomerInfo()

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                any(),
                any(),
                any(),
                any(),
            )
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(mockInfo)
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
                any(),
            )
        }
        assertThat(result).isNull()
        assertThat(exception).isNotNull
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

        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }

    // endregion

    // region awaitOfferings
    @Test
    fun `retrieve offerings - Success`() = runTest {
        mockOfferingsManagerGetOfferings()

        val result = purchases.awaitOfferings()

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `retrieve offerings - Success - offerings match expectations`() = runTest {
        val offerings = mockOfferingsManagerGetOfferings()

        val result = purchases.awaitOfferings()

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(offerings)
    }

    @Test
    fun `retrieve offerings - Error`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.UnknownError, "Offerings error")
        mockOfferingsManagerGetOfferings(error)

        var result: Offerings? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitOfferings()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        assertThat(result).isNull()
        assertThat(exception).isNotNull
    }

    @Test
    fun `retrieve offerings - ConfigurationError`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Offerings config error")
        mockOfferingsManagerGetOfferings(error)

        var result: Offerings? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitOfferings()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    // endregion
}
