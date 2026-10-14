package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.ui.platform.ComposeView
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.helpers.EDGE_TO_EDGE_WINDOW_THEME
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.applyEdgeToEdge

/**
 * The SDK's own [ErrorPresenter], used when neither the call nor the [Purchases] instance supplies one: shows the
 * error in the SDK's dialog over the current activity and completes once the user acknowledges it. It goes through
 * the same contract an app presenter does and uses nothing an app presenter lacks: the current activity, a dialog
 * of its own, and the completion. One instance per checkpoint call.
 *
 * The dialog shows localized copy written for the customer, chosen from the stage and the error code; the
 * developer's reason is in the logs of whoever reported the error.
 *
 * The dialog dies with its activity, so on a configuration change it is shown again over the next started
 * activity. An activity finishing for real takes the flow with it, so the presentation completes then too.
 *
 * Main-thread only, like the paywall that asks for it.
 */
internal class DefaultErrorPresenter(
    private val purchases: Purchases,
    private val createContent: (Activity, message: String, onAcknowledged: () -> Unit) -> View = ::errorDialogContent,
) : ErrorPresenter {

    private class Presentation(val params: ErrorPresenter.Params, val completion: ErrorPresenter.Completion) {
        var dialog: ComponentDialog? = null
    }

    private var presentation: Presentation? = null
    private var application: Application? = null
    private var host: Activity? = null
    private var awaitingRepresent = false

    override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
        val activity = purchases.currentActivity ?: throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.ConfigurationError,
                "Cannot present checkpoint error: no started Activity found.",
            ),
        )
        // A new error while the previous one is still up replaces it: the flow went on past that one. Through the
        // checkpoint queue this never happens, errors arrive one at a time; the contract still allows it.
        presentation?.let { finish(it) }
        val current = Presentation(params, completion)
        presentation = current
        try {
            show(activity, current)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            presentation = null
            dismissDialog(current)
            teardown()
            throw e
        }
    }

    private fun show(activity: Activity, current: Presentation) {
        if (application == null) {
            application = activity.application.also { it.registerActivityLifecycleCallbacks(lifecycleCallbacks) }
        }
        host = activity
        val dialog = ComponentDialog(activity, EDGE_TO_EDGE_WINDOW_THEME)
        dialog.window?.applyEdgeToEdge()
        // The error dialog inside handles back and outside taps itself, both as an acknowledgement. Cancelling
        // the host window only happens if that dialog is not there, and must never leave a touch-blocking window.
        dialog.setOnCancelListener { acknowledged(current) }
        dialog.setContentView(
            createContent(activity, activity.getString(current.params.messageResource)) { acknowledged(current) },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        current.dialog = dialog
        dialog.show()
    }

    private fun acknowledged(current: Presentation) = finish(current)

    // Only the current presentation finishes, and only once: a stale dialog's acknowledgement is ignored.
    private fun finish(current: Presentation) {
        if (presentation !== current) return
        presentation = null
        dismissDialog(current)
        teardown()
        current.completion.complete()
    }

    private fun dismissDialog(current: Presentation) {
        val dialog = current.dialog ?: return
        current.dialog = null
        try {
            dialog.dismiss()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w("Error dismissing checkpoint error dialog: $e")
        }
    }

    private fun teardown() {
        application?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        application = null
        host = null
        awaitingRepresent = false
    }

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityDestroyed(activity: Activity) {
            if (activity !== host) return
            host = null
            val current = presentation ?: return
            // Always before the host's own window teardown, or the framework reports the dialog as leaked.
            dismissDialog(current)
            if (activity.isChangingConfigurations) {
                awaitingRepresent = true
            } else {
                finish(current)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (!awaitingRepresent || activity.isFinishing) return
            awaitingRepresent = false
            val current = presentation ?: return
            try {
                show(activity, current)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // The flow must never wait on a dialog that is not there.
                Logger.e("Failed to re-present checkpoint error dialog after a configuration change: $e")
                finish(current)
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    // Before the flow nothing could be loaded and after it nothing could be verified, whatever the code; during
    // it the code says what the customer can do about it. A stage this code doesn't know reads as "during".
    private val ErrorPresenter.Params.messageResource: Int
        get() = when {
            error.code == PurchasesErrorCode.NetworkError -> R.string.revenuecatui_checkpoint_error_network
            kind == CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN -> R.string.revenuecatui_checkpoint_error_load
            kind == CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED -> R.string.revenuecatui_checkpoint_error_sync
            else -> error.code.duringFlowMessageResource
        }

    private val PurchasesErrorCode.duringFlowMessageResource: Int
        get() = when (this) {
            PurchasesErrorCode.StoreProblemError -> R.string.revenuecatui_checkpoint_error_store
            PurchasesErrorCode.PurchaseNotAllowedError -> R.string.revenuecatui_checkpoint_error_purchase_not_allowed
            PurchasesErrorCode.ProductNotAvailableForPurchaseError ->
                R.string.revenuecatui_checkpoint_error_product_unavailable
            PurchasesErrorCode.ProductAlreadyPurchasedError -> R.string.revenuecatui_checkpoint_error_already_purchased
            PurchasesErrorCode.PaymentPendingError -> R.string.revenuecatui_checkpoint_error_payment_pending
            else -> R.string.revenuecatui_checkpoint_error_generic
        }
}

// The paywall's own error dialog, unchanged: the Compose AlertDialog brings the scrim and the Material look, so the
// window hosting it only needs to be transparent.
private fun errorDialogContent(activity: Activity, message: String, onAcknowledged: () -> Unit): View =
    ComposeView(activity).apply {
        setContent { ErrorDialog(dismissRequest = onAcknowledged, error = message) }
    }
