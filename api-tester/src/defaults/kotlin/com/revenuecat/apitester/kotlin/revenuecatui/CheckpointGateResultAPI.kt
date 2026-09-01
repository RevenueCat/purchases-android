@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.EntitlementGrant

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointGateResultAPI {

    fun checkGateResult(gateResult: CheckpointGateResult) {
        val entitlements: List<EntitlementGrant> = gateResult.entitlements
        val noWorkflowReason: CheckpointGateResult.NoWorkflowReason? = gateResult.noWorkflowReason
        val error: PurchasesError? = gateResult.error
    }

    fun checkEntitlementGrant(grant: EntitlementGrant) {
        val identifier: String = grant.identifier
    }

    fun checkNoWorkflowReason() {
        val noMatch: CheckpointGateResult.NoWorkflowReason = CheckpointGateResult.NoWorkflowReason.NO_MATCH
        val holdout: CheckpointGateResult.NoWorkflowReason = CheckpointGateResult.NoWorkflowReason.HOLDOUT
        val frequencyCapped: CheckpointGateResult.NoWorkflowReason =
            CheckpointGateResult.NoWorkflowReason.FREQUENCY_CAPPED
        val configurationUnavailable: CheckpointGateResult.NoWorkflowReason =
            CheckpointGateResult.NoWorkflowReason.CONFIGURATION_UNAVAILABLE
        val unknownCheckpoint: CheckpointGateResult.NoWorkflowReason =
            CheckpointGateResult.NoWorkflowReason.UNKNOWN_CHECKPOINT
        val invalidCheckpointIdentifier: CheckpointGateResult.NoWorkflowReason =
            CheckpointGateResult.NoWorkflowReason.INVALID_CHECKPOINT_IDENTIFIER
        val error: CheckpointGateResult.NoWorkflowReason = CheckpointGateResult.NoWorkflowReason.ERROR
    }
}
