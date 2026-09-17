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
 * presenter does: it reports how the user left the paywall, except that a purchase, or a restore that granted an
 * entitlement the user did not hold when the paywall opened, continues the flow whatever closed the window. What
 * the user obtained is still read from the synced customer info, like for any other presenter. One instance per
 * presentation, and the host of its own window. Like an app presenter's UI, the window is not taken down when the
 * checkpoint call is abandoned: the user closes it, and its report is ignored.
 */
internal class DefaultPaywallPresenter(
    private val purchases: Purchases,
    private val windowFactory: (callId: String, host: CheckpointPresentationHost) -> CheckpointWorkflowPresenter =
        { callId, host -> CheckpointWorkflowPresenter(callId, host) },
) : PaywallPresenter, CheckpointPresentationHost {

    private lateinit var params: PaywallPresenter.Params
    private var completion: PaywallPresenter.Completion? = null
    private var window: CheckpointWorkflowPresenter? = null
    private var activeEntitlementsBefore: Set<String>? = null
    private var obtained = false

    override fun present(params: PaywallPresenter.Params, completion: PaywallPresenter.Completion) {
        val activity = purchases.currentActivity ?: throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.ConfigurationError,
                "Cannot present checkpoint paywall: no started Activity found.",
            ),
        )
        this.params = params
        this.completion = completion
        purchases.cachedActiveEntitlementIds { activeEntitlementsBefore = it }
        val window = windowFactory(UUID.randomUUID().toString(), this)
        this.window = window
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

    // A grant is sticky: a later error or web checkout must not erase it. A restore that granted nothing changes
    // nothing, so how the user then leaves decides; one that did grant closes the flow like a purchase does.
    override fun recordOutcome(callId: String, outcome: CheckpointFlowOutcome) {
        if (completion == null) return
        when (outcome) {
            is CheckpointFlowOutcome.Purchased -> obtained = true
            is CheckpointFlowOutcome.Restored ->
                if (outcome.customerInfo.obtainedEntitlements(activeEntitlementsBefore).isNotEmpty()) {
                    obtained = true
                    window?.dismiss()
                }
            else -> Unit
        }
    }

    // The window goes away on the report, like an app presenter's UI, and the checkpoint resolves after it.
    override fun onPresentationFinished(callId: String, navigatedBack: Boolean, finishPresentation: () -> Unit) {
        finishPresentation()
        complete(
            when {
                obtained -> Result.Continued
                navigatedBack -> Result.NavigatedBack
                else -> Result.Closed
            },
        )
    }

    override fun onPresentationFailed(callId: String, error: PurchasesError) {
        Logger.e("Checkpoint paywall could not be kept on screen; treating it as closed: $error")
        complete(if (obtained) Result.Continued else Result.Closed)
    }

    private fun complete(result: Result) {
        val completion = this.completion ?: return
        this.completion = null
        completion.complete(result)
    }
}
