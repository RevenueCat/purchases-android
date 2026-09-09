@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointCallback
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointsAPI {

    fun checkCheckpoint(purchases: Purchases, params: CheckpointParams, callback: CheckpointCallback) {
        purchases.checkpoint("checkpoint_identifier") { result: FlowResult? -> }
        purchases.checkpoint("checkpoint_identifier", params) { result: FlowResult? -> }
        purchases.checkpoint("checkpoint_identifier", params = null) { result: FlowResult? -> }
        purchases.checkpoint("checkpoint_identifier", callback)
        purchases.checkpoint(
            checkpointIdentifier = "checkpoint_identifier",
            params = params,
            callback = callback,
        )
    }
}
