package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Hands a paywall's errors to the app's [PaywallErrorPresenter] when its options carry one, and turns the
 * presenter's first report into what the paywall does next through [Host]. Without a presenter, a purchase or
 * restore error shows the SDK's dialog and an error state is left to the paywall to render. A purchase the user
 * cancelled is not shown either way.
 *
 * Error states are taken from [state] rather than from where they are set, once each, so a window re-presented
 * after a configuration change, which collects the same state again, does not ask the presenter twice. The
 * presenter is called on [scope]'s thread and its report is brought back to it. A report only counts for the
 * presentation it was asked in: one that arrives after the paywall was dismissed and presented again is ignored,
 * and so is one for an error that a newer error has since replaced.
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

        /** Goes to the flow's previous step, like system back; false when there is none. */
        fun navigateBack(): Boolean

        /** True once the flow has ended some other way (e.g. the user closed the paywall meanwhile). */
        val flowEnded: Boolean

        /** Changes every time a presentation ends, so a report can be tied to the presentation that asked for it. */
        val presentationGeneration: Int
    }

    private var current: FirstReportCompletion? = null

    init {
        scope.launch { state.filterIsInstance<PaywallState.Error>().collect { onErrorState(it) } }
    }

    // The paywall stays as it is, interactive, and the app's first report decides whether the flow goes on.
    fun onActionError(error: PurchasesError) {
        // The user's own decision, not something to show. Store purchases never report one here, but an app's
        // purchase logic may return it as an error.
        if (error.code == PurchasesErrorCode.PurchaseCancelledError) return
        val presenter = presenter()
        if (presenter == null) {
            host.showErrorDialog(error)
            return
        }
        present(presenter, error, flowCanContinue = true, onFailure = { host.showErrorDialog(error) }) {
            when {
                it == ErrorPresenter.Completion.Result.Retry -> Unit
                // System back: a previous step when the flow has one, leaving the flow otherwise.
                it == ErrorPresenter.Completion.Result.NavigateBack && !host.flowEnded && host.navigateBack() -> Unit
                else -> leaveFlow(it, paywallResult = null)
            }
        }
    }

    private fun onErrorState(errorState: PaywallState.Error) {
        val presenter = presenter() ?: return
        val paywallResult = errorState.toPaywallResult()
        present(
            presenter,
            paywallResult.error,
            flowCanContinue = false,
            onFailure = { host.closePaywall(paywallResult, PaywallDismissReason.CLOSE) },
        ) {
            // The paywall may have moved past this error meanwhile (e.g. a retried load succeeded); a report about
            // an error that is no longer on screen must not close what replaced it.
            if (state.value == errorState) leaveFlow(it, paywallResult)
        }
    }

    // Retry lands here only when the flow cannot go on. An unknown result goes on too: the hierarchy is closed but
    // not sealed, and passing the checkpoint is the safest reading of a report this code does not know.
    private fun leaveFlow(result: ErrorPresenter.Completion.Result, paywallResult: PaywallResult?) {
        if (host.flowEnded) return
        val navigatedBack = result == ErrorPresenter.Completion.Result.NavigateBack
        val known = navigatedBack ||
            result == ErrorPresenter.Completion.Result.Retry ||
            result == ErrorPresenter.Completion.Result.Continue
        if (!known) Logger.e("Unknown error presenter result '$result'; treating it as Continue.")
        host.closePaywall(
            paywallResult,
            if (navigatedBack) PaywallDismissReason.NAVIGATED_BACK else PaywallDismissReason.CLOSE,
        )
    }

    /**
     * Hands [error] to the app's [presenter] and runs [onResult] with its first report, on [scope]'s thread. The
     * report is held until the presenter has returned: a presenter that throws is logged and [onFailure] takes
     * over alone, whatever it reported before throwing or reports afterwards.
     */
    private fun present(
        presenter: PaywallErrorPresenter,
        error: PurchasesError,
        flowCanContinue: Boolean,
        onFailure: () -> Unit,
        onResult: (ErrorPresenter.Completion.Result) -> Unit,
    ) {
        val generation = host.presentationGeneration
        // A report from another thread is queued behind whatever main is doing, so it is checked again on arrival:
        // the presentation must be the same, and the completion must still be the current one.
        val completion = FirstReportCompletion { sender, result ->
            scope.launch {
                if (host.presentationGeneration == generation && current === sender) onResult(result)
            }
        }
        // Like the SDK's own presenter, a new error replaces the one before it: whatever the app still shows for
        // the earlier one can no longer act on the flow.
        current?.discard()
        current = completion
        try {
            presenter.present(error, flowCanContinue, completion)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e("Error presenter failed: $e")
            completion.discard()
            onFailure()
            return
        }
        completion.release()
    }

    // Delivers the first report once, and only after release(); a report made before then waits for it, and
    // discard() drops it. Reports can come from any thread, the delivery runs outside the lock.
    private class FirstReportCompletion(
        private val onFirst: (sender: FirstReportCompletion, ErrorPresenter.Completion.Result) -> Unit,
    ) : ErrorPresenter.Completion {

        private val lock = Any()
        private var reported = false
        private var released = false
        private var pending: ErrorPresenter.Completion.Result? = null

        override fun complete(result: ErrorPresenter.Completion.Result) {
            val deliverNow = synchronized(lock) {
                if (reported) return
                reported = true
                if (released) true else false.also { pending = result }
            }
            if (deliverNow) onFirst(this, result)
        }

        fun release() {
            val held = synchronized(lock) {
                released = true
                pending.also { pending = null }
            }
            held?.let { onFirst(this, it) }
        }

        fun discard() {
            synchronized(lock) {
                reported = true
                pending = null
            }
        }
    }
}
