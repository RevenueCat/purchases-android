@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.apitester.kotlin.exhaustive
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointEvaluation
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointEvaluationAPI {

    fun checkEvaluation(evaluation: CheckpointEvaluation) {
        when (evaluation) {
            is CheckpointEvaluation.OfferingReturned -> {
                val offering: Offering = evaluation.offering
            }
            is CheckpointEvaluation.FlowPresented -> {}
            is CheckpointEvaluation.NoAction -> {
                val reason: CheckpointResult.NoAction.Reason = evaluation.reason
            }
            // The hierarchy is closed but not sealed, so consumers must handle cases added later.
            else -> {}
        }.exhaustive
    }
}
