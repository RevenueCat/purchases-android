@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointCompletedContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointHitContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class CheckpointListenerAPI {

    fun check() {
        val listener = object : CheckpointListener {
            override fun onCheckpointHit(context: CheckpointHitContext) {}

            override fun onCheckpointCompleted(context: CheckpointCompletedContext) {}
        }

        // Every method has a default implementation.
        val listenerWithDefaults = object : CheckpointListener {}
    }

    fun checkCheckpointContext(context: CheckpointContext) {
        val identifier: String = context.identifier
        val customVariables: Map<String, CustomVariableValue> = context.customVariables
    }

    fun checkCheckpointHitContext(context: CheckpointHitContext) {
        val supertype: CheckpointContext = context
    }

    fun checkCheckpointCompletedContext(context: CheckpointCompletedContext) {
        val supertype: CheckpointContext = context
        val result: CheckpointResult = context.result
    }
}
