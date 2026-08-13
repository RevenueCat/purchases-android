@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointInfo
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class CheckpointListenerAPI {

    fun check() {
        val listener = object : CheckpointListener {
            override fun onCheckpointHit(checkpoint: CheckpointInfo) {}

            override fun onCheckpointCompleted(checkpoint: CheckpointInfo, result: CheckpointResult) {}
        }

        // Every method has a default implementation.
        val listenerWithDefaults = object : CheckpointListener {}
    }
}
