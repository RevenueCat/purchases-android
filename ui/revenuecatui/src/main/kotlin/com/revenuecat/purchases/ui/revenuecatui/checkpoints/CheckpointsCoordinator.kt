package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.content.Intent
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs a checkpoint hit end to end: fires listener events, asks the core module what the checkpoint resolves
 * to, and presents the resolved workflow through [CheckpointWorkflowActivity]. Owns the
 * one-presentation-at-a-time constraint and the pending-call registry that routes the presented paywall's
 * terminal outcome back to the suspended [checkpoint] call.
 */
internal object CheckpointsCoordinator {

    private class PendingCall(
        val resolution: CheckpointResolution.Workflow,
        val paywallFinished: CompletableDeferred<CheckpointPaywallOutcome>,
    ) {
        // Kept here rather than on the activity so a configuration change doesn't reset it.
        var outcome: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Dismissed
    }

    private val pendingCalls = mutableMapOf<String, PendingCall>()
    private val presenting = AtomicBoolean(false)

    /**
     * Runs on the main dispatcher; listener callbacks fire on the main thread. For presented workflows,
     * suspends until the paywall finishes.
     *
     * @throws PurchasesException if the checkpoint workflow should run but can't.
     */
    suspend fun checkpoint(
        purchases: Purchases,
        identifier: String,
        params: CheckpointParams?,
    ): CheckpointResult = withContext(Dispatchers.Main) {
        val checkpoint = CheckpointInfo(identifier, params ?: CheckpointParams())
        purchases.checkpointListener?.onCheckpointHit(checkpoint)
        val resolution = purchases.resolveCheckpoint(identifier, checkpoint.params.customProperties)
        val result = when (resolution) {
            is CheckpointResolution.Workflow ->
                CheckpointResult.PaywallPresented(checkpoint, present(purchases, resolution))
            is CheckpointResolution.NoAction ->
                CheckpointResult.NoAction(checkpoint, resolution.reason.toResultReason())
        }
        purchases.checkpointListener?.onCheckpointCompleted(checkpoint, result)
        result
    }

    fun resolution(callId: String): CheckpointResolution.Workflow? =
        synchronized(this) { pendingCalls[callId]?.resolution }

    fun recordOutcome(callId: String, outcome: CheckpointPaywallOutcome) {
        synchronized(this) { pendingCalls[callId]?.outcome = outcome }
    }

    fun onPaywallFinished(callId: String) {
        val pendingCall = synchronized(this) { pendingCalls.remove(callId) } ?: return
        presenting.set(false)
        pendingCall.paywallFinished.complete(pendingCall.outcome)
    }

    @VisibleForTesting
    fun resetForTesting() {
        synchronized(this) { pendingCalls.clear() }
        presenting.set(false)
    }

    private suspend fun present(
        purchases: Purchases,
        resolution: CheckpointResolution.Workflow,
    ): CheckpointPaywallOutcome {
        val activity = purchases.currentActivity ?: presentationError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present checkpoint workflow: no started Activity found.",
        )
        if (!presenting.compareAndSet(false, true)) {
            presentationError(
                PurchasesErrorCode.OperationAlreadyInProgressError,
                "Another checkpoint workflow is already being presented.",
            )
        }
        val callId = UUID.randomUUID().toString()
        val pendingCall = PendingCall(resolution, CompletableDeferred())
        synchronized(this) { pendingCalls[callId] = pendingCall }
        try {
            activity.startActivity(
                Intent(activity, CheckpointWorkflowActivity::class.java)
                    .putExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID, callId),
            )
            return pendingCall.paywallFinished.await()
        } catch (e: CancellationException) {
            abandonPendingCall(callId)
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            abandonPendingCall(callId)
            presentationError(
                PurchasesErrorCode.ConfigurationError,
                "Failed to present checkpoint workflow: $e",
            )
        }
    }

    // Idempotent counterpart of onPaywallFinished for calls that fail or get cancelled before the activity
    // reports: only the side that actually removes the pending call releases the presenting gate.
    private fun abandonPendingCall(callId: String) {
        synchronized(this) { pendingCalls.remove(callId) } ?: return
        presenting.set(false)
    }

    private fun presentationError(code: PurchasesErrorCode, message: String): Nothing {
        val error = PurchasesError(code, message)
        Logger.e(error.toString())
        throw PurchasesException(error)
    }

    private fun CheckpointResolution.NoAction.Reason.toResultReason(): CheckpointResult.NoAction.Reason =
        when (this) {
            CheckpointResolution.NoAction.Reason.NO_MATCH -> CheckpointResult.NoAction.Reason.NO_MATCH
            CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE ->
                CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE
            CheckpointResolution.NoAction.Reason.DISABLED -> CheckpointResult.NoAction.Reason.DISABLED
            CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT ->
                CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT
        }
}
