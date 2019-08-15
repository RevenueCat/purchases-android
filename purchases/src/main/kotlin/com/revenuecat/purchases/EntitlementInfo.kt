package com.revenuecat.purchases

import java.util.Date

/**
 * @property identifier The entitlement identifier configured in the RevenueCat dashboard.
 * @property isActive True if the user has access to this entitlement.
 * @property willRenew True if the underlying subscription is set to renew at the end of the billing
 * period (expirationDate). Will always be True if entitlement is for lifetime access.
 * @property periodType The last period type this entitlement was in Either: NORMAL, INTRO or TRIAL.
 * @property latestPurchaseDate The latest purchase or renewal date for the entitlement.
 * @property originalPurchaseDate The first date this entitlement was purchased.
 * @property expirationDate The expiration date for the entitlement, can be `null` for lifetime
 * access. If the `periodType` is `TRIAL`, this is the trial expiration date.
 * @property store The store where this entitlement was unlocked from. Either: APP_STORE,
 * MAC_APP_STORE, PLAY_STORE, STRIPE, PROMOTIONAL or UNKNOWN_STORE.
 * @property productIdentifier The product identifier that unlocked this entitlement.
 * @property isSandbox False if this entitlement is unlocked via a production purchase.
 * @property unsubscribeDetectedAt The date an unsubscribe was detected. Can be `null`.
 * Note: Entitlement may still be active even if user has unsubscribed. Check the `isActive` property.
 * @property billingIssueDetectedAt The date a billing issue was detected. Can be `null` if there is
 * no billing issue or an issue has been resolved. Note: Entitlement may still be active even if
 * there is a billing issue. Check the `isActive` property.
 */
class EntitlementInfo(
    val identifier: String,
    val isActive: Boolean,
    val willRenew: Boolean,
    val periodType: PeriodType,
    val latestPurchaseDate: Date,
    val originalPurchaseDate: Date,
    val expirationDate: Date?,
    val store: Store,
    val productIdentifier: String,
    val isSandbox: Boolean,
    val unsubscribeDetectedAt: Date?,
    val billingIssueDetectedAt: Date?
)

/**
 * Enum of supported stores
 */
enum class Store {
    /**
     * For entitlements granted via Apple App Store.
     */
    APP_STORE,
    /**
     * For entitlements granted via Apple Mac App Store.
     */
    MAC_APP_STORE,
    /**
     * For entitlements granted via Google Play Store.
     */
    PLAY_STORE,
    /**
     * For entitlements granted via Stripe.
     */
    STRIPE,
    /**
     * For entitlements granted via a promo in RevenueCat.
     */
    PROMOTIONAL,
    /**
     * For entitlements granted via an unknown store.
     */
    UNKNOWN_STORE,
}


/**
 * Enum of supported period types for an entitlement.
 */
enum class PeriodType {
    /**
     * If the entitlement is not under an introductory or trial period.
     */
    NORMAL,
    /**
     * If the entitlement is under a introductory price period.
     */
    INTRO,
    /**
     * If the entitlement is under a trial period.
     */
    TRIAL,
}
