package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
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
     * A matched offering with no [PaywallPresenter], per call or registered: the offering's configured paywall
     * is presented, falling back to the default paywall when it has none.
     */
    class OfferingFlow(val offering: Offering) : CheckpointFlowContent()
}

/**
 * What a checkpoint run produced: the terminal [flowOutcome] of whatever it presented (null when nothing was,
 * including when resolving or presenting failed), and [backedOut], true when the presented flow went away because
 * the user navigated back (system back, or a navigate-back action on a workflow's first step) without purchasing
 * or restoring.
 *
 * [blockedByPresentedFlow] is true when the checkpoint resolved to a flow that was not presented because another
 * checkpoint flow was already on screen. That earlier call is the one the app is waiting on, so a blocked run is
 * not reported as passed.
 */
internal class CheckpointRun(
    val flowOutcome: CheckpointFlowOutcome?,
    val backedOut: Boolean,
    val blockedByPresentedFlow: Boolean = false,
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

    @get:Synchronized
    @set:Synchronized
    var paywallPresenter: PaywallPresenter? = null

    // At most one workflow may be presented at a time, so this single field is both the pending call and
    // the gate that enforces that rule: there is no second piece of state to fall out of sync with.
    private var pendingCall: PendingCall? = null

    private val nothingPresented = CheckpointRun(flowOutcome = null, backedOut = false)
    private val blockedByPresentedFlow =
        CheckpointRun(flowOutcome = null, backedOut = false, blockedByPresentedFlow = true)

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
        val presenter = params?.paywallPresenter ?: paywallPresenter
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
                is CheckpointResolution.MatchedOffering ->
                    presentOffering(purchases, identifier, resolution.offering, customVariables, presenter)
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
     * and on the main thread. The callback is skipped when the user backed out of the presented flow, and when
     * the checkpoint resolved to a flow while another checkpoint flow was already on screen. Never throws.
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
                when {
                    run.blockedByPresentedFlow -> Logger.w(
                        "Checkpoint '$identifier': another checkpoint flow is already being presented, so this " +
                            "call is ignored and its callback is not invoked.",
                    )
                    run.backedOut ->
                        Logger.d("Checkpoint '$identifier': the user backed out, so the callback is not invoked.")
                    else -> callback.onCheckpointPassed(run.toResult(activeEntitlementsBefore))
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

    /**
     * Presents a matched offering through [presenter] (the call's own, else the registered [paywallPresenter])
     * when there is one; the offering's own (or the default fallback) paywall otherwise. The app-owned
     * presentation claims the same one-presentation-at-a-time slot as SDK-presented workflows and resolves through
     * its completion's first report, after the SDK has synced the store purchases made during it.
     */
    private suspend fun presentOffering(
        purchases: Purchases,
        identifier: String,
        offering: Offering,
        customVariables: Map<String, CustomVariableValue>,
        presenter: PaywallPresenter?,
    ): CheckpointRun {
        val content = CheckpointFlowContent.OfferingFlow(offering)
        if (presenter == null) return present(purchases, content, customVariables)
        val call = PendingCall(UUID.randomUUID().toString(), content, customVariables, CompletableDeferred())
        if (!claim(call)) return blockedByPresentedFlow
        try {
            presenter.present(
                PaywallPresenter.Params(offering, identifier, customVariables),
                PresenterCompletion(call.callId, purchases),
            )
            return call.flowFinished.await()
        } catch (e: CancellationException) {
            abandon(call.callId)
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            abandon(call.callId)
            presentationError(
                PurchasesErrorCode.ConfigurationError,
                "Paywall presenter failed: $e",
            )
        }
    }

    // Routes an app-owned presentation's report back to its pending call. take() removes the call, so only the
    // first report wins and the one-presentation slot is released with it, before the sync that follows a
    // purchased or closed report: the app's UI is already gone, so a new checkpoint may present meanwhile.
    private inner class PresenterCompletion(
        private val callId: String,
        private val purchases: Purchases,
    ) : PaywallPresenter.Completion {

        override fun complete(result: PaywallPresenter.Completion.Result) {
            when (result) {
                PaywallPresenter.Completion.Result.Purchased -> finished(reportedPurchase = true)
                PaywallPresenter.Completion.Result.Closed,
                PaywallPresenter.Completion.Result.ContinuedWithoutPurchasing,
                -> finished(reportedPurchase = false)
                PaywallPresenter.Completion.Result.NavigatedBack -> navigatedBack()
                // The hierarchy is closed but not sealed; a result this code doesn't know is the safest thing it
                // can be: the user left without purchasing and passes the checkpoint.
                else -> {
                    Logger.e("Unknown paywall presenter result '$result'; treating it as closed.")
                    finished(reportedPurchase = false)
                }
            }
        }

        private fun navigatedBack() {
            take(callId)?.let { call ->
                call.flowFinished.complete(
                    CheckpointRun(CheckpointFlowOutcome.Dismissed, backedOut = true),
                )
            }
        }

        // FETCH_CURRENT posts the store purchases the SDK hasn't seen yet (e.g. made through the app's own
        // billing client) before fetching, so the outcome reflects whatever the app's paywall sold.
        private fun finished(reportedPurchase: Boolean) {
            val call = take(callId) ?: return
            purchases.getCustomerInfo(
                CacheFetchPolicy.FETCH_CURRENT,
                object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: CustomerInfo) {
                        call.flowFinished.complete(
                            CheckpointRun(
                                CheckpointFlowOutcome.Finished(customerInfo, reportedPurchase),
                                backedOut = false,
                            ),
                        )
                    }

                    override fun onError(error: PurchasesError) {
                        call.flowFinished.complete(
                            CheckpointRun(CheckpointFlowOutcome.Error(error), backedOut = false),
                        )
                    }
                },
            )
        }
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
        if (!claim(call)) return blockedByPresentedFlow
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

    // The single pendingCall field is the one-presentation-at-a-time gate; claiming fails while any
    // presentation, SDK- or app-owned, is live.
    private fun claim(call: PendingCall): Boolean =
        synchronized(this) { (pendingCall == null).also { if (it) pendingCall = call } }

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
