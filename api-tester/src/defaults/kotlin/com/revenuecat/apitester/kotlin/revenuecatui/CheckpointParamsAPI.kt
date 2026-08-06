@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointParamsAPI {

    fun check(params: CheckpointParams) {
        val customProperties: Map<String, Any> = params.customProperties

        val empty = CheckpointParams()
        val fromMap = CheckpointParams(mapOf("source" to "api-tester", "count" to 1, "enabled" to true))
        val fromPairs = CheckpointParams("source" to "api-tester", "count" to 1, "enabled" to true)
    }
}
