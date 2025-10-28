package com.revenuecat.purchases.fallbackurl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.purchaseWith
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchasesFallbackURLTest : BasePurchasesIntegrationTest() {

    override var forceServerErrorsStrategy: ForceServerErrorStrategy? = ForceServerErrorStrategy.failExceptFallbackUrls

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    @Test
    fun customerInfoCannotBeFetchedFromFallbackURLSoUsesAnOfflineCalculatedOne() {
        ensureBlockFinishes { latch -> waitForProductEntitlementMappingToUpdate { latch.countDown() } }

        val appUserID = Purchases.sharedInstance.appUserID

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getCustomerInfoWith({
                fail("Should be success but got error: $it")
            }) {
                assertThat(it.entitlements.active).isEmpty()
                assertThat(it.originalAppUserId).isEqualTo(appUserID)
                latch.countDown()
            }
        }
    }

    @Test
    fun offeringsCanBeFetchedFromFallbackURL() {
        mockBillingAbstract.mockQueryProductDetails()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getOfferingsWith({
                fail("should be success. Error: $it")
            }) {
                assertThat(it.all).isNotEmpty
                latch.countDown()
            }
        }
    }

    @Test
    fun productEntitlementMappingCanBeFetchedFromFallbackURL() {
        ensureBlockFinishes { latch ->
            waitForProductEntitlementMappingToUpdate { latch.countDown() }
        }
    }

    @Test
    fun canMakePurchasesFromFallbackURLUsingOfflineEntitlements() {
        performPurchase()
    }

    @Test
    fun postsPurchasePerformedOnFallbackURLWhenRecoveringToMainServer() {
        performPurchase()

        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = false)

        forceServerErrorsStrategy = null

        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = true)
    }

    @Test
    fun postsPurchasePerformedOnFallbackURLWhenRecoveringAfterRestartToMainServer() {
        performPurchase()

        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = false)

        simulateSdkRestart(activity, forceServerErrorsStrategy = null)

        verifyGetCustomerInfo(shouldHaveAcknowledgedPurchase = true)
    }

    // region Helpers

    private fun verifyGetCustomerInfo(
        shouldHaveAcknowledgedPurchase: Boolean,
    ) {
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { fail("Expected success, got error: $it") },
                onSuccess = { customerInfo ->
                    assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
                        entitlementsToVerify,
                    )
                    if (shouldHaveAcknowledgedPurchase) {
                        assertAcknowledgePurchaseDidHappen()
                    } else {
                        assertAcknowledgePurchaseDidNotHappen()
                    }

                    latch.countDown()
                },
            )
        }
    }

    private fun performPurchase(
        shouldHaveAcknowledgedPurchase: Boolean = false,
    ) {
        mockBillingAbstract.mockQueryProductDetails()

        var offerings: Offerings? = null
        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { fail("should be success. Error fetching offerings: $it") },
                onSuccess = {
                    offerings = it
                    latch.countDown()
                },
            )
        }
        ensureBlockFinishes { latch ->
            waitForProductEntitlementMappingToUpdate { latch.countDown() }
        }

        val packageToPurchase = offerings?.current?.availablePackages?.first()
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

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build(),
                onError = { purchasesError, userCancelled ->
                    fail(
                        "Should be success. Error purchasing: $purchasesError",
                    )
                },
                onSuccess = { transaction, customerInfo ->
                    assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
                        entitlementsToVerify,
                    )
                    if (shouldHaveAcknowledgedPurchase) {
                        assertAcknowledgePurchaseDidHappen()
                    } else {
                        assertAcknowledgePurchaseDidNotHappen()
                    }
                    latch.countDown()
                },
            )
        }
    }

    // endregion Helpers
}
