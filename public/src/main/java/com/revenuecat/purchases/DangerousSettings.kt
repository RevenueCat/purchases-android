package com.revenuecat.purchases

/**
 * Only use a Dangerous Setting if suggested by RevenueCat support team.
 */
data class DangerousSettings(
    /**
     * Disable or enable syncing purchases automatically. If this is disabled, RevenueCat will not sync any purchase
     * automatically, and you will have to call syncPurchases whenever a new purchase is completed in order to send it
     * to the RevenueCat's backend. Auto syncing of purchases is enabled by default.
     */
    val autoSyncPurchases: Boolean = true,

    /**
     * Disable or enable offline entitlements. If this is disabled, RevenueCat will not try to provide entitlements in
     * the unlikely case of an outage in our servers. This will prevent entitlements from being available for your
     * users during an outage. This is enabled by default.
     */
    val offlineEntitlementsEnabled: Boolean = DEFAULT_OFFLINE_ENTITLEMENTS_ENABLED
) {
    companion object {
        private const val DEFAULT_OFFLINE_ENTITLEMENTS_ENABLED = true
    }

    constructor(autoSyncPurchases: Boolean) : this(autoSyncPurchases, DEFAULT_OFFLINE_ENTITLEMENTS_ENABLED)
}
