package com.revenuecat.purchases.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.MainActivity
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.forceServerErrors
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.resetSingleton
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

abstract class BaseOfflineEntitlementsWithInitialRequestsCompletedIntegrationTest :
    BaseOfflineEntitlementsIntegrationTest() {

    @Before
    fun setUp() {
        ensureBlockFinishes { latch ->
            setupTestWaitingForInitialRequests {
                latch.countDown()
            }
        }
    }

    // region helpers

    private fun setupTestWaitingForInitialRequests(
        postSetupTestCallback: (MainActivity) -> Unit = {}
    ) {
        setUpTest {
            waitForInitialRequestsToEnd {
                postSetupTestCallback(it)
            }
        }
    }

    private fun waitForInitialRequestsToEnd(completion: () -> Unit) {
        Purchases.sharedInstance.offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale {
            if (it != null) {
                fail("Expected to get product entitlement mapping but got error: $it")
            } else {
                Purchases.sharedInstance.getCustomerInfoWith(
                    onError = { customerInfoError ->
                        fail("Expected to succeed getting customer info. Got $customerInfoError")
                    },
                    onSuccess = {
                        completion()
                    }
                )
            }
        }
    }

    // endregion helpers
}

@RunWith(AndroidJUnit4::class)
class OfflineEntitlementsWithInitialRequestsCompletedAndInitialPurchasesIntegrationTest :
    BaseOfflineEntitlementsWithInitialRequestsCompletedIntegrationTest() {

    override val initialActivePurchasesToUse: Map<String, StoreTransaction> = initialActivePurchases

    @Test
    fun entersOfflineEntitlementsModeIfNoCachedCustomerInfoAndCustomerInfoRequestReturns500() {
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            Purchases.sharedInstance.invalidateCustomerInfoCache()

            Purchases.sharedInstance.getCustomerInfoWith(
                fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                onError = {
                    fail("Expected success but got error: $it")
                },
                onSuccess = { receivedCustomerInfo ->
                    assertCustomerInfoHasExpectedPurchaseData(receivedCustomerInfo)
                    latch.countDown()
                }
            )
        }
    }

    @Test
    fun doesNotEnterOfflineEntitlementsModeIfCachedCustomerInfoAndCustomerInfoRequestReturns500() {
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            Purchases.sharedInstance.getCustomerInfoWith(
                fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                onError = {
                    Purchases.sharedInstance.getCustomerInfoWith(
                        fetchPolicy = CacheFetchPolicy.CACHE_ONLY,
                        onError = {
                            fail("Expected success but got error: $it")
                        },
                        onSuccess = {
                            assertCustomerInfoDoesNotHavePurchaseData(it)
                            latch.countDown()
                        }
                    )
                },
                onSuccess = {
                    fail("Expected error but got success: $it")
                }
            )
        }
    }
}

