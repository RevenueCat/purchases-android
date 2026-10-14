package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter.Completion.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.UUID

/**
 * The SDK's own [PaywallPresenter], used for a matched offering when neither the call nor the [Purchases]
 * instance supplies one: presents the offering's configured paywall, falling back to the default paywall, in a
 * [CheckpointWorkflowPresenter] window over the current activity. It goes through the same contract an app
 * presenter does, and configures the paywall with the same [PaywallOptions] an app embedding it would (offering,
 * custom variables, dismiss button, [PaywallListener]), so what it does is available to app presenters. The two
 * exceptions are learning how the paywall was dismissed, which the paywall only reports internally, and handing
 * the paywall's errors to the checkpoint's [ErrorPresenter], which the paywall only offers internally.
 *
 * It reports how the user left the paywall, except that a purchase, or a restore that granted an entitlement the
 * user did not hold when the paywall opened, continues the flow whatever closed the window; a granting restore
 * also closes the paywall, like a purchase does. What the user obtained is still read from the synced customer
 * info, like for any other presenter. A paywall that could not load, or a window that could not be kept on
 * screen, reports [Result.Failed] unless the user obtained something first: the checkpoint is not passed. One
 * instance per presentation, and the host of its own window. Like an app presenter's UI, the window is not taken
 * down when the checkpoint call is abandoned: the user closes it, and its report is ignored.
 */
internal class DefaultPaywallPresenter(
    private val purchases: Purchases,
    private val errorPresenter: ErrorPresenter,
    private val windowFactory: (
        callId: String,
        host: CheckpointPresentationHost,
        presentationMode: FlowPresentationMode,
    ) -> CheckpointWorkflowPresenter = { callId, host, mode -> CheckpointWorkflowPresenter(callId, host, mode) },
) : PaywallPresenter, CheckpointPresentationHost {

    private lateinit var params: PaywallPresenter.Params
    private var completion: PaywallPresenter.Completion? = null
    private var activeEntitlementsBefore: Set<String>? = null
    private var obtained = false
    private var failure: PurchasesError? = null

    // The dismissal wiring of the window currently on screen; a re-present after a configuration change replaces it.
    private var dismiss: ((navigatedBack: Boolean) -> Unit)? = null

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
        val window = windowFactory(UUID.randomUUID().toString(), this, params.presentationMode)
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
    override fun paywallOptions(callId: String, dismiss: (navigatedBack: Boolean) -> Unit): PaywallOptions? =
        completion?.let {
            this.dismiss = dismiss
            PaywallOptions.Builder(dismissRequest = { dismiss(false) })
                // What an app embedding the paywall cannot do: the paywall handles system back itself and only
                // reports the reason through this internal channel, and a paywall that could not load carries the
                // error here once its error UI is done. Everything else here is public API.
                .setDismissRequestWithExitOffering { _, result, reason ->
                    (result as? PaywallResult.Error)?.let { failure = it.error }
                    dismiss(reason == PaywallDismissReason.NAVIGATED_BACK)
                }
                .setErrorPresenter(errorPresenter.forCheckpoint(params.checkpointIdentifier, params.customVariables))
                .setOffering(params.offering)
                .setShouldDisplayDismissButton(true)
                .setCustomVariables(params.customVariables)
                .setListener(listener)
                .build()
        }

    // A grant is sticky: a later error or web checkout must not erase it, and neither changes how leaving is
    // reported, so they are not observed. A restore that granted nothing changes nothing either.
    private val listener = object : PaywallListener {
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            if (completion == null) return
            obtained = true
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            if (completion == null) return
            if (customerInfo.grantsNewEntitlements(activeEntitlementsBefore)) {
                obtained = true
                dismiss?.invoke(false)
            }
        }
    }

    // The window goes away on the report, like an app presenter's UI, and the checkpoint resolves after it.
    override fun onPresentationFinished(callId: String, navigatedBack: Boolean, finishPresentation: () -> Unit) {
        finishPresentation()
        val failure = this.failure
        complete(
            when {
                obtained -> Result.Continued
                failure != null -> Result.Failed(failure)
                navigatedBack -> Result.NavigatedBack
                else -> Result.Closed
            },
        )
    }

    // The window is gone and the paywall cannot go on, so the error is presented like the paywall's own would
    // be, and the report follows the app's error UI.
    override fun onPresentationFailed(callId: String, error: PurchasesError) {
        Logger.e("Checkpoint paywall could not be kept on screen: $error")
        if (obtained) {
            complete(Result.Continued)
            return
        }
        if (completion == null) return
        errorPresenter.presentOnce(
            ErrorPresenter.Params(
                error,
                params.checkpointIdentifier,
                params.customVariables,
                ErrorPresentationStage.DURING_FLOW_PRESENTATION,
            ),
        ) { complete(Result.Failed(error)) }
    }

    private fun complete(result: Result) {
        val completion = this.completion ?: return
        this.completion = null
        completion.complete(result)
    }
}
