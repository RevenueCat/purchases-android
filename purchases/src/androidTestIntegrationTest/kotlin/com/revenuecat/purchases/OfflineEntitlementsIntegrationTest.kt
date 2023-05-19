package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineEntitlementsIntegrationTest : BasePurchasesIntegrationTest() {

    private val testProductId = "annual_freetrial"
    private val testBasePlanId = "p1y"
    private val productEntitlementMappingMockResponse = """
        {
          "product_entitlement_mapping": {
            "$testProductId": {
              "base_plan_id": "$testBasePlanId",
              "entitlements": [
                "pro_cat"
              ],
              "product_identifier": "$testProductId"
            }
          }
        }
    """.trimIndent()
    private val customerInfoCacheString = """
        {
          "request_date": "2023-05-16T14:06:28Z",
          "request_date_ms": 1684245988765,
          "subscriber": {
            "entitlements": {},
            "first_seen": "2023-05-16T14:06:28Z",
            "last_seen": "2023-05-16T14:06:28Z",
            "management_url": null,
            "non_subscriptions": {},
            "original_app_user_id": "$testUserId",
            "original_application_version": null,
            "original_purchase_date": null,
            "other_purchases": {},
            "subscriptions": {}
          },
          "schema_version": 3,
          "customer_info_request_date": 1684245988766
        }
    """.trimIndent()
    private val cacheKeyPrefix = "com.revenuecat.purchases.${Constants.apiKey}"
    private val productEntitlementMappingCacheKey = "$cacheKeyPrefix.productEntitlementMapping"
    private val initialActiveTransaction = StoreTransactionFactory.createStoreTransaction(
        skus = listOf(testProductId),
        purchaseToken = "test-token"
    )
    private val initialActivePurchases = mapOf(
        initialActiveTransaction.purchaseToken.sha1() to initialActiveTransaction
    )

    @After
    fun tearDown() {
        tearDownTest()
        Purchases.proxyURL = null
    }

    @Test
    fun entersOfflineEntitlementsModeIfNoCachedCustomerInfoAndCustomerInfoRequestReturns500() {
        ensureBlockFinishes { latch ->
            setupTest(
                buildSharedPreferencesMap(shouldIncludeCustomerInfo = false),
                initialActivePurchases,
                forceServerErrors = true
            ) {
                Purchases.sharedInstance.getCustomerInfoWith(
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                    onError = {
                        latch.countDown()
                        fail("Expected success but got error: $it")
                    },
                    onSuccess = { receivedCustomerInfo ->
                        assertThat(receivedCustomerInfo.entitlements.active.keys).containsExactly(
                            "pro_cat"
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
            setupTest(
                buildSharedPreferencesMap(shouldIncludeCustomerInfo = true),
                initialActivePurchases,
                forceServerErrors = true
            ) {
                Purchases.sharedInstance.getCustomerInfoWith(
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                    onError = {
                        latch.countDown()
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
        val storeProduct = StoreProductFactory.createGoogleStoreProduct(
            productId = testProductId,
            basePlanId = testBasePlanId,
            subscriptionOptionsList = listOf(
                StoreProductFactory.createGoogleSubscriptionOption(
                    productId = testProductId,
                    basePlanId = testBasePlanId,
                )
            )
        )

        ensureBlockFinishes { latch ->
            setupTest(
                buildSharedPreferencesMap(shouldIncludeCustomerInfo = true),
                initialActivePurchases = emptyMap(),
                forceServerErrors = true
            ) { activity ->
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
                        assertThat(customerInfo.entitlements.active.keys).containsExactly(
                            "pro_cat"
                        )
                        assertThat(receivedCustomerInfos).hasSize(2)
                        assertThat(receivedCustomerInfos.first().entitlements.active).isEmpty()
                        assertThat(receivedCustomerInfos.last().entitlements.active.keys).containsExactly("pro_cat")
                        latch.countDown()
                    }
                )
            }
        }
    }

    // region helpers

    private fun buildSharedPreferencesMap(
        shouldIncludeProductEntitlementMapping: Boolean = true,
        shouldIncludeCustomerInfo: Boolean = false
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (shouldIncludeProductEntitlementMapping) {
            result += mapOf(
                productEntitlementMappingCacheKey to productEntitlementMappingMockResponse
            )
        }
        if (shouldIncludeCustomerInfo) {
            result += mapOf(
                "$cacheKeyPrefix.new" to testUserId,
                "$cacheKeyPrefix.$testUserId" to customerInfoCacheString,
            )
        }
        return result
    }

    // endregion helpers
}
