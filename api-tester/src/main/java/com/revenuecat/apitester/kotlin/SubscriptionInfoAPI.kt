package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.models.Price
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class SubscriptionInfoAPI {
    @SuppressWarnings("LongMethod")
    fun check(subscriptionInfo: SubscriptionInfo) {
        with(subscriptionInfo) {
            val productIdentifier: String = productIdentifier
            val purchaseDate: Date = purchaseDate
            val originalPurchaseDate: Date? = originalPurchaseDate
            val expiresDate: Date? = expiresDate
            val store: Store = store
            val unsubscribeDetectedAt: Date? = unsubscribeDetectedAt
            val isSandbox: Boolean = isSandbox
            val billingIssuesDetectedAt: Date? = billingIssuesDetectedAt
            val gracePeriodExpiresDate: Date? = gracePeriodExpiresDate
            val ownershipType: OwnershipType = ownershipType
            val periodType: PeriodType = periodType
            val refundedAt: Date? = refundedAt
            val storeTransactionId: String? = storeTransactionId
            val autoResumeDate: Date? = autoResumeDate
            val displayName: String? = displayName
            val price: Price? = price
            val productPlanIdentifier: String? = productPlanIdentifier
            val isActive: Boolean = isActive
            val willRenew: Boolean = willRenew
        }

        // Test deprecated constructor
        @Suppress("DEPRECATION")
        val deprecatedSubscriptionInfo = SubscriptionInfo(
            productIdentifier = "product",
            purchaseDate = Date(),
            originalPurchaseDate = Date(),
            expiresDate = Date(),
            store = Store.PLAY_STORE,
            isSandbox = false,
            unsubscribeDetectedAt = Date(),
            billingIssuesDetectedAt = Date(),
            gracePeriodExpiresDate = Date(),
            ownershipType = OwnershipType.PURCHASED,
            periodType = PeriodType.NORMAL,
            refundedAt = Date(),
            storeTransactionId = "store_id",
            requestDate = Date(),
        )

        // Test new constructor
        val newSubscriptionInfo = SubscriptionInfo(
            productIdentifier = "product",
            purchaseDate = Date(),
            originalPurchaseDate = Date(),
            expiresDate = Date(),
            store = Store.PLAY_STORE,
            isSandbox = false,
            unsubscribeDetectedAt = Date(),
            billingIssuesDetectedAt = Date(),
            gracePeriodExpiresDate = Date(),
            ownershipType = OwnershipType.PURCHASED,
            periodType = PeriodType.NORMAL,
            refundedAt = Date(),
            storeTransactionId = "store_id",
            autoResumeDate = null,
            displayName = "Display Name",
            price = Price("", 0, "USD"),
            productPlanIdentifier = "plan_id",
            requestDate = Date(),
        )
    }
}
