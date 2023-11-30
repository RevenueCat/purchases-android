package com.revenuecat.purchases.offlineentitlements

import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions
import kotlin.time.Duration

abstract class BaseOfflineEntitlementsIntegrationTest : BasePurchasesIntegrationTest() {
    private val initialActiveTransaction = StoreTransactionFactory.createStoreTransaction(
        skus = listOf(Constants.productIdToPurchase),
        purchaseToken = Constants.googlePurchaseToken,
    )
    protected val initialActivePurchases = mapOf(
        initialActiveTransaction.purchaseToken.sha1() to initialActiveTransaction,
    )

    // Hack until we get a running token for production API tests. After that, we can just use "entitlementsToVerify"
    private val expectedEntitlements = entitlementsToVerify.ifEmpty { listOf("pro_cat") }

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

    protected fun assertAcknowledgePurchaseDidNotHappen() {
        verify(exactly = 0) {
            mockBillingAbstract.consumeAndSave(any(), any(), initiationSource = any())
        }
    }

    protected fun assertAcknowledgePurchaseDidHappen(timeout: Duration = testTimeout) {
        verify(timeout = timeout.inWholeMilliseconds) {
            mockBillingAbstract.consumeAndSave(any(), any(), initiationSource = any())
        }
    }

    // endregion helpers
}
