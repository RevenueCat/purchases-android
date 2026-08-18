package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.DangerousSettings

@Suppress("unused", "UNUSED_VARIABLE")
private class DangerousSettingsAPI {
    fun check(dangerousSettings: DangerousSettings) {
        val autoSync: Boolean = dangerousSettings.autoSyncPurchases
    }
}
