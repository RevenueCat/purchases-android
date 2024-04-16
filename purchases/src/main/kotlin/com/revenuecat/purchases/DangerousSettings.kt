package com.revenuecat.purchases

/**
 * Only use a Dangerous Setting if suggested by RevenueCat support team.
 */
data class DangerousSettings internal constructor(
    /**
     * Disable or enable syncing purchases automatically. If this is disabled, RevenueCat will not sync any purchase
     * automatically, and you will have to call syncPurchases whenever a new purchase is completed in order to send it
     * to the RevenueCat's backend. Auto syncing of purchases is enabled by default.
     */
    val autoSyncPurchases: Boolean = true,

    /**
     * Do not consume IAP (In app products) after a successful purchase. This can be useful when using IAP as lifetime
     * products and you don't want them to disappear from the Google query method. DO NOT use this if you allow
     * purchasing the same IAP multiple times, like for currency/items. This also means that the purchase might be
     * synced with multiple users in different devices, unless they are logged in with the same user ID.
     * This does not affect Amazon. This is disabled by default.
     */
    val doNotConsumeIAP: Boolean = false,

    internal val customEntitlementComputation: Boolean = false,
) {
    constructor(autoSyncPurchases: Boolean = true, doNotConsumeIAP: Boolean = false) :
        this(autoSyncPurchases, doNotConsumeIAP, false)
}
