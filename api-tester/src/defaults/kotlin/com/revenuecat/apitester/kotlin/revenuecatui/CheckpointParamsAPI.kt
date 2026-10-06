@file:OptIn(InviteOnlyCheckpointsAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowPresentationMode

@Suppress("unused", "UNUSED_VARIABLE", "LongMethod")
private class CheckpointParamsAPI {

    fun check(params: CheckpointParams) {
        val customVariables: Map<String, CustomVariableValue> = params.customVariables
        val presentationMode: FlowPresentationMode = params.presentationMode

        val empty: CheckpointParams = CheckpointParams.Builder().build()
        val emptyDsl: CheckpointParams = CheckpointParams {}
        val fromBuilder: CheckpointParams = CheckpointParams.Builder()
            .setCustomVariables(mapOf("source" to CustomVariableValue.String("api-tester")))
            .build()
        val fromDsl: CheckpointParams = CheckpointParams {
            customVariables {
                "string" to "api-tester"
                "int" to 1
                "long" to 1L
                "double" to 1.0
                "float" to 1.0f
                "boolean" to true
                "value" to CustomVariableValue.String("api-tester")
            }
        }
        val added: Map<String, CustomVariableValue> = CheckpointParams.CustomVariablesBuilder()
            .add("string", "api-tester")
            .add("int", 1)
            .add("long", 1L)
            .add("double", 1.0)
            .add("float", 1.0f)
            .add("boolean", true)
            .add("value", CustomVariableValue.Boolean(true))
            .build()
        val fromAdded: CheckpointParams = CheckpointParams.Builder().setCustomVariables(added).build()
        val sheetFromBuilder: CheckpointParams = CheckpointParams.Builder()
            .setPresentationMode(FlowPresentationMode.MODAL_SHEET)
            .build()
        val fullScreenFromDsl: CheckpointParams = CheckpointParams {
            presentationMode(FlowPresentationMode.MODAL_FULL_SCREEN)
        }
    }

    fun checkPresentationMode(mode: FlowPresentationMode) {
        val default: FlowPresentationMode = FlowPresentationMode.DEFAULT
        val fullScreen: FlowPresentationMode = FlowPresentationMode.MODAL_FULL_SCREEN
        val sheet: FlowPresentationMode = FlowPresentationMode.MODAL_SHEET
        val same: Boolean = mode == default
        val hash: Int = mode.hashCode()
        val text: String = mode.toString()
    }
}
