@file:OptIn(InviteOnlyCheckpointsAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedEntitlement
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedProduct

@Suppress("unused", "UNUSED_VARIABLE")
private class FlowResultAPI {

    fun checkCheckpointResult(result: CheckpointResult) {
        val flow: FlowResult? = result.flow
        val customer: CustomerInfo = result.customer
        val obtainedEntitlement: ObtainedEntitlement? = result.obtainedEntitlement("pro")
        val didObtainEntitlement: Boolean = result.didObtainEntitlement("pro")
        val obtainedProduct: ObtainedProduct? = result.obtainedProduct("monthly")
        val didObtainProduct: Boolean = result.didObtainProduct("monthly")
    }

    fun checkResult(result: FlowResult) {
        val obtainedEntitlements: Set<ObtainedEntitlement> = result.obtainedEntitlements
        val obtainedProducts: Set<ObtainedProduct> = result.obtainedProducts
        val obtainedEntitlement: ObtainedEntitlement? = result.obtainedEntitlement("pro")
        val didObtainEntitlement: Boolean = result.didObtainEntitlement("pro")
        val obtainedProduct: ObtainedProduct? = result.obtainedProduct("monthly")
        val didObtainProduct: Boolean = result.didObtainProduct("monthly")
    }

    fun checkObtainedProduct(obtained: ObtainedProduct) {
        val product: StoreProduct = obtained.product
    }

    fun checkObtainedEntitlement(obtained: ObtainedEntitlement) {
        val entitlementInfo: EntitlementInfo = obtained.entitlementInfo
    }
}
