@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.OnCheckpointCompletedInfo
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.OnCheckpointHitInfo

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class CheckpointListenerAPI {

    fun check() {
        val listener = object : CheckpointListener {
            override fun onCheckpointHit(hit: OnCheckpointHitInfo) {}

            override fun onCheckpointCompleted(completion: OnCheckpointCompletedInfo) {}
        }

        // Every method has a default implementation.
        val listenerWithDefaults = object : CheckpointListener {}
    }

    fun checkOnCheckpointHitInfo(hit: OnCheckpointHitInfo) {
        val identifier: String = hit.identifier
        val customVariables: Map<String, CustomVariableValue> = hit.customVariables
    }

    fun checkOnCheckpointCompletedInfo(completion: OnCheckpointCompletedInfo) {
        val identifier: String = completion.identifier
        val customVariables: Map<String, CustomVariableValue> = completion.customVariables
        val result: CheckpointResult = completion.result
    }
}
