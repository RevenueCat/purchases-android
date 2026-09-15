@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedEntitlement

@Suppress("unused", "UNUSED_VARIABLE")
private class FlowResultAPI {

    fun checkResult(result: FlowResult) {
        val obtainedEntitlements: Set<ObtainedEntitlement> = result.obtainedEntitlements
    }

    fun checkObtainedEntitlement(obtained: ObtainedEntitlement) {
        val entitlementInfo: EntitlementInfo = obtained.entitlementInfo
    }
}
