package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
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
        val storeProduct = stubStoreProduct(STUB_PRODUCT_IDENTIFIER)
        val purchaseOptionParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        with(mockBillingAbstract) {
            every {
                makePurchaseAsync(any(), any(), any(), any(), any(), any())
            } answers {
                capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
                    getMockedPurchaseList(
                        storeProduct.id,
                        "adgsagas",
                        ProductType.SUBS
                    )
                )
            }
        }
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
        var result: PurchaseResult? = null
        var exception: PurchasesTransactionException? = null

        val storeProduct = stubStoreProduct(STUB_PRODUCT_IDENTIFIER)
        val purchaseOptionParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        with(mockBillingAbstract) {
            every {
                makePurchaseAsync(any(), any(), any(), any(), any(), any())
            } answers {
                val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
                capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)

            }
        }

        runCatching {
            result = purchases.awaitPurchase(purchaseOptionParams)
        }.onFailure {
            exception = it as? PurchasesTransactionException
        }

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

    // awaitGetProducts
    @Test
    fun `getProducts - Success`() = runTest {
        val subsIds = listOf("product_1")
        val inAppIds = listOf("product_2")
        val productIds = subsIds + inAppIds
        val subs = mockStoreProduct(subsIds, subsIds, ProductType.SUBS)
        val inApps = mockStoreProduct(inAppIds, inAppIds, ProductType.INAPP)
        every {
            mockBillingAbstract.queryProductDetailsAsync(
                productType = ProductType.SUBS,
                productIds = productIds.toSet(),
                appInBackground = any(),
                onReceive = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(subs)
        }
        every {
            mockBillingAbstract.queryProductDetailsAsync(
                productType = ProductType.INAPP,
                productIds = productIds.toSet(),
                appInBackground = any(),
                onReceive = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(inApps)
        }

        val result = purchases.awaitGetProducts(productIds)

        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(productIds.size)
        assertThat(result.containsAll(subs)).isTrue()
        assertThat(result.containsAll(inApps)).isTrue()
    }

    @Test
    fun `get products - ConfigurationError`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Offerings config error")
        val subsIds = listOf("product_1")
        val inAppIds = listOf("product_2")
        val productIds = subsIds + inAppIds
        every {
            mockBillingAbstract.queryProductDetailsAsync(
                productType = ProductType.SUBS,
                productIds = productIds.toSet(),
                appInBackground = any(),
                onReceive = any(),
                onError = captureLambda(),
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }

        var result: List<StoreProduct>? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitGetProducts(productIds)
        }.onFailure {
            exception = it
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    // endregion

    // region awaitRestore

    @Test
    fun `restore - Success`() = runTest {
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                appInBackground = any(),
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any(),
            )
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(listOf(mockk(relaxed = true)))
            }
        }

        val result: CustomerInfo = purchases.awaitRestore()

        verify(exactly = 1) {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                any(),
                any(),
                any(),
            )
        }
        assertThat(result).isNotNull
    }

    @Test
    fun `restore - Success - customer info matches expectations`() = runTest {
        val afterRestoreCustomerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>(relaxed = true)
        every {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = storeTransaction,
                storeProduct = any(),
                isRestore = true,
                appUserID = appUserId,
                initiationSource = PostReceiptInitiationSource.RESTORE,
                appInBackground = any(),
                onSuccess = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(StoreTransaction, CustomerInfo) -> Unit>().captured.also {
                it.invoke(storeTransaction, afterRestoreCustomerInfo)
            }
        }
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                appInBackground = any(),
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any()
            )
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(listOf(storeTransaction))
            }
        }

        val result: CustomerInfo = purchases.awaitRestore()

        verify(exactly = 1) {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                appInBackground = any(),
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = any(),
            )
        }
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(afterRestoreCustomerInfo)
    }

    @Test
    fun `restore - CustomerInfoError`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.CustomerInfoError, "Customer info error")
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                appInBackground = any(),
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = captureLambda(),
            )
        } answers {
            lambda<(PurchasesError?) -> Unit>().captured.invoke(error)
        }

        var result: CustomerInfo? = null
        var exception: Throwable? = null
        runCatching {
            result = purchases.awaitRestore()
        }.onFailure {
            exception = it
        }

        verify(exactly = 1) {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                appInBackground = any(),
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = any(),
            )
        }

        assertThat(result).isNull()
        assertThat(exception).isNotNull
        assertThat(exception).isInstanceOf(PurchasesException::class.java)
        assertThat((exception as PurchasesException).code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
    }

    // endregion
}
