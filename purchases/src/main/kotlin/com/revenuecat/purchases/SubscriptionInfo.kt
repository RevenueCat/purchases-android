package com.revenuecat.purchases

import com.revenuecat.purchases.common.responses.SubscriptionInfoResponse
import com.revenuecat.purchases.utils.DateHelper
import com.revenuecat.purchases.utils.EntitlementInfoHelper
import java.util.Date

/**
 * Subscription purchases of the Customer.
 */
@SuppressWarnings("LongParameterList")
class SubscriptionInfo(
    /**
     * The product identifier.
     */
    val productIdentifier: String,
    /**
     * Date when the last subscription period started.
     */
    val purchaseDate: Date,
    /**
     * Date when this subscription first started. This property does not update with renewals.
     * This property also does not update for product changes within a subscription group or
     * re-subscriptions by lapsed subscribers.
     */
    val originalPurchaseDate: Date?,
    /**
     * Date when the subscription expires/expired
     */
    val expiresDate: Date?,
    /**
     * Store where the subscription was purchased.
     */
    val store: Store,
    /**
     * Date when RevenueCat detected that auto-renewal was turned off for this subscription.
     * Note the subscription may still be active, check the ``expiresDate`` attribute.
     */
    val unsubscribeDetectedAt: Date?,
    /**
     * Whether or not the purchase was made in sandbox mode.
     */
    val isSandbox: Boolean,
    /**
     * Date when RevenueCat detected any billing issues with this subscription.
     * If and when the billing issue gets resolved, this field is set to nil.
     * Note the subscription may still be active, check the ``expiresDate`` attribute.
     */
    val billingIssuesDetectedAt: Date?,
    /**
     * Date when any grace period for this subscription expires/expired.
     * nil if the customer has never been in a grace period.
     */
    val gracePeriodExpiresDate: Date?,
    /**
     * How the Customer received access to this subscription:
     * - [OwnershipType.PURCHASED]: The customer bought the subscription.
     * - [OwnershipType.FAMILY_SHARED]: The Customer has access to the product via their family.
     */
    val ownershipType: OwnershipType = OwnershipType.UNKNOWN,
    /**
     * Type of the current subscription period:
     * - [PeriodType.NORMAL]: The product is in a normal period (default)
     * - [PeriodType.TRIAL]: The product is in a free trial period
     * - [PeriodType.INTRO]: The product is in an introductory pricing period
     */
    val periodType: PeriodType,
    /**
     * Date when RevenueCat detected a refund of this subscription.
     */
    val refundedAt: Date?,
    /**
     * The transaction id in the store of the subscription.
     */
    val storeTransactionId: String?,
    /**
     * The date the request was made.
     */
    private val requestDate: Date,
) {

    /**
     * Whether the subscription is currently active.
     */
    val isActive: Boolean = DateHelper.isDateActive(expiresDate, requestDate).isActive

    /**
     * Whether the subscription will renew at the next billing period.
     */
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
