package com.revenuecat.purchases

class EntitlementInfo(

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
