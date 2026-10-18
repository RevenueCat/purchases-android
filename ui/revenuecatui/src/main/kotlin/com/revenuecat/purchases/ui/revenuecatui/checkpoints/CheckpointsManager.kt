package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PresentationSlot.PendingCall
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
 * Everything the manager's [CheckpointWorkflowPresenter] needs to build a workflow paywall, and the manager to
 * resolve the flow once it ends, read in a single pass so it can't observe a call that was taken between two
 * accessors.
 */
internal class CheckpointPresentation(
    val purchases: Purchases,
    val workflow: CheckpointResolution.MatchedWorkflow,
    val checkpointIdentifier: String,
    val customVariables: Map<String, CustomVariableValue>,
    val errorPresenter: PaywallErrorPresenter?,
    val purchaseLogic: PaywallPurchaseLogic?,
)

/**
 * What a checkpoint run produced: the terminal [flowOutcome] of whatever it presented (null when nothing was),
 * and [backedOut], true when the presented flow went away because the user navigated back (system back, or a
 * navigate-back action on a workflow's first step) without purchasing or restoring.
 *
 * [failed] is true when the checkpoint could not be served before anything was on screen: the failure went to
 * the error presenter and the run is not reported as passed. [blockedByPresentedFlow] is true when the checkpoint
 * resolved to a flow that was not presented because another checkpoint flow was already on screen. That earlier
 * call is the one the app is waiting on, so a blocked run is not reported as passed either.
 */
internal class CheckpointRun(
    val flowOutcome: CheckpointFlowOutcome?,
    val backedOut: Boolean,
    val blockedByPresentedFlow: Boolean = false,
    val failed: Boolean = false,
    // Releases whatever the SDK still has on screen for this run; invoked once the app has been told.
    val finishPresentation: () -> Unit = {},
)

/**
 * Runs a checkpoint hit end to end: asks the core module what the checkpoint resolves to, and presents the
 * resolved flow: a workflow through a [CheckpointWorkflowPresenter] this manager hosts, an offering through a
 * [PaywallPresenter], the app's or the SDK's own [DefaultPaywallPresenter]. The errors of a flow the SDK presents
 * go to an [ErrorPresenter], the app's or the SDK's own [DefaultErrorPresenter]. Owns the checkpoint policy: how
 * outcomes supersede each other and how a finished or failed presentation resolves the suspended [runCheckpoint]
 * call. The pending call itself, and the one-presentation-at-a-time rule, live in [PresentationSlot]; runs that
 * present nothing never claim that slot.
 *
 * There is one instance per [Purchases] instance, held in its `internalCpManagerSlot` and reached through
 * [checkpointsManager], so a reconfigured SDK starts with a free presentation slot. A workflow that is already
 * on screen keeps reporting to the manager that presented it, exactly once, even if the SDK is reconfigured
 * underneath it.
 */
