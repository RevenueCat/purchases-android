package com.revenuecat.purchases

@Suppress("MaxLineLength")
internal enum class LockedFeature(val isLocked: Boolean, val lockedMessage: String) {
    ObserverMode(true, "Observer mode is not supported in this version. " +
        "Please disable it or use the latest stable version to use this."),
    AmazonStore(false, "Amazon store is not supported in this version. " +
        "Please use the Google store or use the latest stable version to use this."),
    SyncPurchases(true, "Syncing purchases is not supported in this version. " +
        "Please use the latest stable version to use this.")
}

internal class FeatureNotSupportedException(lockedFeature: LockedFeature) : Exception(lockedFeature.lockedMessage)
