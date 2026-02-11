package com.revenuecat.purchases

import android.net.Uri
import com.revenuecat.purchases.common.responses.SubscriptionInfoResponse
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.DateHelper
import com.revenuecat.purchases.utils.EntitlementInfoHelper
import java.util.Date
import java.util.Locale

/**
 * Subscription purchases of the Customer.
 */
@SuppressWarnings("LongParameterList")
public class SubscriptionInfo(
    /**
     * The product identifier.
     */
    public val productIdentifier: String,
    /**
     * Date when the last subscription period started.
     */
    public val purchaseDate: Date,
    /**
     * Date when this subscription first started. This property does not update with renewals.
     * This property also does not update for product changes within a subscription group or
     * re-subscriptions by lapsed subscribers.
     */
    public val originalPurchaseDate: Date?,
    /**
     * Date when the subscription expires/expired
     */
    public val expiresDate: Date?,
    /**
     * Store where the subscription was purchased.
     */
    public val store: Store,
    /**
     * Date when RevenueCat detected that auto-renewal was turned off for this subscription.
     * Note the subscription may still be active, check the ``expiresDate`` attribute.
     */
    public val unsubscribeDetectedAt: Date?,
    /**
     * Whether or not the purchase was made in sandbox mode.
     */
    public val isSandbox: Boolean,
    /**
     * Date when RevenueCat detected any billing issues with this subscription.
     * If and when the billing issue gets resolved, this field is set to nil.
     * Note the subscription may still be active, check the ``expiresDate`` attribute.
     */
    public val billingIssuesDetectedAt: Date?,
    /**
     * Date when any grace period for this subscription expires/expired.
     * nil if the customer has never been in a grace period.
     */
    public val gracePeriodExpiresDate: Date?,
    /**
     * How the Customer received access to this subscription:
     * - [OwnershipType.PURCHASED]: The customer bought the subscription.
     * - [OwnershipType.FAMILY_SHARED]: The Customer has access to the product via their family.
     */
    public val ownershipType: OwnershipType = OwnershipType.UNKNOWN,
    /**
     * Type of the current subscription period:
     * - [PeriodType.NORMAL]: The product is in a normal period (default)
     * - [PeriodType.TRIAL]: The product is in a free trial period
     * - [PeriodType.INTRO]: The product is in an introductory pricing period
     * - [PeriodType.PREPAID]: The product is in a prepaid pricing period
     */
    public val periodType: PeriodType,
    /**
     * Date when RevenueCat detected a refund of this subscription.
     */
    public val refundedAt: Date?,
    /**
     * The transaction id in the store of the subscription.
     */
    public val storeTransactionId: String?,
    /**
     * Date when the subscription will auto-resume. This property is only applicable for Google Play subscriptions
     * and will only have a value when the subscription is currently paused.
     */
    public val autoResumeDate: Date?,
    /**
     * The display name of the subscription as configured in the RevenueCat dashboard.
     */
    public val displayName: String?,
    /**
     * Paid price for the subscription.
     */
    public val price: Price?,
    /**
     * The identifier of the product plan.
     */
    public val productPlanIdentifier: String?,
    /**
     * URL to manage this subscription.
     */
    public val managementURL: Uri?,
    /**
     * The date the request was made.
     */
    private val requestDate: Date,
) {

    /**
     * Whether the subscription is currently active.
     */
    public val isActive: Boolean = DateHelper.isDateActive(expiresDate, requestDate).isActive

    /**
     * Whether the subscription will renew at the next billing period.
     */
    public val willRenew: Boolean = EntitlementInfoHelper.getWillRenew(
        store,
        expiresDate,
        unsubscribeDetectedAt,
        billingIssuesDetectedAt,
        periodType,
    )

    @Deprecated(
        message = """
            Use the constructor with all fields instead. This constructor is missing the new fields: 
            autoResumeDate, displayName, price, and productPlanIdentifier
            """,
        replaceWith = ReplaceWith(
            "SubscriptionInfo(productIdentifier, purchaseDate, originalPurchaseDate, expiresDate, store, " +
                "isSandbox, unsubscribeDetectedAt, billingIssuesDetectedAt, gracePeriodExpiresDate, ownershipType, " +
                "periodType, refundedAt, storeTransactionId, autoResumeDate, displayName, price, " +
                "productPlanIdentifier, requestDate)",
        ),
    )
    constructor(
        productIdentifier: String,
        purchaseDate: Date,
        originalPurchaseDate: Date?,
        expiresDate: Date?,
        store: Store,
        unsubscribeDetectedAt: Date?,
        isSandbox: Boolean,
        billingIssuesDetectedAt: Date?,
        gracePeriodExpiresDate: Date?,
        ownershipType: OwnershipType = OwnershipType.UNKNOWN,
        periodType: PeriodType,
        refundedAt: Date?,
        storeTransactionId: String?,
        requestDate: Date,
    ) : this(
        productIdentifier = productIdentifier,
        purchaseDate = purchaseDate,
        originalPurchaseDate = originalPurchaseDate,
        expiresDate = expiresDate,
        store = store,
        isSandbox = isSandbox,
        unsubscribeDetectedAt = unsubscribeDetectedAt,
        billingIssuesDetectedAt = billingIssuesDetectedAt,
        gracePeriodExpiresDate = gracePeriodExpiresDate,
        ownershipType = ownershipType,
        periodType = periodType,
        refundedAt = refundedAt,
        storeTransactionId = storeTransactionId,
        autoResumeDate = null,
        displayName = null,
        price = null,
        productPlanIdentifier = null,
        managementURL = null,
        requestDate = requestDate,
    )

    @Deprecated(
        message = """
            Use the constructor that has managementURL
            """,
        replaceWith = ReplaceWith(
            "SubscriptionInfo(productIdentifier, purchaseDate, originalPurchaseDate, expiresDate, store, " +
                "isSandbox, unsubscribeDetectedAt, billingIssuesDetectedAt, gracePeriodExpiresDate, ownershipType, " +
                "periodType, refundedAt, storeTransactionId, autoResumeDate, displayName, price, " +
                "productPlanIdentifier, managementURL, requestDate)",
        ),
    )
    constructor(
        productIdentifier: String,
        purchaseDate: Date,
        originalPurchaseDate: Date?,
        expiresDate: Date?,
        store: Store,
        unsubscribeDetectedAt: Date?,
        isSandbox: Boolean,
        billingIssuesDetectedAt: Date?,
        gracePeriodExpiresDate: Date?,
        ownershipType: OwnershipType = OwnershipType.UNKNOWN,
        periodType: PeriodType,
        refundedAt: Date?,
        storeTransactionId: String?,
        autoResumeDate: Date?,
        displayName: String?,
        price: Price?,
        productPlanIdentifier: String?,
        requestDate: Date,
    ) : this(
        productIdentifier = productIdentifier,
        purchaseDate = purchaseDate,
        originalPurchaseDate = originalPurchaseDate,
        expiresDate = expiresDate,
        store = store,
        isSandbox = isSandbox,
        unsubscribeDetectedAt = unsubscribeDetectedAt,
        billingIssuesDetectedAt = billingIssuesDetectedAt,
        gracePeriodExpiresDate = gracePeriodExpiresDate,
        ownershipType = ownershipType,
        periodType = periodType,
        refundedAt = refundedAt,
        storeTransactionId = storeTransactionId,
        autoResumeDate = autoResumeDate,
        displayName = displayName,
        price = price,
        productPlanIdentifier = productPlanIdentifier,
        managementURL = null,
        requestDate = requestDate,
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
                willRenew: $willRenew,
                price: $price,
                productPlanIdentifier: $productPlanIdentifier,
                displayName: $displayName,
                autoResumeDate: $autoResumeDate,
                managementURL: $managementURL,
                requestDate: $requestDate,
                productIdentifier: $productIdentifier
            }
        """.trimIndent()
    }

    internal constructor(
        productIdentifier: String,
        requestDate: Date,
        response: SubscriptionInfoResponse,
        locale: Locale = Locale.getDefault(),
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
        autoResumeDate = response.autoResumeDate,
        displayName = response.displayName,
        price = response.price?.toPrice(locale),
        productPlanIdentifier = response.productPlanIdentifier,
        managementURL = response.managementURL?.let { Uri.parse(it) },
    )
}