@Suppress("TooManyFunctions")
internal class CheckpointsManager(
    // Owns the coroutines behind the callback-based gate API, so an un-awaited checkpoint lives and dies with
    // the Purchases instance holding this manager rather than with any caller scope. The plain main dispatcher,
    // not the immediate one: the error presentation queue relies on launch posting, so a presenter is never asked
    // to present again from inside its own completion.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val presenterFactory: (
        callId: String,
        manager: CheckpointsManager,
        presentationMode: FlowPresentationMode,
    ) -> CheckpointWorkflowPresenter = { callId, manager, mode -> CheckpointWorkflowPresenter(callId, manager, mode) },
    private val defaultPresenterFactory: (
        purchases: Purchases,
        errorPresenter: ErrorPresenter,
        purchaseLogic: PaywallPurchaseLogic?,
    ) -> DefaultPaywallPresenter = { purchases, errorPresenter, purchaseLogic ->
        DefaultPaywallPresenter(purchases, errorPresenter, purchaseLogic)
    },
    private val defaultErrorPresenterFactory: (Purchases) -> DefaultErrorPresenter = { DefaultErrorPresenter(it) },
) : CheckpointPresentationHost {

    @get:Synchronized
    @set:Synchronized
    var paywallPresenter: PaywallPresenter? = null

    @get:Synchronized
    @set:Synchronized
    var errorPresenter: ErrorPresenter? = null

    private val slot = PresentationSlot()
    private val errorPresentationQueue = ErrorPresentationQueue(scope)

    private val nothingPresented = CheckpointRun(flowOutcome = null, backedOut = false)
    private val blockedByPresentedFlow =
        CheckpointRun(flowOutcome = null, backedOut = false, blockedByPresentedFlow = true)
    private val failed = CheckpointRun(flowOutcome = null, backedOut = false, failed = true)

    /**
     * Runs a checkpoint hit: resolves the checkpoint and presents whatever it resolved to, suspending until the
     * presented flow finishes. Never throws: a failure to resolve or present is logged, handed to the error
     * presenter, and comes back as a failed run once the app's error UI is done. Completion is reported by
     * [checkpoint].
     */
    suspend fun runCheckpoint(
        purchases: Purchases,
        identifier: String,
        params: CheckpointParams?,
    ): CheckpointRun = withContext(Dispatchers.Main) {
        val customVariables = (params ?: CheckpointParams {}).customVariables
        val presenter = params?.paywallPresenter ?: paywallPresenter
        // Resolved lazily: an app paywall presenter owns its own errors and may never need one. Whichever presenter
        // it is, the queue hands it one error at a time, and the SDK's own stands in for an app presenter that throws.
        val sdkErrorPresenter = { defaultErrorPresenterFactory(purchases) }
        val errorPresenter: () -> ErrorPresenter = {
            errorPresentationQueue.serialized(
                (params?.errorPresenter ?: errorPresenter)?.withFallback(sdkErrorPresenter) ?: sdkErrorPresenter(),
            )
        }
        val presentationMode = resolvePresentationMode(params?.presentationMode)
        val purchaseLogic = params?.purchaseLogic
        val presentFailure: suspend (PurchasesError) -> CheckpointRun =
            { error -> presentFailure(errorPresenter(), identifier, customVariables, error) }
        if (!CheckpointIdentifierValidator.isValid(identifier)) {
            val message = CheckpointIdentifierValidator.invalidIdentifierLogMessage(identifier)
            Logger.e(message)
            return@withContext presentFailure(PurchasesError(PurchasesErrorCode.ConfigurationError, message))
        }
        val resolution = try {
            purchases.internalResolveCp(
                identifier,
                customVariables.mapValues { (_, value) -> value.asRulesDimensionValue },
            )
        } catch (e: PurchasesException) {
            Logger.e("Checkpoint '$identifier' could not be resolved: ${e.error}")
            return@withContext presentFailure(e.error)
        }
        try {
            when (resolution) {
                is CheckpointResolution.MatchedOffering -> presentOffering(
                    purchases,
                    PaywallPresenter.Params(resolution.offering, identifier, customVariables, presentationMode),
                    presenter ?: defaultPresenterFactory(purchases, errorPresenter(), purchaseLogic),
                    errorPresenter,
                )
                is CheckpointResolution.MatchedWorkflow -> present(
                    purchases,
                    identifier,
                    resolution,
                    customVariables,
                    errorPresenter(),
                    presentationMode,
                    purchaseLogic,
                )
                is CheckpointResolution.NoAction -> when (resolution.reason) {
                    CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE -> {
                        val message = "The configuration needed to serve checkpoint '$identifier' could not be read."
                        Logger.e(message)
                        presentFailure(PurchasesError(PurchasesErrorCode.ConfigurationError, message))
                    }
                    else -> nothingPresented
                }
            }
        } catch (e: PurchasesException) {
            // Already logged by presentationError.
            presentFailure(e.error)
        }
    }

    // The user is where they were before the checkpoint and stays there whatever the app's error UI does, so
    // the run only waits for that UI to be done.
    private suspend fun presentFailure(
        errorPresenter: ErrorPresenter,
        identifier: String,
        customVariables: Map<String, CustomVariableValue>,
        error: PurchasesError,
    ): CheckpointRun {
        val done = CompletableDeferred<Unit>()
        errorPresenter.presentOnce(
            ErrorPresenter.Params(
                error,
                identifier,
                customVariables,
                CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN,
            ),
        ) { done.complete(Unit) }
        done.await()
        return failed
    }

    /**
     * The checkpoint API: runs the checkpoint and invokes [callback] with what the user obtained, at most once
     * and on the main thread. The callback is skipped when the checkpoint could not be served or its flow ended
     * in an error, when the user backed out of the presented flow, and when the checkpoint resolved to a flow
     * while another checkpoint flow was already on screen. Never throws.
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
                    run.failed || run.flowOutcome is CheckpointFlowOutcome.Error ->
                        Logger.d("Checkpoint '$identifier': it could not be served, so the callback is not invoked.")
                    run.backedOut ->
                        Logger.d("Checkpoint '$identifier': the user backed out, so the callback is not invoked.")
                    else -> callback.onCheckpointPassed(CheckpointResult(run.toResult(activeEntitlementsBefore)))
                }
            } finally {
                run.finishPresentation()
            }
        }
    }

    // Null for a call whose flow a PaywallPresenter shows: that UI is not the manager's.
    fun presentation(callId: String): CheckpointPresentation? =
        slot.with(callId) { it.presentation }

    // Direct dismissals (a completed purchase or restore) carry no reason and count as a close; everything else
    // reports one, and a paywall that could not load carries the error as its result when its error UI is done.
    // A purchase the app's own logic completed is reported here too, synced, rather than to the listener. The
    // exit offering, if any, is not presented for checkpoints.
    override fun paywallOptions(callId: String, dismiss: (navigatedBack: Boolean) -> Unit): PaywallOptions? =
        presentation(callId)?.let { presentation ->
            PaywallOptions.Builder(dismissRequest = { dismiss(false) })
                .setDismissRequestWithExitOffering { _, result, reason ->
                    when (result) {
                        is PaywallResult.Error -> recordOutcome(callId, CheckpointFlowOutcome.Error(result.error))
                        is PaywallResult.Purchased -> recordOutcome(
                            callId,
                            CheckpointFlowOutcome.Purchased(result.customerInfo, storeTransaction = null),
                        )
                        else -> Unit
                    }
                    dismiss(reason == PaywallDismissReason.NAVIGATED_BACK)
                }
                .setErrorPresenter(presentation.errorPresenter)
                .setCustomVariables(presentation.customVariables)
                .setListener(OutcomeListener(callId))
                .setPurchaseLogic(
                    presentation.purchaseLogic?.let { purchaseLogic ->
                        ObservedPurchaseLogic(purchaseLogic) {
                            recordOutcome(callId, CheckpointFlowOutcome.CompletedByApp)
                        }
                    },
                )
                .injectedWorkflow(
                    presentation.workflow.workflow,
                    presentation.workflow.offerings,
                    presentation.workflow.uiConfig,
                    presentation.workflow.traceId,
                    presentation.workflow.workflowBlobRef,
                    presentation.checkpointIdentifier,
                )
                .build()
        }

    private inner class OutcomeListener(private val callId: String) : PaywallListener {
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            recordOutcome(callId, CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
        }

        // A restore that granted nothing the user didn't already hold is not an outcome: how the user then
        // leaves decides. A gone call reads as "unknown before", and recording into it is a no-op anyway.
        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            if (customerInfo.grantsNewEntitlements(slot.with(callId) { it.activeEntitlementsBefore })) {
                recordOutcome(callId, CheckpointFlowOutcome.Restored(customerInfo))
            }
        }

        // A failed purchase or restore is not observed: the paywall stays up and how the user leaves decides.
        override fun onWebCheckoutOpened() {
            recordOutcome(callId, CheckpointFlowOutcome.WebCheckoutOpened)
        }
    }

    // A purchase or granting restore is what the user went through for, so a later error, web checkout or
    // dismissal must not erase it. A later purchase or restore still replaces it with newer CustomerInfo.
    fun recordOutcome(callId: String, outcome: CheckpointFlowOutcome) {
        slot.with(callId) {
            if (!it.outcome.isObtained || outcome.isObtained) {
                it.outcome = outcome
            }
        }
    }

    // A recorded purchase or restore means the user went through, however the window went away: checkpoint
    // paywalls don't auto-dismiss on restore, so a user who restored and then backed out still went through. A
    // paywall that went away without reporting anything was dismissed.
    override fun onPresentationFinished(
        callId: String,
        navigatedBack: Boolean,
        finishPresentation: () -> Unit,
    ) {
        val finished = slot.take(callId) ?: run {
            finishPresentation()
            return
        }
        val recorded = finished.outcome
        if (recorded is CheckpointFlowOutcome.CompletedByApp) {
            syncAppCompletedFlow(finished, finishPresentation)
            return
        }
        finished.flowFinished.complete(
            CheckpointRun(
                recorded ?: CheckpointFlowOutcome.Dismissed,
                backedOut = navigatedBack && !recorded.isObtained,
                finishPresentation = finishPresentation,
            ),
        )
    }

    // Like onPresentationFinished, for a presentation that failed: a purchase or granting restore the paywall
    // already reported (e.g. before the configuration change) still wins. Otherwise the window is gone and the
    // flow cannot go on, so the error is presented and the run ends with it once the app's error UI is done.
    override fun onPresentationFailed(callId: String, error: PurchasesError) {
        val failed = slot.take(callId) ?: return
        val obtained = failed.outcome.takeIf { it.isObtained }
        when {
            obtained is CheckpointFlowOutcome.CompletedByApp -> syncAppCompletedFlow(failed, finishPresentation = {})
            obtained != null -> failed.flowFinished.complete(CheckpointRun(obtained, backedOut = false))
            else -> {
                val complete = {
                    failed.flowFinished.complete(
                        CheckpointRun(CheckpointFlowOutcome.Error(error), backedOut = false),
                    )
                    Unit
                }
                failed.presentation?.errorPresenter
                    ?.presentOnce(error, CheckpointFailureKind.FLOW_ACTION_FAILED, complete)
                    ?: complete()
            }
        }
    }

    // The app's own logic completed a purchase or restore that the paywall never reported with the customer's
    // information: a restore, or a purchase the user left before the paywall had synced it. What the user obtained
    // is read the way it is for an app presenter: the window goes down first, as the app's UI would be gone, then
    // the SDK syncs the store purchases and refreshes the customer's information.
    private fun syncAppCompletedFlow(call: PendingCall, finishPresentation: () -> Unit) {
        finishPresentation()
        val presentation = call.presentation ?: run {
            call.flowFinished.complete(CheckpointRun(CheckpointFlowOutcome.Dismissed, backedOut = false))
            return
        }
        syncAndComplete(call, presentation.purchases, presentation.checkpointIdentifier) { error, onComplete ->
            presentation.errorPresenter
                ?.presentOnce(error, CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED, onComplete)
                ?: onComplete()
        }
    }

    // FETCH_CURRENT posts the store purchases the SDK hasn't seen yet (e.g. made through the app's own billing
    // client) before fetching, so the outcome reflects whatever the flow sold. A sync that fails goes to
    // [presentSyncFailure]: the user has left the flow and goes on either way, once the app's error UI is done.
    private fun syncAndComplete(
        call: PendingCall,
        purchases: Purchases,
        checkpointIdentifier: String,
        presentSyncFailure: (error: PurchasesError, onComplete: () -> Unit) -> Unit,
    ) {
        purchases.getCustomerInfo(
            CacheFetchPolicy.FETCH_CURRENT,
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    call.flowFinished.complete(
                        CheckpointRun(CheckpointFlowOutcome.Finished(customerInfo), backedOut = false),
                    )
                }

                override fun onError(error: PurchasesError) {
                    Logger.e(
                        "Checkpoint '$checkpointIdentifier': the purchases made during the flow could not be " +
                            "synced: $error",
                    )
                    presentSyncFailure(error) {
                        call.flowFinished.complete(
                            CheckpointRun(CheckpointFlowOutcome.SyncFailed, backedOut = false),
                        )
                    }
                }
            },
        )
    }

    /**
     * Presents a matched offering, described by [params] (resolved presentation mode included), through
     * [presenter]: the call's own, else the registered [paywallPresenter], else the SDK's own
     * [DefaultPaywallPresenter]. Either way the presentation claims the same one-presentation-at-a-time slot as
     * workflows and resolves through its completion's first report, after the SDK has synced the store purchases
     * made during it. A sync that fails is handed to [errorPresenter]: that step is the SDK's, whoever presented.
     */
    private suspend fun presentOffering(
        purchases: Purchases,
        params: PaywallPresenter.Params,
        presenter: PaywallPresenter,
        errorPresenter: () -> ErrorPresenter,
    ): CheckpointRun {
        val call = PendingCall(
            callId = UUID.randomUUID().toString(),
            presentation = null,
            activeEntitlementsBefore = null,
            flowFinished = CompletableDeferred(),
        )
        if (!slot.claim(call)) return blockedByPresentedFlow
        try {
            presenter.present(
                params,
                PresenterCompletion(call.callId, purchases, params, errorPresenter),
            )
            return call.flowFinished.await()
        } catch (e: CancellationException) {
            abandon(call.callId)
            throw e
        } catch (e: PurchasesException) {
            abandon(call.callId)
            presentationError(PurchasesErrorCode.ConfigurationError, "Paywall presenter failed: ${e.error}")
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            abandon(call.callId)
            presentationError(PurchasesErrorCode.ConfigurationError, "Paywall presenter failed: $e")
        }
    }

    // Routes an app-owned presentation's report back to its pending call. slot.take() removes the call, so only the
    // first report wins and the one-presentation slot is released with it, before the sync that follows a
    // continued or closed report: the app's UI is already gone, so a new checkpoint may present meanwhile.
    private inner class PresenterCompletion(
        private val callId: String,
        private val purchases: Purchases,
        private val params: PaywallPresenter.Params,
        private val errorPresenter: () -> ErrorPresenter,
    ) : PaywallPresenter.Completion {

        override fun complete(result: PaywallPresenter.Completion.Result) {
            when (result) {
                PaywallPresenter.Completion.Result.Continued,
                PaywallPresenter.Completion.Result.Closed,
                -> finished()
                PaywallPresenter.Completion.Result.NavigatedBack -> navigatedBack()
                is PaywallPresenter.Completion.Result.Failed -> failed(result.error)
                // The hierarchy is closed but not sealed; a result this code doesn't know is the safest thing it
                // can be: the user left without purchasing and passes the checkpoint.
                else -> {
                    Logger.e("Unknown paywall presenter result '$result'; treating it as closed.")
                    finished()
                }
            }
        }

        private fun navigatedBack() {
            slot.take(callId)?.let { call ->
                call.flowFinished.complete(
                    CheckpointRun(CheckpointFlowOutcome.Dismissed, backedOut = true),
                )
            }
        }

        // The SDK's own paywall already presented the error; nothing to sync, the checkpoint is not passed.
        private fun failed(error: PurchasesError) {
            slot.take(callId)?.let { call ->
                call.flowFinished.complete(
                    CheckpointRun(CheckpointFlowOutcome.Error(error), backedOut = false),
                )
            }
        }

        private fun finished() {
            val call = slot.take(callId) ?: return
            syncAndComplete(call, purchases, params.checkpointIdentifier, ::presentSyncFailure)
        }

        private fun presentSyncFailure(error: PurchasesError, onComplete: () -> Unit) =
            errorPresenter().presentOnce(
                ErrorPresenter.Params(
                    error,
                    params.checkpointIdentifier,
                    params.customVariables,
                    CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED,
                ),
                onComplete,
            )
    }

    @Suppress("LongParameterList")
    private suspend fun present(
        purchases: Purchases,
        identifier: String,
        resolution: CheckpointResolution.MatchedWorkflow,
        customVariables: Map<String, CustomVariableValue>,
        errorPresenter: ErrorPresenter,
        presentationMode: FlowPresentationMode,
        purchaseLogic: PaywallPurchaseLogic?,
    ): CheckpointRun {
        val activity = purchases.currentActivity ?: presentationError(
            PurchasesErrorCode.ConfigurationError,
            "Cannot present checkpoint workflow: no started Activity found.",
        )
        val call = PendingCall(
            callId = UUID.randomUUID().toString(),
            presentation = CheckpointPresentation(
                purchases = purchases,
                workflow = resolution,
                checkpointIdentifier = identifier,
                customVariables = customVariables,
                errorPresenter = errorPresenter.forCheckpoint(identifier, customVariables),
                purchaseLogic = purchaseLogic,
            ),
            activeEntitlementsBefore = cachedActiveEntitlementIds(purchases),
            flowFinished = CompletableDeferred(),
        )
        if (!slot.claim(call)) return blockedByPresentedFlow
        try {
            val presenter = presenterFactory(call.callId, this, presentationMode)
            slot.with(call.callId) { it.presenter = presenter }
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

    // Counterpart of onPresentationFinished for calls that fail or get cancelled before the presented flow
    // reports: releases the slot and takes an orphaned workflow window down with the caller that asked for it. A
    // paywall presenter's UI, the SDK's own included, is left to the user: its report will find no call and be
    // ignored. Always runs on the main thread, inside the checkpoint() dispatch.
    private fun abandon(callId: String) {
        slot.take(callId)?.presenter?.abandon()
    }

    // DEFAULT stands for whatever the SDK presents by default, which today is the sheet; presenters, the SDK's own
    // included, only ever see a concrete mode.
    private fun resolvePresentationMode(requested: FlowPresentationMode?): FlowPresentationMode =
        when (requested) {
            null, FlowPresentationMode.DEFAULT -> FlowPresentationMode.MODAL_SHEET
            else -> requested
        }

    private val CheckpointFlowOutcome?.isObtained: Boolean
        get() = this is CheckpointFlowOutcome.Purchased ||
            this is CheckpointFlowOutcome.Restored ||
            this is CheckpointFlowOutcome.CompletedByApp

    private fun presentationError(code: PurchasesErrorCode, message: String): Nothing {
        val error = PurchasesError(code, message)
        Logger.e(error.toString())
        throw PurchasesException(error)
    }
}
