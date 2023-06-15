package com.revenuecat.purchases.trustedentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.purchaseWith
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class TrustedEntitlementsInformationalFailureIntegrationTest : BasePurchasesIntegrationTest() {

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest(
                entitlementVerificationMode = EntitlementVerificationMode.INFORMATIONAL,
                forceSigningErrors = true,
            ) {
                latch.countDown()
            }
        }
    }

    @Test
    fun initialCustomerInfoFailsToVerify() {
        var receivedCustomerInfo: CustomerInfo? = null
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { fail("should be success. Error: ${it.message}") },
                onSuccess = {
                    receivedCustomerInfo = it
                    latch.countDown()
                },
            )
        }

        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.entitlements?.verification).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun canPurchaseProductFailingToVerifyStatus() {
        val storeProduct = StoreProductFactory.createGoogleStoreProduct()
        val storeTransaction = StoreTransactionFactory.createStoreTransaction()
        mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))

        var receivedCustomerInfo: CustomerInfo? = null
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ -> fail("Purchase should be successful. Error: ${error.message}") },
                onSuccess = { _, customerInfo ->
                    receivedCustomerInfo = customerInfo
                    latch.countDown()
                },
            )
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(storeTransaction))
        }

        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.entitlements?.verification).isEqualTo(VerificationResult.FAILED)
        assertThat(receivedCustomerInfo?.entitlements?.all).isNotEmpty
        assertThat(
            receivedCustomerInfo?.entitlements?.all?.values?.first()?.verification,
        ).isEqualTo(VerificationResult.FAILED)
    }
}
