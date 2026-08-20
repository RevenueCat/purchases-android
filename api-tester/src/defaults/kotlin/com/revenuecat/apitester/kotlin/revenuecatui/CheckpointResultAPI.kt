@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.apitester.kotlin.exhaustive
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointInfo
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointResultAPI {

    fun checkResult(result: CheckpointResult) {
        val checkpoint: CheckpointInfo = result.checkpoint

        when (result) {
            is CheckpointResult.ReceivedOffering -> {
                val offering: Offering = result.offering
            }
            is CheckpointResult.PaywallPresented -> {
                val paywallOutcome: CheckpointPaywallOutcome = result.paywallOutcome
            }
            is CheckpointResult.NoAction -> {
                val reason: CheckpointResult.NoAction.Reason = result.reason
            }
            // The hierarchy is closed but not sealed, so consumers must handle cases added later.
            else -> {}
        }.exhaustive
    }

    fun checkCheckpointInfo(checkpoint: CheckpointInfo) {
        val identifier: String = checkpoint.identifier
        val params = checkpoint.params
    }

    fun checkReason(reason: CheckpointResult.NoAction.Reason) {
        val value: String = reason.value

        val noMatch: CheckpointResult.NoAction.Reason = CheckpointResult.NoAction.Reason.NO_MATCH
        val holdout: CheckpointResult.NoAction.Reason = CheckpointResult.NoAction.Reason.HOLDOUT
        val frequencyCapped: CheckpointResult.NoAction.Reason = CheckpointResult.NoAction.Reason.FREQUENCY_CAPPED
        val configurationUnavailable: CheckpointResult.NoAction.Reason =
            CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE
        val disabled: CheckpointResult.NoAction.Reason = CheckpointResult.NoAction.Reason.DISABLED
        val unknownCheckpoint: CheckpointResult.NoAction.Reason = CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT
        val invalidCheckpointIdentifier: CheckpointResult.NoAction.Reason =
            CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER
    }
}
