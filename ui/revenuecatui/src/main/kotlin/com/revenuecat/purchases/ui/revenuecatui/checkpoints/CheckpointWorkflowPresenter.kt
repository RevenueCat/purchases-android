package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Presents the workflow resolved for a checkpoint in a dialog-owned [Window] over the current activity, so the
 * host activity stays started underneath, and reports the terminal [CheckpointPaywallOutcome] back to the
 * [CheckpointsManager] that asked for it, exactly once. Terminal purchase/restore events are recorded as they
 * happen and delivered when the workflow window goes away for good.
 *
 * The window dies with its host activity, so this presenter outlives any single window: on a configuration
 * change it dismisses the window (before the host tears its own down) and re-presents over the next started
 * activity, while a host that is finishing for real ends the presentation. A [ViewModelStore] owned by the
 * presenter, not the window, keeps the paywall's ViewModel alive across those re-presents.
 *
 * Main-thread only, like the [CheckpointsManager.checkpoint] dispatch that drives it.
 */
internal class CheckpointWorkflowPresenter(
    private val callId: String,
    private val manager: CheckpointsManager,
    private val createContent: (Activity, PaywallOptions) -> View = { activity, options ->
        ComposeView(activity).apply { setContent { Paywall(options) } }
    },
) {

    // The only strong path to the host activity: nulled on every dismissal, so dropping it at host destroy is
    // also the no-leak guarantee.
    private var dialog: ComponentDialog? = null
    private var host: Activity? = null
    private var application: Application? = null
    private var awaitingRepresent = false

    // Saved on a configuration-change dismissal and restored into the re-presented window, so view hierarchy
    // and rememberSaveable state survive rotation.
    private var pendingSavedState: Bundle? = null

    private val viewModelStore = ViewModelStore()
    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = this@CheckpointWorkflowPresenter.viewModelStore
    }

    /**
     * Throws on failure (e.g. the activity's window token dying underneath us); the first show surfaces that
     * through [CheckpointsManager.present]'s error handling, a re-present catches it in [lifecycleCallbacks].
     */
    fun show(activity: Activity) {
        dismissWindowOnly()
        val presentation = manager.presentation(callId)
        if (presentation == null) {
            Logger.w("Checkpoint call '$callId' no longer exists. Closing the checkpoint workflow.")
            teardown()
            return
        }
        if (application == null) {
            application = activity.application.also { it.registerActivityLifecycleCallbacks(lifecycleCallbacks) }
        }
        host = activity
        val resolution = presentation.resolution
        val options = PaywallOptions.Builder(dismissRequest = ::requestDismiss)
            .injectedWorkflow(resolution.workflow, resolution.offering, resolution.uiConfig)
            .setCustomVariables(presentation.customVariables)
            .setListener(outcomeListener)
            .build()
        val dialog = ComponentDialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.window?.let(::configureWindow)
        // Back must never fall through to the dispatcher's cancel fallback; the paywall's own BackHandler
        // decides what back does.
        dialog.setCancelable(false)
        dialog.setContentView(
            createContent(activity, options),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        dialog.window?.decorView?.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        dialog.setOnDismissListener { dismissed -> onWindowDismissed(dismissed) }
        this.dialog = dialog
        val savedState = pendingSavedState
        pendingSavedState = null
        // onRestoreInstanceState drives onCreate with the saved state and shows the dialog itself when the
        // state was captured from a showing window.
        savedState?.let(dialog::onRestoreInstanceState)
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    // The manager already took the call, so this only takes the window down and stops observing: completing
    // here would double-report.
    fun abandon() {
        dismissWindowOnly()
        teardown()
    }

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityDestroyed(activity: Activity) {
            if (activity !== host) return
            host = null
            val isConfigurationChange = activity.isChangingConfigurations
            if (isConfigurationChange) {
                pendingSavedState = dialog?.onSaveInstanceState()
            }
            // Always before the host's own window teardown, or the framework reports the dialog as leaked.
            dismissWindowOnly()
            if (isConfigurationChange) {
                awaitingRepresent = true
            } else {
                finish()
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (!awaitingRepresent || activity.isFinishing) return
            awaitingRepresent = false
            try {
                show(activity)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // The suspended checkpoint() call must never hang on a failed re-present.
                Logger.e("Failed to re-present checkpoint workflow after a configuration change: $e")
                finish()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    private fun requestDismiss() {
        dismissWindowOnly()
        finish()
    }

    // Safety net for dismissals this presenter didn't initiate (e.g. the system tearing the window down):
    // without it the suspended checkpoint() call and the one-at-a-time slot would stay stuck. The identity
    // check matters because an external dismissal posts this callback before the listener can be detached:
    // by the time it runs, a configuration change may already have re-presented a new window, which a stale
    // report must not take down.
    private fun onWindowDismissed(dismissed: DialogInterface) {
        if (dismissed !== dialog) return
        dialog = null
        finish()
    }

    private fun dismissWindowOnly() {
        val dialog = this.dialog ?: return
        this.dialog = null
        // Dialog delivers the dismiss callback through a posted message, so the safety net must be detached
        // before dismissing rather than suppressed around the call.
        dialog.setOnDismissListener(null)
        try {
            dialog.dismiss()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w("Error dismissing checkpoint workflow window: $e")
        }
    }

    private fun finish() {
        teardown()
        manager.onPresentationFinished(callId)
    }

    private fun teardown() {
        dismissWindowOnly()
        application?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        application = null
        host = null
        awaitingRepresent = false
        pendingSavedState = null
        viewModelStore.clear()
    }

    // Hand-rolls what enableEdgeToEdge does for activity windows. The window frame itself already covers the
    // system bar regions: the non-floating dialog theme makes PhoneWindow.generateLayout grant this window
    // the same layout flags and fitInsetsTypes=0 an activity window gets.
    private fun configureWindow(window: Window) {
        // Forces decor installation so generateLayout() cannot overwrite the attributes set below.
        window.decorView
        // Hardware acceleration is not inherited from the host: under a software-rendered host (e.g. Unity)
        // Compose's hardware bitmaps would crash without it.
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        // <30: extends the frame behind the bars via the legacy systemUiVisibility flags; 30-34: stops the
        // decor from padding the content; 35+: no-op (enforced).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Legacy framework themes don't set windowDrawsSystemBarBackgrounds, without which the decor paints
        // the bar regions black instead of transparent - including on 35+, where the bar color itself is
        // already enforced transparent.
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        // The legacy dialog theme never requests light system bars, leaving light-on-light icons in light
        // mode; match enableEdgeToEdge's day/night-based appearance.
        val isDarkMode = window.context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
        window.setSoftInputMode(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            },
        )
    }

    private val outcomeListener = object : PaywallListener {
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            recordOutcome(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            recordOutcome(CheckpointPaywallOutcome.Restored(customerInfo))
        }

        override fun onPurchaseError(error: PurchasesError) {
            if (error.code != PurchasesErrorCode.PurchaseCancelledError) {
                recordOutcome(CheckpointPaywallOutcome.Error(error))
            }
        }

        override fun onRestoreError(error: PurchasesError) {
            recordOutcome(CheckpointPaywallOutcome.Error(error))
        }

        override fun onWebCheckoutOpened() {
            recordOutcome(CheckpointPaywallOutcome.WebCheckoutOpened)
        }
    }

    private fun recordOutcome(outcome: CheckpointPaywallOutcome) {
        manager.recordOutcome(callId, outcome)
    }
}
