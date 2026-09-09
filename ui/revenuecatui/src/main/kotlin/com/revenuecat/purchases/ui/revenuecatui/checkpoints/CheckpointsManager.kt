package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Everything [CheckpointWorkflowPresenter] needs to build its paywall, read in a single pass so it can't
 * observe a call that was taken between two accessors.
 */
internal class CheckpointPresentation(
    val content: CheckpointFlowContent,
    val customVariables: Map<String, CustomVariableValue>,
)

/** What [CheckpointWorkflowPresenter] renders for a presented checkpoint. */
internal sealed class CheckpointFlowContent {

    class Workflow(val resolution: CheckpointResolution.MatchedWorkflow) : CheckpointFlowContent()

    /**
     * A matched offering. Until offering presenters exist, the offering's configured paywall is presented,
     * falling back to the default paywall when it has none.
     */
    class OfferingFlow(val offering: Offering) : CheckpointFlowContent()
}

/**
 * What a checkpoint run produced: the terminal [flowOutcome] of whatever it presented (null when nothing was,
 * including when resolving or presenting failed), and [backedOut], true when the presented flow went away because
 * the user navigated back (system back, or a navigate-back action on a workflow's first step) without purchasing
 * or restoring.
 */
internal class CheckpointRun(
    val flowOutcome: CheckpointFlowOutcome?,
    val backedOut: Boolean,
    // Releases whatever the SDK still has on screen for this run; invoked once the app has been told.
    val finishPresentation: () -> Unit = {},
)

/**
 * Runs a checkpoint hit end to end: asks the core module what the checkpoint resolves to, and presents the
 * resolved flow through [CheckpointWorkflowPresenter]. Owns the one-presentation-at-a-time constraint and the
 * pending call that routes a presented flow's terminal outcome back to the suspended [runCheckpoint] call. Runs
 * that present nothing never claim that presentation slot.
 *
 * There is one instance per [Purchases] instance, held in its `internalCpManagerSlot` and reached through
 * [checkpointsManager], so a reconfigured SDK starts with a free presentation slot. A workflow that is already
 * on screen keeps reporting to the manager that presented it, exactly once, even if the SDK is reconfigured
 * underneath it.
 */
