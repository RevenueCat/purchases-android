@file:OptIn(InviteOnlyCheckpointsAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedEntitlement

@Suppress("unused", "UNUSED_VARIABLE")
private class FlowResultAPI {

    fun checkCheckpointResult(result: CheckpointResult) {
        val flow: FlowResult? = result.flow
        val customer: CustomerInfo = result.customer
    }

    fun checkResult(result: FlowResult) {
        val obtainedEntitlements: Set<ObtainedEntitlement> = result.obtainedEntitlements
    }

    fun checkObtainedEntitlement(obtained: ObtainedEntitlement) {
        val entitlementInfo: EntitlementInfo = obtained.entitlementInfo
    }
}
