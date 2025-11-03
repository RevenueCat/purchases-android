package com.revenuecat.purchases.fallbackurl

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PurchasesFallbackURLTest : BasePurchasesIntegrationTest() {

    override var forceServerErrorsStrategy: ForceServerErrorStrategy? = ForceServerErrorStrategy.failExceptFallbackUrls

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    @Test
    fun customerInfoCannotBeFetchedFromFallbackURLSoUsesAnOfflineCalculatedOne() = runTest {
        waitForProductEntitlementMappingToUpdate()

        val appUserID = Purchases.sharedInstance.appUserID

        val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        assertThat(customerInfo.entitlements.active).isEmpty()
        assertThat(customerInfo.originalAppUserId).isEqualTo(appUserID)
        assertThat(customerInfo.entitlements.verification).isEqualTo(VerificationResult.VERIFIED_ON_DEVICE)
    }

    @Test
    fun offeringsCanBeFetchedFromFallbackURL() = runTest {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()
        assertThat(offerings.all).isNotEmpty
    }

    @Test
    fun productEntitlementMappingCanBeFetchedFromFallbackURL() = runTest {
        waitForProductEntitlementMappingToUpdate()
    }

    @Test
    fun canMakePurchasesFromFallbackURLUsingOfflineEntitlements() = runTest {
        performPurchase()
    }

    @Test
    fun postsPurchasePerformedOnFallbackURLWhenRecoveringToMainServer() = runTest {
        performPurchase()

        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = false)

        forceServerErrorsStrategy = null

        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = true)
    }

    @Test
    fun postsPurchasePerformedOnFallbackURLWhenRecoveringAfterRestartToMainServer() = runTest {
        val activePurchases = performPurchase()

        // Should have entitlements using offline entitlement
        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = false)

        // Restart and recover connectivity to main server
        simulateSdkRestart(
            activity,
            forceServerErrorsStrategy = ForceServerErrorStrategy.doNotFail,
            initialActivePurchases = activePurchases,
        )

        // Restore purchases since purchase won't be moved to new user when syncing unsynced purchases.
        Purchases.sharedInstance.awaitRestore()

        // Check that active purchases are synced.
        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = true)
    }

    // region Helpers

    private suspend fun verifyGetCustomerInfo(
        shouldHaveAcknowledgedPurchase: Boolean,
    ) {
        val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
        assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
            entitlementsToVerify,
        )
        if (shouldHaveAcknowledgedPurchase) {
            assertAcknowledgePurchaseDidHappen()
        } else {
            assertAcknowledgePurchaseDidNotHappen()
        }
    }

    private suspend fun performPurchase(
        shouldHaveAcknowledgedPurchase: Boolean = false,
    ): Map<String, StoreTransaction> {
        mockBillingAbstract.mockQueryProductDetails()

        val offerings = Purchases.sharedInstance.awaitOfferings()

        waitForProductEntitlementMappingToUpdate()

        val packageToPurchase = offerings.current?.availablePackages?.first()
            ?: fail("Couldn't get package to purchase")

        val activeTransaction = StoreTransactionFactory.createStoreTransaction(
            skus = listOf(Constants.productIdToPurchase),
            purchaseToken = Constants.googlePurchaseToken,
        )
        val activePurchases = mapOf(
            activeTransaction.purchaseToken.sha1() to activeTransaction,
        )
        every {
            mockBillingAbstract.makePurchaseAsync(any(), any(), any(), any(), any(), any())
        } answers {
            mockActivePurchases(activePurchases)
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(activePurchases.values.toList())
        }

        val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, packageToPurchase).build())
        assertThat(result.customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
            entitlementsToVerify,
        )
        if (shouldHaveAcknowledgedPurchase) {
            assertAcknowledgePurchaseDidHappen()
        } else {
            assertAcknowledgePurchaseDidNotHappen()
        }

        return activePurchases
    }

    // endregion Helpers
}
