package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineEntitlementsIntegrationTest : BasePurchasesIntegrationTest() {

    private val initialActiveTransaction = StoreTransactionFactory.createStoreTransaction(
        skus = listOf(Constants.productIdToPurchase),
        purchaseToken = Constants.googlePurchaseToken
    )
    private val initialActivePurchases = mapOf(
        initialActiveTransaction.purchaseToken.sha1() to initialActiveTransaction
    )

    @After
    fun tearDown() {
        tearDownTest()
    }

    @Test
    fun entersOfflineEntitlementsModeIfNoCachedCustomerInfoAndCustomerInfoRequestReturns500() {
        ensureBlockFinishes { latch ->
            setupTestWaitingForInitialRequests(
                initialActivePurchases = initialActivePurchases
            ) {
                Purchases.sharedInstance.forceServerErrors = true

                Purchases.sharedInstance.invalidateCustomerInfoCache()

                Purchases.sharedInstance.getCustomerInfoWith(
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                    onError = {
                        latch.countDown()
                        fail("Expected success but got error: $it")
                    },
                    onSuccess = { receivedCustomerInfo ->
                        assertThat(receivedCustomerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
                            entitlementsToVerify
                        )
                        assertThat(receivedCustomerInfo.activeSubscriptions).containsExactly(
                            "${Constants.productIdToPurchase}:${Constants.basePlanIdToPurchase}"
                        )
                        latch.countDown()
                    }
                )
            }
        }
    }

    @Test
    fun doesNotEnterOfflineEntitlementsModeIfCachedCustomerInfoAndCustomerInfoRequestReturns500() {
        ensureBlockFinishes { latch ->
            setupTestWaitingForInitialRequests(
                initialActivePurchases = initialActivePurchases
            ) {
                Purchases.sharedInstance.forceServerErrors = true

                Purchases.sharedInstance.getCustomerInfoWith(
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                    onError = {
                        Purchases.sharedInstance.getCustomerInfoWith(
                            fetchPolicy = CacheFetchPolicy.CACHE_ONLY,
                            onError = {
                                latch.countDown()
                                fail("Expected success but got error: $it")
                            },
                            onSuccess = {
                                assertThat(it.entitlements.active).isEmpty()
                                assertThat(it.activeSubscriptions).isEmpty()
                                latch.countDown()
                            }
                        )
                    },
                    onSuccess = {
                        latch.countDown()
                        fail("Expected error but got success: $it")
                    }
                )
            }
        }
    }

    @Test
    fun entersOfflineEntitlementsModeIfPurchaseRequestReturns500() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()

        ensureBlockFinishes { latch ->
            setupTestWaitingForInitialRequests(
                initialActivePurchases = emptyMap()
            ) { activity ->
                Purchases.sharedInstance.forceServerErrors = true

                val receivedCustomerInfos = mutableListOf<CustomerInfo>()
                Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
                    receivedCustomerInfos.add(it)
                }
                every {
                    mockBillingAbstract.makePurchaseAsync(any(), any(), any(), any(), any(), any())
                } answers {
                    mockActivePurchases(initialActivePurchases)
                    latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(initialActiveTransaction))
                }
                mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))
                Purchases.sharedInstance.purchaseWith(
                    PurchaseParams.Builder(activity, storeProduct).build(),
                    onError = { error, _ ->
                        latch.countDown()
                        fail("Expected success but got error: $error")
                    },
                    onSuccess = { _, customerInfo ->
                        assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
                            entitlementsToVerify
                        )
                        assertThat(receivedCustomerInfos).hasSize(2)
                        assertThat(receivedCustomerInfos.first().entitlements.active).isEmpty()
                        assertThat(
                            receivedCustomerInfos.last().entitlements.active.keys
                        ).containsExactlyInAnyOrderElementsOf(entitlementsToVerify)
                        latch.countDown()
                    }
                )
            }
        }
    }

    // region helpers

    private fun setupTestWaitingForInitialRequests(
        initialActivePurchases: Map<String, StoreTransaction> = emptyMap(),
        postSetupTestCallback: (MainActivity) -> Unit = {}
    ) {
        setupTest(
            initialActivePurchases = initialActivePurchases
        ) {
            waitForInitialRequestsToEnd {
                postSetupTestCallback(it)
            }
        }
    }

    private fun waitForInitialRequestsToEnd(completion: () -> Unit) {
        Purchases.sharedInstance.offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { fail("Expected to succeed") },
                onSuccess = {
                    completion()
                }
            )
        }
    }

    // endregion helpers
}
