@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointParamsAPI {

    fun check(params: CheckpointParams) {
        val customVariables: Map<String, CustomVariableValue> = params.customVariables

        val empty = CheckpointParams()
        val fromMap = CheckpointParams(
            mapOf(
                "source" to CustomVariableValue.String("api-tester"),
                "count" to CustomVariableValue.Number(1),
                "enabled" to CustomVariableValue.Boolean(true),
            ),
        )
        val fromPairs = CheckpointParams(
            "source" to CustomVariableValue.String("api-tester"),
            "count" to CustomVariableValue.Number(1),
            "enabled" to CustomVariableValue.Boolean(true),
        )
    }
}
