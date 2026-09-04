@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateCallback
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingCompletion
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointOfferingPresenter

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointsAPI {

    fun checkCheckpoint(purchases: Purchases, params: CheckpointParams, callback: CheckpointGateCallback) {
        purchases.checkpoint("checkpoint_identifier") { gateResult: CheckpointGateResult -> }
        purchases.checkpoint("checkpoint_identifier", params) { gateResult: CheckpointGateResult -> }
        purchases.checkpoint("checkpoint_identifier", params = null) { gateResult: CheckpointGateResult -> }
        purchases.checkpoint("checkpoint_identifier", callback)
        purchases.checkpoint(
            checkpointIdentifier = "checkpoint_identifier",
            params = params,
            callback = callback,
        )
    }

    fun checkListener(purchases: Purchases, listener: CheckpointListener) {
        purchases.checkpointListener = listener
        purchases.checkpointListener = null
        val currentListener: CheckpointListener? = purchases.checkpointListener
    }

    fun checkOfferingPresenter(purchases: Purchases, presenter: CheckpointOfferingPresenter) {
        purchases.checkpointOfferingPresenter = presenter
        purchases.checkpointOfferingPresenter = null
        val currentPresenter: CheckpointOfferingPresenter? = purchases.checkpointOfferingPresenter
        val lambdaPresenter = CheckpointOfferingPresenter {
                offering: Offering, completion: CheckpointOfferingCompletion ->
            completion.finished()
            completion.failed()
        }
    }
}
