package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter.Completion.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.UUID

/**
 * The SDK's own [PaywallPresenter], used for a matched offering when neither the call nor the [Purchases]
 * instance supplies one: presents the offering's configured paywall, falling back to the default paywall, in a
 * [CheckpointWorkflowPresenter] window over the current activity. It goes through the same contract an app
 * presenter does, so it only reports how the user left the paywall; what they obtained is read from the synced
 * customer info like for any other presenter. One instance per presentation, and the host of its own window. Like
 * an app presenter's UI, the window is not taken down when the checkpoint call is abandoned: the user closes it,
 * and its report is ignored.
 */
internal class DefaultPaywallPresenter(
    private val purchases: Purchases,
    private val windowFactory: (callId: String, host: CheckpointPresentationHost) -> CheckpointWorkflowPresenter =
        { callId, host -> CheckpointWorkflowPresenter(callId, host) },
) : PaywallPresenter, CheckpointPresentationHost {

    private lateinit var params: PaywallPresenter.Params
    private var completion: PaywallPresenter.Completion? = null

    override fun present(params: PaywallPresenter.Params, completion: PaywallPresenter.Completion) {
        val activity = purchases.currentActivity ?: throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.ConfigurationError,
                "Cannot present checkpoint paywall: no started Activity found.",
            ),
        )
        this.params = params
        this.completion = completion
        val window = windowFactory(UUID.randomUUID().toString(), this)
        try {
            window.show(activity)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // A failed show may have left the window observing the activity; nothing will ever report for it.
            window.abandon()
            throw e
        }
    }

    // Host of a single window, so the call id is not checked. The presentation is gone once the window has
    // reported, so a stale re-present finds nothing and tears itself down.
    override fun presentation(callId: String): CheckpointPresentation? = completion?.let {
        CheckpointPresentation(CheckpointFlowContent.OfferingFlow(params.offering), params.customVariables)
    }

    override fun recordOutcome(callId: String, outcome: CheckpointFlowOutcome) = Unit

    // The window goes away on the report, like an app presenter's UI, and the checkpoint resolves after it.
    override fun onPresentationFinished(callId: String, navigatedBack: Boolean, finishPresentation: () -> Unit) {
        finishPresentation()
        complete(if (navigatedBack) Result.NavigatedBack else Result.Closed)
    }

    override fun onPresentationFailed(callId: String, error: PurchasesError) {
        Logger.e("Checkpoint paywall could not be kept on screen; treating it as closed: $error")
        complete(Result.Closed)
    }

    private fun complete(result: Result) {
        val completion = this.completion ?: return
        this.completion = null
        completion.complete(result)
    }
}
