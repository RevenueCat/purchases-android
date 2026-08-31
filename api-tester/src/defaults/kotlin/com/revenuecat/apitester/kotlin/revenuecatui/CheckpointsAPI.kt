@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointListener

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointsAPI {

    suspend fun checkCoroutines(purchases: Purchases, params: CheckpointParams) {
        val result: CheckpointResult = purchases.awaitCheckpoint("checkpoint_identifier")
        val resultWithParams: CheckpointResult = purchases.awaitCheckpoint("checkpoint_identifier", params)
        val resultWithNamedParams: CheckpointResult = purchases.awaitCheckpoint(
            checkpointIdentifier = "checkpoint_identifier",
            params = params,
        )
    }

    fun checkListener(purchases: Purchases, listener: CheckpointListener) {
        purchases.checkpointListener = listener
        purchases.checkpointListener = null
        val currentListener: CheckpointListener? = purchases.checkpointListener
    }
}
