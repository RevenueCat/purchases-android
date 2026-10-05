package com.revenuecat.purchases.integration.offlineentitlements

import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.common.localrules.CustomerInfoDimensionProvider
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import java.util.Date

abstract class BaseOfflineEntitlementsIntegrationTest : BasePurchasesIntegrationTest() {

    private val initialActiveTransaction get() = StoreTransactionFactory.createStoreTransaction(
        skus = listOf(Constants.productIdToPurchase),
        purchaseToken = Constants.googlePurchaseToken,
    )
    protected val initialActivePurchases get() = mapOf(
        initialActiveTransaction.purchaseToken.sha1() to initialActiveTransaction,
    )

    // Hack until we get a running token for production API tests. After that, we can just use "entitlementsToVerify"
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

    protected fun localRulesPurchases(customerInfo: CustomerInfo): Map<String, Map<String, RulesDimensionValue>> {
        val provider = CustomerInfoDimensionProvider(
            currentAppUserId = { customerInfo.originalAppUserId },
            customerInfo = { customerInfo },
        )
        val dimensions = runBlocking { provider.dimensions(Date()) }
        val purchases = dimensions[CustomerInfoDimensionProvider.KEY_PURCHASES] as RulesDimensionValue.ObjectListValue
        return purchases.value.associateBy { purchase ->
            (purchase[CustomerInfoDimensionProvider.KEY_PRODUCT_IDENTIFIER] as RulesDimensionValue.StringValue).value
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
