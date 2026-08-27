@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.OnCheckpointCompletedContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.OnCheckpointHitContext

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class CheckpointListenerAPI {

    fun check() {
        val listener = object : CheckpointListener {
            override fun onCheckpointHit(context: OnCheckpointHitContext) {}

            override fun onCheckpointCompleted(context: OnCheckpointCompletedContext) {}
        }

        // Every method has a default implementation.
        val listenerWithDefaults = object : CheckpointListener {}
    }

    fun checkCheckpointContext(context: CheckpointContext) {
        val identifier: String = context.identifier
        val customVariables: Map<String, CustomVariableValue> = context.customVariables
    }

    fun checkOnCheckpointHitContext(context: OnCheckpointHitContext) {
        val supertype: CheckpointContext = context
    }

    fun checkOnCheckpointCompletedContext(context: OnCheckpointCompletedContext) {
        val supertype: CheckpointContext = context
        val result: CheckpointResult = context.result
    }
}
