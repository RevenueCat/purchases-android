package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Intent
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * Everything [CheckpointWorkflowActivity] needs to build its paywall, read in a single pass so it can't observe
 * a call that was taken between two accessors.
 */
internal class CheckpointPresentation(
    val resolution: CheckpointResolution.MatchedWorkflow,
    val customVariables: Map<String, CustomVariableValue>,
)

/**
 * Runs a checkpoint hit end to end: fires listener events, asks the core module what the checkpoint resolves
 * to, and either returns its data or presents the resolved workflow through [CheckpointWorkflowActivity]. Owns
 * the one-presentation-at-a-time constraint and the pending call that routes a presented paywall's terminal
 * outcome back to the suspended [checkpoint] call. Data-only results never claim that presentation slot.
 *
 * There is one instance per [Purchases] instance, held in its `checkpointManagerSlot` and reached through
 * [checkpointsManager], so the listener and any in-flight presentation die with the SDK instance that owns
 * them and reconfiguring the SDK always starts from a clean state.
 */
internal class CheckpointsManager {

    private class PendingCall(
        val callId: String,
        val resolution: CheckpointResolution.MatchedWorkflow,
        val customVariables: Map<String, CustomVariableValue>,
        val paywallFinished: CompletableDeferred<CheckpointPaywallOutcome>,
    ) {
        // Kept here rather than on the activity so a configuration change doesn't reset it.
        var outcome: CheckpointPaywallOutcome = CheckpointPaywallOutcome.Dismissed

        // Weak because this manager outlives every activity. Only used to take an orphaned paywall down
        // when its call is abandoned.
        var activity: WeakReference<Activity>? = null
    }

    @get:Synchronized
    @set:Synchronized
    var checkpointListener: CheckpointListener? = null

    // At most one workflow may be presented at a time, so this single field is both the pending call and
    // the gate that enforces that rule: there is no second piece of state to fall out of sync with.
    private var pendingCall: PendingCall? = null

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
        val checkpoint = CheckpointInfo(identifier, params ?: CheckpointParams {})
        checkpointListener?.onCheckpointHit(checkpoint)
        if (!CheckpointIdentifierValidator.isValid(identifier)) {
            Logger.e(CheckpointIdentifierValidator.invalidIdentifierLogMessage(identifier))
            val result = CheckpointResult.NoAction(
                checkpoint,
                CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER,
            )
            checkpointListener?.onCheckpointCompleted(checkpoint, result)
            return@withContext result
        }

        val resolution = purchases.resolveCheckpoint(
            identifier,
            checkpoint.params.customVariables.mapValues { (_, value) -> value.asRulesDimensionValue },
        )
        val result = when (resolution) {
            is CheckpointResolution.MatchedOffering -> CheckpointResult.ReceivedOffering(
                checkpoint,
                resolution.offering,
            )
            is CheckpointResolution.MatchedWorkflow ->
                CheckpointResult.PaywallPresented(
                    checkpoint,
                    present(purchases, resolution, checkpoint.params.customVariables),
                )
            is CheckpointResolution.NoAction ->
                CheckpointResult.NoAction(checkpoint, resolution.reason.toResultReason())
        }
        checkpointListener?.onCheckpointCompleted(checkpoint, result)
        result
    }

    fun presentation(callId: String): CheckpointPresentation? =
        withPendingCall(callId) { CheckpointPresentation(it.resolution, it.customVariables) }

    fun onPresentationStarted(callId: String, activity: Activity) {
        withPendingCall(callId) { it.activity = WeakReference(activity) }
    }

    fun recordOutcome(callId: String, outcome: CheckpointPaywallOutcome) {
        withPendingCall(callId) { it.outcome = outcome }
    }

    /**
     * A rotation must keep the pending call alive for the recreated instance, but a backgrounded destroy
     * (memory pressure, "Don't keep activities") must still release it, or the suspended caller and the
     * one-at-a-time slot stay stuck for as long as this manager lives.
     */
    fun onActivityDestroyed(callId: String, isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) return
        val finished = take(callId) ?: return
        finished.paywallFinished.complete(finished.outcome)
    }

    private suspend fun present(
        purchases: Purchases,
        resolution: CheckpointResolution.MatchedWorkflow,
        customVariables: Map<String, CustomVariableValue>,
    ): CheckpointPaywallOutcome {
        val activity = purchases.currentActivity ?: presentationError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present checkpoint workflow: no started Activity found.",
        )
        val call = PendingCall(UUID.randomUUID().toString(), resolution, customVariables, CompletableDeferred())
        val claimed = synchronized(this) { (pendingCall == null).also { if (it) pendingCall = call } }
        if (!claimed) {
            presentationError(
                PurchasesErrorCode.OperationAlreadyInProgressError,
                "Another checkpoint workflow is already being presented.",
            )
        }
        try {
            activity.startActivity(
                Intent(activity, CheckpointWorkflowActivity::class.java)
                    .putExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID, call.callId),
            )
            return call.paywallFinished.await()
        } catch (e: CancellationException) {
            abandon(call.callId)
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            abandon(call.callId)
            presentationError(
                PurchasesErrorCode.ConfigurationError,
                "Failed to present checkpoint workflow: $e",
            )
        }
    }

    // Counterpart of onActivityDestroyed for calls that fail or get cancelled before the presented workflow
    // reports: releases the slot and takes the orphaned paywall down with the caller that asked for it.
    // Always runs on the main thread, inside the checkpoint() dispatch.
    private fun abandon(callId: String) {
        take(callId)?.activity?.get()?.finish()
    }

    private fun take(callId: String): PendingCall? = withPendingCall(callId) {
        pendingCall = null
        it
    }

    /**
     * Runs [block] under the lock on the pending call, but only if [callId] still identifies it. Matching on
     * the id is what makes a late report from an abandoned or process-restored activity a no-op instead of
     * something that disturbs the call that replaced it, so every accessor goes through here.
     */
    private fun <T> withPendingCall(callId: String, block: (PendingCall) -> T): T? =
        synchronized(this) { pendingCall?.takeIf { it.callId == callId }?.let(block) }

    private fun presentationError(code: PurchasesErrorCode, message: String): Nothing {
        val error = PurchasesError(code, message)
        Logger.e(error.toString())
        throw PurchasesException(error)
    }
}

// Exhaustive by construction: a reason added to the core seam is a compile error here rather than a
// silently unmapped value.
private fun CheckpointResolution.NoAction.Reason.toResultReason(): CheckpointResult.NoAction.Reason =
    when (this) {
        CheckpointResolution.NoAction.Reason.NO_MATCH -> CheckpointResult.NoAction.Reason.NO_MATCH
        CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE ->
            CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE
        CheckpointResolution.NoAction.Reason.DISABLED -> CheckpointResult.NoAction.Reason.DISABLED
        CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT ->
            CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT
    }