@Suppress("TooManyFunctions")
internal class CheckpointsManager(
    // Owns the coroutines behind the callback-based gate API, so an un-awaited checkpoint lives and dies with
    // the Purchases instance holding this manager rather than with any caller scope.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val presenterFactory: (callId: String, manager: CheckpointsManager) -> CheckpointWorkflowPresenter =
        { callId, manager -> CheckpointWorkflowPresenter(callId, manager) },
) {

    private class PendingCall(
        val callId: String,
        val content: CheckpointFlowContent,
        val customVariables: Map<String, CustomVariableValue>,
        val flowFinished: CompletableDeferred<CheckpointRun>,
    ) {
        // Null until the paywall reports something; kept on the call rather than the presented window so
        // losing the window (configuration change) doesn't reset it.
        var outcome: CheckpointFlowOutcome? = null

        // Only used to take an orphaned workflow window down when its call is abandoned.
        var presenter: CheckpointWorkflowPresenter? = null
    }

    // At most one workflow may be presented at a time, so this single field is both the pending call and
    // the gate that enforces that rule: there is no second piece of state to fall out of sync with.
    private var pendingCall: PendingCall? = null

    private val nothingPresented = CheckpointRun(flowOutcome = null, backedOut = false)

    /**
     * Runs a checkpoint hit: resolves the checkpoint and presents whatever it resolved to, suspending until the
     * presented flow finishes. Never throws: a resolution or presentation failure is logged and comes back as a
     * run that presented nothing. Completion is reported by [checkpoint].
     */
    suspend fun runCheckpoint(
        purchases: Purchases,
        identifier: String,
        params: CheckpointParams?,
    ): CheckpointRun = withContext(Dispatchers.Main) {
        val customVariables = (params ?: CheckpointParams {}).customVariables
        if (!CheckpointIdentifierValidator.isValid(identifier)) {
            Logger.e(CheckpointIdentifierValidator.invalidIdentifierLogMessage(identifier))
            return@withContext nothingPresented
        }
        val resolution = try {
            purchases.internalResolveCp(
                identifier,
                customVariables.mapValues { (_, value) -> value.asRulesDimensionValue },
            )
        } catch (e: PurchasesException) {
            Logger.e("Checkpoint '$identifier' could not be resolved: ${e.error}")
            return@withContext nothingPresented
        }
        try {
            when (resolution) {
                // No offering presenter exists yet, so the offering's (or the default fallback) paywall is
                // presented instead of returning the offering for app-owned presentation.
                is CheckpointResolution.MatchedOffering ->
                    present(purchases, CheckpointFlowContent.OfferingFlow(resolution.offering), customVariables)
                is CheckpointResolution.MatchedWorkflow ->
                    present(purchases, CheckpointFlowContent.Workflow(resolution), customVariables)
                is CheckpointResolution.NoAction -> nothingPresented
            }
        } catch (_: PurchasesException) {
            // Already logged by presentationError.
            nothingPresented
        }
    }

    /**
     * The checkpoint API: runs the checkpoint and invokes [callback] with what the user obtained, at most once
     * and on the main thread, unless the user backed out of the presented flow. Never throws.
     */
    fun checkpoint(
        purchases: Purchases,
        identifier: String,
        params: CheckpointParams?,
        callback: CheckpointPassedCallback,
    ) {
        scope.launch {
            val activeEntitlementsBefore = cachedActiveEntitlementIds(purchases)
            val run = runCheckpoint(purchases, identifier, params)
            try {
                if (run.backedOut) {
                    Logger.d("Checkpoint '$identifier': the user backed out, so the callback is not invoked.")
                } else {
                    callback.onCheckpointPassed(run.toResult(activeEntitlementsBefore))
                }
            } finally {
                run.finishPresentation()
            }
        }
    }

    fun presentation(callId: String): CheckpointPresentation? =
        withPendingCall(callId) { CheckpointPresentation(it.content, it.customVariables) }

    fun recordOutcome(callId: String, outcome: CheckpointFlowOutcome) {
        withPendingCall(callId) { it.outcome = outcome }
    }

    // A recorded purchase or restore means the user went through, however the window went away: checkpoint
    // paywalls don't auto-dismiss on restore, so a user who restored and then backed out still went through. A
    // paywall that went away without reporting anything was dismissed.
    fun onPresentationFinished(
        callId: String,
        navigatedBack: Boolean = false,
        finishPresentation: () -> Unit = {},
    ) {
        val finished = take(callId) ?: run {
            finishPresentation()
            return
        }
        val recorded = finished.outcome
        val obtained = recorded is CheckpointFlowOutcome.Purchased || recorded is CheckpointFlowOutcome.Restored
        finished.flowFinished.complete(
            CheckpointRun(
                recorded ?: CheckpointFlowOutcome.Dismissed,
                backedOut = navigatedBack && !obtained,
                finishPresentation = finishPresentation,
            ),
        )
    }

    // Like onPresentationFinished, for a presentation that failed: an outcome the paywall already reported
    // (e.g. a purchase before the configuration change) still wins, but a workflow that failed before
    // reporting anything surfaces as an error rather than a phantom dismissal.
    fun onPresentationFailed(callId: String, error: PurchasesError) {
        val failed = take(callId) ?: return
        failed.flowFinished.complete(
            CheckpointRun(failed.outcome ?: CheckpointFlowOutcome.Error(error), backedOut = false),
        )
    }

    private suspend fun present(
        purchases: Purchases,
        content: CheckpointFlowContent,
        customVariables: Map<String, CustomVariableValue>,
    ): CheckpointRun {
        val activity = purchases.currentActivity ?: presentationError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present checkpoint workflow: no started Activity found.",
        )
        val call = PendingCall(UUID.randomUUID().toString(), content, customVariables, CompletableDeferred())
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
            return call.flowFinished.await()
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
