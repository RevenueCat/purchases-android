package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI

@Suppress("unused", "UNUSED_VARIABLE")
private class DangerousSettingsAPI {
    fun check(dangerousSettings: DangerousSettings) {
        val autoSync: Boolean = dangerousSettings.autoSyncPurchases
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun checkInternalRevenueCatAPIs() {
        val forWorkflows: DangerousSettings = DangerousSettings.forWorkflows()
        val forWorkflowsNoSync: DangerousSettings = DangerousSettings.forWorkflows(autoSyncPurchases = false)
        val useWorkflows: Boolean = forWorkflows.useWorkflows
    }
}
