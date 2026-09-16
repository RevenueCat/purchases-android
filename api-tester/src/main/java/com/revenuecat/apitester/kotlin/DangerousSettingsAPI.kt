package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.DangerousSettings

@Suppress("unused", "UNUSED_VARIABLE")
private class DangerousSettingsAPI {
    fun check(dangerousSettings: DangerousSettings) {
        val autoSync: Boolean = dangerousSettings.autoSyncPurchases
        val disableRequiredSignatureVerifications: Boolean =
            dangerousSettings.disableRequiredSignatureVerifications
        dangerousSettings.forceAllowTestStoreInReleaseBuilds()
    }

    fun checkConstructors() {
        val defaults = DangerousSettings()
        val autoSync = DangerousSettings(autoSyncPurchases = false)
        val skipping = DangerousSettings(
            autoSyncPurchases = true,
            disableRequiredSignatureVerifications = true,
        )
    }
}
