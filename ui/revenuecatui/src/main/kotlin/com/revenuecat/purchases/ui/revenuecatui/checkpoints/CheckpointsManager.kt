package com.revenuecat.purchases.ui.revenuecatui.checkpoints

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
import java.util.UUID

/**
 * Everything [CheckpointWorkflowPresenter] needs to build its paywall, read in a single pass so it can't
 * observe a call that was taken between two accessors.
 */
internal class CheckpointPresentation(
    val resolution: CheckpointResolution.MatchedWorkflow,
    val customVariables: Map<String, CustomVariableValue>,
)

/**
 * [backedOut] is true when a presented paywall went away because the user navigated back (system back,
 * or a navigate-back action on a workflow's first step) without purchasing or restoring.
 */
internal class CheckpointRun(val result: CheckpointResult, val backedOut: Boolean)

/**
 * Runs a checkpoint hit end to end: asks the core module what the checkpoint resolves to, and either returns
 * its data or presents the resolved workflow through [CheckpointWorkflowPresenter].
 * Owns the one-presentation-at-a-time constraint and the pending call that routes a presented paywall's
 * terminal outcome back to the suspended [checkpoint] call. Data-only results never claim that presentation
 * slot.
 *
 * There is one instance per [Purchases] instance, held in its `internalCpManagerSlot` and reached through
 * [checkpointsManager], so a reconfigured SDK starts with a free presentation slot. A workflow that is already
 * on screen keeps reporting to the manager that presented it, exactly once, even if the SDK is reconfigured
 * underneath it.
 */
@Suppress("TooManyFunctions")
internal class CheckpointsManager(
    private val presenterFactory: (callId: String, manager: CheckpointsManager) -> CheckpointWorkflowPresenter =
        { callId, manager -> CheckpointWorkflowPresenter(callId, manager) },
) {

    private class PendingCall(
        val callId: String,
        val resolution: CheckpointResolution.MatchedWorkflow,
        val customVariables: Map<String, CustomVariableValue>,
        val paywallFinished: CompletableDeferred<PresentationEnd>,
    ) {
        // Null until the paywall reports something; kept on the call rather than the presented window so
        // losing the window (configuration change) doesn't reset it.
        var outcome: CheckpointPaywallOutcome? = null

        // Only used to take an orphaned workflow window down when its call is abandoned.
        var presenter: CheckpointWorkflowPresenter? = null
    }

    private class PresentationEnd(val outcome: CheckpointPaywallOutcome, val backedOut: Boolean)

    // At most one workflow may be presented at a time, so this single field is both the pending call and
    // the gate that enforces that rule: there is no second piece of state to fall out of sync with.
    private var pendingCall: PendingCall? = null

    /**
     * Runs on the main dispatcher. For presented workflows, suspends until the paywall finishes.
     *
     * @throws PurchasesException if the checkpoint workflow should run but can't.
     */
    suspend fun checkpoint(
        purchases: Purchases,
        identifier: String,
        params: CheckpointParams?,
    ): CheckpointResult = runCheckpoint(purchases, identifier, params).result

    suspend fun runCheckpoint(
        purchases: Purchases,
        identifier: String,
        params: CheckpointParams?,
    ): CheckpointRun = withContext(Dispatchers.Main) {
        val customVariables = (params ?: CheckpointParams {}).customVariables
        if (!CheckpointIdentifierValidator.isValid(identifier)) {
            Logger.e(CheckpointIdentifierValidator.invalidIdentifierLogMessage(identifier))
            return@withContext CheckpointRun(
                CheckpointResult.NoAction(CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER),
                backedOut = false,
            )
        }

        val resolution = purchases.internalResolveCp(
            identifier,
            customVariables.mapValues { (_, value) -> value.asRulesDimensionValue },
        )
        val run = when (resolution) {
            is CheckpointResolution.MatchedOffering ->
                CheckpointRun(CheckpointResult.ReceivedOffering(resolution.offering), backedOut = false)
            is CheckpointResolution.MatchedWorkflow -> {
                val end = present(purchases, resolution, customVariables)
                CheckpointRun(CheckpointResult.PaywallPresented(end.outcome), end.backedOut)
            }
            is CheckpointResolution.NoAction ->
                CheckpointRun(CheckpointResult.NoAction(resolution.reason.toResultReason()), backedOut = false)
        }
        run
    }

    fun presentation(callId: String): CheckpointPresentation? =
        withPendingCall(callId) { CheckpointPresentation(it.resolution, it.customVariables) }

    fun recordOutcome(callId: String, outcome: CheckpointPaywallOutcome) {
        withPendingCall(callId) { it.outcome = outcome }
    }

    // A recorded purchase or restore means the user went through, however the window went away: checkpoint
    // paywalls don't auto-dismiss on restore, so a user who restored and then backed out still went through. A
    // paywall that went away without reporting anything was dismissed.
    fun onPresentationFinished(callId: String, navigatedBack: Boolean = false) {
        val finished = take(callId) ?: return
        val recorded = finished.outcome
        val obtained = recorded is CheckpointPaywallOutcome.Purchased || recorded is CheckpointPaywallOutcome.Restored
        finished.paywallFinished.complete(
            PresentationEnd(recorded ?: CheckpointPaywallOutcome.Dismissed, backedOut = navigatedBack && !obtained),
        )
    }

    // Like onPresentationFinished, for a presentation that failed: an outcome the paywall already reported
    // (e.g. a purchase before the configuration change) still wins, but a workflow that failed before
    // reporting anything surfaces as an error rather than a phantom dismissal.
    fun onPresentationFailed(callId: String, error: PurchasesError) {
        val failed = take(callId) ?: return
        failed.paywallFinished.complete(
            PresentationEnd(failed.outcome ?: CheckpointPaywallOutcome.Error(error), backedOut = false),
        )
    }

    private suspend fun present(
        purchases: Purchases,
        resolution: CheckpointResolution.MatchedWorkflow,
        customVariables: Map<String, CustomVariableValue>,
    ): PresentationEnd {
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
            val presenter = presenterFactory(call.callId, this)
            withPendingCall(call.callId) { it.presenter = presenter }
            presenter.show(activity)
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

    // Counterpart of onPresentationFinished for calls that fail or get cancelled before the presented workflow
    // reports: releases the slot and takes the orphaned paywall down with the caller that asked for it.
    // Always runs on the main thread, inside the checkpoint() dispatch.
    private fun abandon(callId: String) {
        take(callId)?.presenter?.abandon()
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
        CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT ->
            CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT
    }
