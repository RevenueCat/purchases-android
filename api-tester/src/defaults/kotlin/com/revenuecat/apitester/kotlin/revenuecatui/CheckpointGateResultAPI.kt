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
        val noActionReason: CheckpointGateResult.NoActionReason? = gateResult.noActionReason
        val error: PurchasesError? = gateResult.error
    }

    fun checkEntitlementGrant(grant: EntitlementGrant) {
        val identifier: String = grant.identifier
    }

    fun checkNoActionReason() {
        val noMatch: CheckpointGateResult.NoActionReason = CheckpointGateResult.NoActionReason.NO_MATCH
        val holdout: CheckpointGateResult.NoActionReason = CheckpointGateResult.NoActionReason.HOLDOUT
        val frequencyCapped: CheckpointGateResult.NoActionReason =
            CheckpointGateResult.NoActionReason.FREQUENCY_CAPPED
        val configurationUnavailable: CheckpointGateResult.NoActionReason =
            CheckpointGateResult.NoActionReason.CONFIGURATION_UNAVAILABLE
        val unknownCheckpoint: CheckpointGateResult.NoActionReason =
            CheckpointGateResult.NoActionReason.UNKNOWN_CHECKPOINT
        val invalidCheckpointIdentifier: CheckpointGateResult.NoActionReason =
            CheckpointGateResult.NoActionReason.INVALID_CHECKPOINT_IDENTIFIER
        val error: CheckpointGateResult.NoActionReason = CheckpointGateResult.NoActionReason.ERROR
    }
}
