package com.revenuecat.purchases.trustedentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.forceSigningErrors
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.purchaseWith
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class TrustedEntitlementsInformationalModeIntegrationTest : BasePurchasesIntegrationTest() {

    @Before
    public fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest(entitlementVerificationMode = EntitlementVerificationMode.INFORMATIONAL) {
                latch.countDown()
            }
        }
    }

    @Test
    public fun initialCustomerInfoIsVerified() = runTestActivityLifecycleScope {
        confirmProductionBackendEnvironment()

        val receivedCustomerInfo = Purchases.sharedInstance.awaitCustomerInfo()

        assertThat(receivedCustomerInfo.entitlements.verification).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    public fun canPurchaseProductWithVerificationStatusCorrect() {
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
        assertThat(receivedCustomerInfo?.entitlements?.verification).isEqualTo(VerificationResult.VERIFIED)
        assertThat(receivedCustomerInfo?.entitlements?.all).isNotEmpty
        assertThat(
            receivedCustomerInfo?.entitlements?.all?.values?.first()?.verification,
        ).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    public fun verificationChangesAfterSuccessIsNotified() = runTestActivityLifecycleScope {
        confirmProductionBackendEnvironment()

        val receivedCustomerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        assertThat(receivedCustomerInfo.entitlements.verification).isEqualTo(VerificationResult.VERIFIED)
        Purchases.sharedInstance.forceSigningErrors = true

        val receivedCustomerInfo2 = Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)

        assertThat(receivedCustomerInfo2.entitlements.verification).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    public fun initialCustomerInfoFailsToVerify() = runTestActivityLifecycleScope {
        confirmProductionBackendEnvironment()

        Purchases.sharedInstance.forceSigningErrors = true
        val receivedCustomerInfo = Purchases.sharedInstance.awaitCustomerInfo()

        assertThat(receivedCustomerInfo.entitlements.verification).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    public fun canPurchaseProductFailingToVerifyStatus() {
        Purchases.sharedInstance.forceSigningErrors = true

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
