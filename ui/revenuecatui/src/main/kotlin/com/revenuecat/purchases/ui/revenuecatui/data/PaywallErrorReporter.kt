package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hands a paywall's errors to the app's [PaywallErrorPresenter] when its options carry one, and turns the
 * presenter's first report into what the paywall does next through [Host]. Without a presenter, a purchase or
 * restore error shows the SDK's dialog and an error state is left to the paywall to render.
 *
 * Error states are taken from [state] rather than from where they are set, once each, so a window re-presented
 * after a configuration change, which collects the same state again, does not ask the presenter twice. The
 * presenter is called on [scope]'s thread and its report is brought back to it.
 */
internal class PaywallErrorReporter(
    private val presenter: () -> PaywallErrorPresenter?,
    private val scope: CoroutineScope,
    state: StateFlow<PaywallState>,
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
    }

    init {
        scope.launch { state.filterIsInstance<PaywallState.Error>().collect { onErrorState(it) } }
    }

    // The paywall stays as it is, interactive, and the app's first report decides whether the flow goes on.
    fun onActionError(error: PurchasesError, source: ErrorPresenter.Source) {
        val presenter = presenter()
        if (presenter == null) {
            host.showErrorDialog(error)
            return
        }
        present(presenter, error, source, flowCanContinue = true, onFailure = { host.showErrorDialog(error) }) {
            when {
                it == ErrorPresenter.Completion.Result.Retry -> Unit
                // System back: a previous step when the flow has one, leaving the flow otherwise.
                it == ErrorPresenter.Completion.Result.NavigatedBack && !host.flowEnded && host.navigateBack() -> Unit
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
            ErrorPresenter.Source.PRESENTATION,
            flowCanContinue = false,
            onFailure = { host.closePaywall(paywallResult, PaywallDismissReason.CLOSE) },
        ) { leaveFlow(it, paywallResult) }
    }

    // Retry lands here only when the flow cannot go on. An unknown result goes on too: the hierarchy is closed but
    // not sealed, and passing the checkpoint is the safest reading of a report this code does not know.
    private fun leaveFlow(result: ErrorPresenter.Completion.Result, paywallResult: PaywallResult?) {
        if (host.flowEnded) return
        val navigatedBack = result == ErrorPresenter.Completion.Result.NavigatedBack
        val known = navigatedBack ||
            result == ErrorPresenter.Completion.Result.Retry ||
            result == ErrorPresenter.Completion.Result.Continued
        if (!known) Logger.e("Unknown error presenter result '$result'; treating it as Continued.")
        host.closePaywall(
            paywallResult,
            if (navigatedBack) PaywallDismissReason.NAVIGATED_BACK else PaywallDismissReason.CLOSE,
        )
    }

    /**
     * Hands [error] to the app's [presenter] and runs [onResult] with its first report, on [scope]'s thread. A
     * presenter that throws is logged and [onFailure] takes over; anything it reports afterwards is ignored.
     */
    @Suppress("LongParameterList")
    private fun present(
        presenter: PaywallErrorPresenter,
        error: PurchasesError,
        source: ErrorPresenter.Source,
        flowCanContinue: Boolean,
        onFailure: () -> Unit,
        onResult: (ErrorPresenter.Completion.Result) -> Unit,
    ) {
        val completion = FirstReportCompletion { result -> scope.launch { onResult(result) } }
        try {
            presenter.present(error, source, flowCanContinue, completion)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e("Error presenter failed: $e")
            completion.discard()
            onFailure()
        }
    }

    private class FirstReportCompletion(
        private val onFirst: (ErrorPresenter.Completion.Result) -> Unit,
    ) : ErrorPresenter.Completion {

        private val reported = AtomicBoolean(false)

        override fun complete(result: ErrorPresenter.Completion.Result) {
            if (reported.compareAndSet(false, true)) onFirst(result)
        }

        fun discard() {
            reported.set(true)
        }
    }
}
