package com.revenuecat.purchases.loadshedderonly

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoadShedderPurchaseIntegrationTest : BasePurchasesIntegrationTest() {

    companion object {
        private const val coinsProductId = "consumable.10_coins"
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        confirmRunningLoadShedderTests()

        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    @Test
    fun canFetchConsumableProducts() = runTest {
        val offerings = fetchConsumableProducts()

        assertThat(offerings.all.size).isEqualTo(1)
        assertThat(offerings.current).isNull()
        val coinsOffering = offerings.all["coins"]
        assertThat(coinsOffering).isNotNull
    }

    @Test
    fun canPurchaseConsumableProducts() = runTest {
        val offerings = fetchConsumableProducts()
        val coinsOffering = offerings.all["coins"]
        val packageToPurchase = coinsOffering?.getPackage("10.coins")
        assertThat(packageToPurchase).isNotNull
        val activeTransaction = StoreTransactionFactory.createStoreTransaction(
            skus = listOf(coinsProductId),
            purchaseToken = Constants.googlePurchaseToken,
            type = ProductType.INAPP,
            isAutoRenewing = null,
            subscriptionOptionId = null,
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
        val result = Purchases.sharedInstance.awaitPurchase(
            PurchaseParams.Builder(activity, packageToPurchase!!).build(),
        )
        assertThat(result.customerInfo.entitlements.all).isEmpty()
        assertThat(result.customerInfo.allPurchasedProductIds).contains(coinsProductId)
        assertThat(result.customerInfo.nonSubscriptionTransactions.size).isEqualTo(1)
        assertThat(result.customerInfo.nonSubscriptionTransactions[0]?.productIdentifier).isEqualTo(coinsProductId)
    }

    private suspend fun fetchConsumableProducts(): Offerings {
        mockBillingAbstract.mockQueryProductDetails(
            queryProductDetailsSubsReturn = emptyList(),
            queryProductDetailsInAppReturn = listOf(
                StoreProductFactory.createGoogleStoreProduct(
                    productId = coinsProductId,
                    basePlanId = null,
                    type = ProductType.INAPP,
                    name = "Coins",
                    title = "Coins (RevenueCat SDK Tester)",
                    period = null,
                    subscriptionOptionsList = null,
                ),
            ),
        )

        return Purchases.sharedInstance.awaitOfferings()
    }

    private fun confirmRunningLoadShedderTests() {
        assumeTrue(
            "LoadShedder integration tests are disabled. Set isRunningLoadShedderIntegrationTests to 'true' to run.",
            Constants.isRunningLoadShedderIntegrationTests == "true",
        )
    }
}
