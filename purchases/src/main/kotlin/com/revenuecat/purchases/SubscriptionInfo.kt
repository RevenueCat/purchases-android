package com.revenuecat.purchases

import com.revenuecat.purchases.common.responses.SubscriptionInfoResponse
import com.revenuecat.purchases.utils.DateHelper
import com.revenuecat.purchases.utils.EntitlementInfoHelper
import java.util.Date

@SuppressWarnings("LongParameterList")
class SubscriptionInfo(
    val productIdentifier: String,
    val purchaseDate: Date,
    val originalPurchaseDate: Date?,
    val expiresDate: Date?,
    val store: Store,
    val unsubscribeDetectedAt: Date?,
    val isSandbox: Boolean,
    val billingIssuesDetectedAt: Date?,
    val gracePeriodExpiresDate: Date?,
    val ownershipType: OwnershipType = OwnershipType.UNKNOWN,
    val periodType: PeriodType,
    val refundedAt: Date?,
    val storeTransactionId: String?,
    val requestDate: Date,
) {

    val isActive: Boolean = DateHelper.isDateActive(expiresDate, requestDate).isActive

    val willRenew: Boolean = EntitlementInfoHelper.getWillRenew(
        store,
        expiresDate,
        unsubscribeDetectedAt,
        billingIssuesDetectedAt,
    )

    override fun toString(): String {
        return """
            SubscriptionInfo {
                purchaseDate: $purchaseDate,
                originalPurchaseDate: $originalPurchaseDate,
                expiresDate: $expiresDate,
                store: $store,
                isSandbox: $isSandbox,
                unsubscribeDetectedAt: $unsubscribeDetectedAt,
                billingIssuesDetectedAt: $billingIssuesDetectedAt,
                gracePeriodExpiresDate: $gracePeriodExpiresDate,
                ownershipType: $ownershipType,
                periodType: $periodType,
                refundedAt: $refundedAt,
                storeTransactionId: $storeTransactionId,
                isActive: $isActive,
                willRenew: $willRenew
            }
        """.trimIndent()
    }

    internal constructor(
        productIdentifier: String,
        requestDate: Date,
        response: SubscriptionInfoResponse,
    ) : this(
        productIdentifier = productIdentifier,
        requestDate = requestDate,
        purchaseDate = response.purchaseDate,
        originalPurchaseDate = response.originalPurchaseDate,
        expiresDate = response.expiresDate,
        store = response.store,
        isSandbox = response.isSandbox,
        unsubscribeDetectedAt = response.unsubscribeDetectedAt,
        billingIssuesDetectedAt = response.billingIssuesDetectedAt,
        gracePeriodExpiresDate = response.gracePeriodExpiresDate,
        ownershipType = response.ownershipType,
        periodType = response.periodType,
        refundedAt = response.refundedAt,
        storeTransactionId = response.storeTransactionId,
    )
}
