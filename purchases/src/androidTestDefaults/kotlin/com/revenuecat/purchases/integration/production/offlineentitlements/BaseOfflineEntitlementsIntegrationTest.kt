package com.revenuecat.purchases.integration.production.offlineentitlements

import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import org.assertj.core.api.Assertions

abstract class BaseOfflineEntitlementsIntegrationTest : BasePurchasesIntegrationTest() {

    override val environmentConfig get() = Constants.production

    private val initialActiveTransaction get() = StoreTransactionFactory.createStoreTransaction(
        skus = listOf(Constants.productIdToPurchase),
        purchaseToken = Constants.googlePurchaseToken,
    )
    protected val initialActivePurchases get() = mapOf(
        initialActiveTransaction.purchaseToken.sha1() to initialActiveTransaction,
    )

    private val expectedEntitlements get() = entitlementsToVerify.ifEmpty { listOf("pro_cat") }

    // region helpers

    protected fun mockPurchaseResult(activePurchases: Map<String, StoreTransaction> = initialActivePurchases) {
        every {
            mockBillingAbstract.makePurchaseAsync(any(), any(), any(), any(), any(), any())
        } answers {
            mockActivePurchases(activePurchases)
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(activePurchases.values.toList())
        }
    }

    protected fun assertCustomerInfoDoesNotHavePurchaseData(customerInfo: CustomerInfo) {
        Assertions.assertThat(customerInfo.entitlements.active).isEmpty()
        Assertions.assertThat(customerInfo.activeSubscriptions).isEmpty()
    }

    protected fun assertCustomerInfoHasExpectedPurchaseData(customerInfo: CustomerInfo) {
        Assertions.assertThat(customerInfo.entitlements.active.keys).containsExactlyInAnyOrderElementsOf(
            expectedEntitlements,
        )
        Assertions.assertThat(customerInfo.activeSubscriptions).containsExactly(
            "${Constants.productIdToPurchase}:${Constants.basePlanIdToPurchase}",
        )
    }

    // endregion helpers
}
