package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubStoreProduct
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
internal class PurchasesCoroutinesCommonTest : BasePurchasesTest() {

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
        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    // endregion

    // region awaitPurchase

    @Test
    fun `await purchase - Success`() = runTest {
        val storeProduct = stubStoreProduct("abc")
        val purchaseOptionParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        val (storeTransaction, customerInfo) = purchases.awaitPurchase(purchaseOptionParams)

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        assertThat(storeTransaction).isNotNull
        assertThat(customerInfo).isNotNull
    }

    @Test
    fun `await purchase - Error`() = runTest {
        val storeProduct = stubStoreProduct("abc")
        val purchaseOptionParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        var result: Pair<StoreTransaction, CustomerInfo>? = null
        var exception: PurchasesTransactionException? = null

        runCatching {
            result = purchases.awaitPurchase(purchaseOptionParams)
        }.onFailure {
            exception = it as? PurchasesTransactionException
        }
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    // endregion
}