@RunWith(AndroidJUnit4::class)
class OfflineEntitlementsWithInitialRequestsCompletedAndNoInitialPurchasesIntegrationTest :
    BaseOfflineEntitlementsWithInitialRequestsCompletedIntegrationTest() {

    @Test
    fun entersOfflineEntitlementsModeIfPurchaseRequestReturns500() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            val receivedCustomerInfosInListener = mutableListOf<CustomerInfo>()
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
                receivedCustomerInfosInListener.add(it)
            }
            mockPurchaseResult()
            mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ ->
                    fail("Expected success but got error: $error")
                },
                onSuccess = { _, customerInfo ->
                    assertCustomerInfoHasExpectedPurchaseData(customerInfo)
                    Assertions.assertThat(receivedCustomerInfosInListener).hasSize(2)
                    assertCustomerInfoDoesNotHavePurchaseData(receivedCustomerInfosInListener.first())
                    assertCustomerInfoHasExpectedPurchaseData(receivedCustomerInfosInListener.last())
                    assertAcknowledgePurchaseDidNotHappen()
                    latch.countDown()
                }
            )
        }
    }

    @Test
    fun doesNotEnterOfflineEntitlementsModeIfPurchasesConsumable() {
        val inAppProduct = StoreProductFactory.createGoogleStoreProduct(type = ProductType.INAPP)
        val inAppTransaction = StoreTransactionFactory.createStoreTransaction(
            skus = listOf(inAppProduct.id),
            type = ProductType.INAPP
        )

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            mockPurchaseResult(activePurchases = mapOf(inAppTransaction.purchaseToken.sha1() to inAppTransaction))
            mockBillingAbstract.mockQueryProductDetails(queryProductDetailsInAppReturn = listOf(inAppProduct))
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, inAppProduct).build(),
                onError = { _, _ ->
                    latch.countDown()
                },
                onSuccess = { _, _ ->
                    fail("Expected error")
                }
            )
        }
    }

    @Test
    fun gettingCustomerInfoWhileInOfflineEntitlementsModeReturnsOfflineCustomerInfo() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            mockPurchaseResult()
            mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ ->
                    fail("Expected success but got error: $error")
                },
                onSuccess = { _, customerInfo ->
                    assertCustomerInfoHasExpectedPurchaseData(customerInfo)
                    assertAcknowledgePurchaseDidNotHappen()

                    Purchases.sharedInstance.getCustomerInfoWith(
                        onError = {
                            fail("Expected success but got error: $it")
                        },
                        onSuccess = {
                            assertCustomerInfoHasExpectedPurchaseData(it)
                            latch.countDown()
                        }
                    )
                }
            )
        }
    }

    @Test
    fun sendsOfflinePurchasesAfterForegroundingApp() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            mockPurchaseResult()
            mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ ->
                    fail("Expected success but got error: $error")
                },
                onSuccess = { _, customerInfo ->
                    assertCustomerInfoHasExpectedPurchaseData(customerInfo)
                    assertAcknowledgePurchaseDidNotHappen()

                    Purchases.sharedInstance.forceServerErrors = false
                    mockActivePurchases(initialActivePurchases)

                    Purchases.sharedInstance.onAppForegrounded()
                    assertAcknowledgePurchaseDidHappen()

                    latch.countDown()
                }
            )
        }
    }

    @Test
    fun sendsOfflinePurchasesAfterRestartingApp() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            mockPurchaseResult()
            mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ ->
                    fail("Expected success but got error: $error")
                },
                onSuccess = { _, customerInfo ->
                    assertCustomerInfoHasExpectedPurchaseData(customerInfo)
                    assertAcknowledgePurchaseDidNotHappen()

                    Purchases.resetSingleton()
                    mockActivePurchases(initialActivePurchases)
                    configureSdk(activity, forceServerErrors = false)

                    assertAcknowledgePurchaseDidHappen()

                    latch.countDown()
                }
            )
        }
    }

    @Test
    fun recoversFromOfflineEntitlementsModeIfGetCustomerInfoSucceeds() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.forceServerErrors = true

            mockPurchaseResult()
            mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
            Purchases.sharedInstance.purchaseWith(
                PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ ->
                    fail("Expected success but got error: $error")
                },
                onSuccess = { _, customerInfo ->
                    assertCustomerInfoHasExpectedPurchaseData(customerInfo)
                    assertAcknowledgePurchaseDidNotHappen()

                    Purchases.sharedInstance.forceServerErrors = false

                    Purchases.sharedInstance.getCustomerInfoWith(
                        CacheFetchPolicy.FETCH_CURRENT,
                        onError = {
                            fail("Expected success but got error: $it")
                        },
                        onSuccess = {
                            // This is a known limitation. Ideally we would sync unsynced purchases
                            // as soon as possible to avoid getting outdated info from the backend.
                            assertCustomerInfoDoesNotHavePurchaseData(it)
                            assertAcknowledgePurchaseDidNotHappen()

                            latch.countDown()
                        }
                    )
                }
            )
        }
    }
}
