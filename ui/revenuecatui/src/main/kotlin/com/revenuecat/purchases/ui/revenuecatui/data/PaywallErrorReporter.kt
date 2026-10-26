package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointFailureKind
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.HeldCompletion
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Hands a paywall's errors to the app's [PaywallErrorPresenter] when its options carry one, and turns the
 * presenter's completion into what the paywall does next through [Host]: a purchase or restore error leaves the
 * paywall where it is, an error state closes it with the error. Without a presenter, a purchase or restore error
 * shows the SDK's dialog and an error state is left to the paywall to render. A purchase the user cancelled is not
 * shown either way.
 *
 * Error states are taken from [state] rather than from where they are set, once each, so a window re-presented
 * after a configuration change, which collects the same state again, does not ask the presenter twice. The
 * presenter is called on [scope]'s thread and its completion is brought back to it. A completion only counts for
 * the presentation it was asked in: one that arrives after the paywall was dismissed and presented again is
 * ignored, and so is one for an error that a newer error has since replaced.
 */
internal class PaywallErrorReporter(
    private val presenter: () -> PaywallErrorPresenter?,
    private val scope: CoroutineScope,
    private val state: StateFlow<PaywallState>,
    private val host: Host,
) {

    internal interface Host {
        /** Shows the SDK's own dialog for a purchase or restore error. */
        fun showErrorDialog(error: PurchasesError)

        /** Ends the flow; [result] carries the error of a paywall that could not be shown. */
        fun closePaywall(result: PaywallResult?, reason: PaywallDismissReason)

        /** True once the flow has ended some other way (e.g. the user closed the paywall meanwhile). */
        val flowEnded: Boolean

        /** Changes every time a presentation ends, so a completion can be tied to the presentation that asked. */
        val presentationGeneration: Int
    }

    private var current: HeldCompletion? = null

    init {
        scope.launch { state.filterIsInstance<PaywallState.Error>().collect { onErrorState(it) } }
    }

    // The paywall stays as it is, interactive, whatever the app shows over it and however long it takes.
    fun onActionError(error: PurchasesError) {
        // The user's own decision, not something to show. Store purchases never report one here, but an app's
        // purchase logic may return it as an error.
        if (error.code == PurchasesErrorCode.PurchaseCancelledError) return
        val presenter = presenter()
        if (presenter == null) {
            host.showErrorDialog(error)
            return
        }
        present(
            presenter,
            error,
            CheckpointFailureKind.FLOW_ACTION_FAILED,
            onFailure = { host.showErrorDialog(error) },
            onComplete = {},
        )
    }

    // The paywall has nothing to show, so the app's error UI is all the user sees of this flow: once it is done,
    // the flow ends with the error, back where the user was before it.
    private fun onErrorState(errorState: PaywallState.Error) {
        val presenter = presenter() ?: return
        val paywallResult = errorState.toPaywallResult()
        present(
            presenter,
            paywallResult.error,
            CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN,
            onFailure = { host.closePaywall(paywallResult, PaywallDismissReason.CLOSE) },
        ) {
            // The paywall may have moved past this error meanwhile (e.g. a retried load succeeded); a completion
            // for an error that is no longer on screen must not close what replaced it.
            if (state.value == errorState && !host.flowEnded) {
                host.closePaywall(paywallResult, PaywallDismissReason.CLOSE)
            }
        }
    }

    /**
     * Hands [error] to the app's [presenter] and runs [onComplete] on its first completion, on [scope]'s thread.
     * The completion is held until the presenter has returned: a presenter that throws is logged and [onFailure]
     * takes over alone, whether it completed before throwing or completes afterwards.
     */
    private fun present(
        presenter: PaywallErrorPresenter,
        error: PurchasesError,
        kind: CheckpointFailureKind,
        onFailure: () -> Unit,
        onComplete: () -> Unit,
    ) {
        val generation = host.presentationGeneration
        // A completion from another thread is queued behind whatever main is doing, so it is checked again on
        // arrival: the presentation must be the same, and the completion must still be the current one.
        val completion = HeldCompletion { sender ->
            scope.launch {
                if (host.presentationGeneration == generation && current === sender) onComplete()
            }
        }
        // A new error supersedes the one before it: whatever the app still shows for the earlier one (the
        // checkpoint queue hands it the newer error only once that is done) can no longer act on the flow.
        current?.discard()
        current = completion
        try {
            presenter.present(error, kind, completion)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e("Error presenter failed: $e")
            completion.discard()
            onFailure()
            return
        }
        completion.release()
    }
}
