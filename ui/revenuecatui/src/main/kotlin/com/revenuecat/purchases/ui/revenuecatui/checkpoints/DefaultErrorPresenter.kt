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
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter.Completion.Result
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.helpers.EDGE_TO_EDGE_WINDOW_THEME
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.applyEdgeToEdge

/**
 * The SDK's own [ErrorPresenter], used when neither the call nor the [Purchases] instance supplies one: shows the
 * error in the SDK's dialog over the current activity and, once the user acknowledges it, resumes the flow when it
 * can go on and ends it otherwise. It goes through the same contract an app presenter does and uses nothing an app
 * presenter lacks: the current activity, a dialog of its own, and the completion. One instance per checkpoint call.
 *
 * The dialog dies with its activity, so on a configuration change it is shown again over the next started
 * activity. An activity finishing for real takes the flow with it, so the presentation ends with [Result.Retry],
 * which also ends a flow that cannot go on.
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
        // A new error while the previous one is still up replaces it: the flow went on, so that one is a Retry.
        presentation?.let { finish(it, Result.Retry) }
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
        // The error dialog inside handles back and outside taps itself, both as an acknowledgement.
        dialog.setCancelable(false)
        dialog.setContentView(
            createContent(activity, current.message) { acknowledged(current) },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        current.dialog = dialog
        dialog.show()
    }

    private fun acknowledged(current: Presentation) {
        finish(current, if (current.params.flowCanContinue) Result.Retry else Result.Continued)
    }

    // Only the current presentation finishes, and only once: a stale dialog's report is ignored.
    private fun finish(current: Presentation, result: Result) {
        if (presentation !== current) return
        presentation = null
        dismissDialog(current)
        teardown()
        current.completion.complete(result)
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
                finish(current, Result.Retry)
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
                finish(current, Result.Retry)
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    // What the paywall's own dialog shows: a presentation error carries its detail in the underlying message (the
    // paywall's reason), while a purchase or restore error reads best as its code's description.
    private val Presentation.message: String
        get() = if (params.source == ErrorPresenter.Source.PRESENTATION) {
            params.error.underlyingErrorMessage ?: params.error.message
        } else {
            params.error.message
        }
}

// The paywall's own error dialog, unchanged: the Compose AlertDialog brings the scrim and the Material look, so the
// window hosting it only needs to be transparent.
private fun errorDialogContent(activity: Activity, message: String, onAcknowledged: () -> Unit): View =
    ComposeView(activity).apply {
        setContent { ErrorDialog(dismissRequest = onAcknowledged, error = message) }
    }
