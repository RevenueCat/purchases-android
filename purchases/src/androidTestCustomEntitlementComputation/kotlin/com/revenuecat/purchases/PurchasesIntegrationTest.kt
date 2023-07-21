package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleStoreProduct
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class PurchasesIntegrationTest : BasePurchasesIntegrationTest() {

    private val userID = "testAppUserID"

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest(appUserID = userID) {
                latch.countDown()
            }
        }
    }

    // region tests

    @Test
    fun sdkCanBeConfigured() {
        onActivityReady {
            assertThat(Purchases.sharedInstance.appUserID).isEqualTo(userID)
        }
    }

    @Test
    fun settingCustomerInfoListenerDoesNotTriggerUpdate() {
        var listenerCalled = false
        onActivityReady {
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
                listenerCalled = true
            }
        }
        assertThat(listenerCalled).isFalse
    }

    @Test
    fun canFetchOfferings() = runTestActivityLifecycleScope {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()
        assertThat(offerings.current).isNotNull
        assertThat(offerings.current?.availablePackages?.size).isEqualTo(1)
        assertThat(offerings.current?.availablePackages?.get(0)?.product?.sku)
            .isEqualTo(Constants.productIdToPurchase)

        assertThat(offerings.current?.metadata).isNotNull
        assertThat(offerings.current?.metadata?.get("dontdeletethis")).isEqualTo("useforintegrationtesting")
    }

    @Test
    fun offeringsArePersistedAndUsedOnServerErrors() = runTestActivityLifecycleScope {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()

        assertThat(offerings.current).isNotNull
        assertThat(offerings.current?.availablePackages?.size).isEqualTo(1)
        assertThat(offerings.current?.availablePackages?.get(0)?.product?.sku)
            .isEqualTo(Constants.productIdToPurchase)

        simulateSdkRestart(activity, forceServerErrors = true)

        val newOfferings = Purchases.sharedInstance.awaitOfferings()

        assertThat(newOfferings.current).isNotNull
        assertThat(newOfferings.current?.availablePackages?.size).isEqualTo(1)
        assertThat(newOfferings.current?.availablePackages?.get(0)?.product?.sku)
            .isEqualTo(Constants.productIdToPurchase)
    }

    @Test
    fun canPurchaseSubsProduct() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()
        val storeTransaction = StoreTransactionFactory.createStoreTransaction()
        mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ -> fail("Purchase should be successful. Error: ${error.message}") },
                onSuccess = { transaction, customerInfo ->
                    assertThat(transaction).isEqualTo(storeTransaction)
                    verifyCustomerInfoHasPurchased(customerInfo)
                    latch.countDown()
                },
            )
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(storeTransaction))
        }

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                userID,
                match {
                    it is GooglePurchasingData.Subscription &&
                        storeProduct is GoogleStoreProduct &&
                        it.productId == storeProduct.productId &&
                        it.optionId == storeProduct.basePlanId
                },
                replaceProductInfo = null,
                presentedOfferingIdentifier = null,
                isPersonalizedPrice = null,
            )
        }
    }

    @Test
    fun purchaseTriggersCustomerInfoListener() {
        val listenerCalledValues = mutableListOf<CustomerInfo>()

        val storeProduct = StoreProductFactory.createGoogleStoreProduct()
        val storeTransaction = StoreTransactionFactory.createStoreTransaction()
        mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
                listenerCalledValues.add(it)
            }
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ -> fail("Purchase should be successful. Error: ${error.message}") },
                onSuccess = { _, _ -> latch.countDown() },
            )
            assertThat(listenerCalledValues.size).isEqualTo(0)
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(storeTransaction))
        }
        assertThat(listenerCalledValues.size).isEqualTo(1)
        verifyCustomerInfoHasPurchased(listenerCalledValues.first())
    }

    @Test
    fun switchUserWorks() {
        onActivityReady {
            assertThat(Purchases.sharedInstance.appUserID).isEqualTo(userID)
            val anotherUserID = "anotherTestUserID"
            Purchases.sharedInstance.switchUser(anotherUserID)
            assertThat(Purchases.sharedInstance.appUserID).isEqualTo(anotherUserID)
        }
    }

    // endregion

    // region private

    private fun verifyCustomerInfoHasPurchased(customerInfo: CustomerInfo) {
        assertThat(customerInfo.allPurchaseDatesByProduct.size).isEqualTo(1)
        val productId = customerInfo.allPurchaseDatesByProduct.keys.first()
        val expectedProductId = "${Constants.productIdToPurchase}:${Constants.basePlanIdToPurchase}"
        assertThat(productId).isEqualTo(expectedProductId)
        assertThat(customerInfo.entitlements.active.size).isEqualTo(entitlementsToVerify.size)
        entitlementsToVerify.onEach { entitlementId ->
            assertThat(customerInfo.entitlements.active[entitlementId]).isNotNull
        }
        assertThat(customerInfo.originalAppUserId).isEqualTo(userID)
    }

    // endregion
}
