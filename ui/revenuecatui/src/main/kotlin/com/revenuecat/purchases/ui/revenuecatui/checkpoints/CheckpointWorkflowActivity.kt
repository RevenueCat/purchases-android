package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Presents the workflow resolved for a checkpoint and reports the terminal [CheckpointPaywallOutcome] back to
 * the [CheckpointsManager] that asked for it, exactly once. Terminal purchase/restore events are recorded as
 * they happen and delivered when the paywall dismisses (or the activity is otherwise finished), mirroring
 * [com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivity]'s result handling.
 */
internal class CheckpointWorkflowActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALL_ID = "checkpoint_call_id"
    }

    private var callId: String? = null

    // Resolved once, so this activity keeps reporting to the manager that presented it even if the SDK is
    // reconfigured underneath it.
    private var manager: CheckpointsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_CALL_ID)
        callId = id
        // Purchases is unconfigured when the task is restored after process death, which is also exactly
        // when the pending call no longer exists.
        val manager = if (Purchases.isConfigured) Purchases.sharedInstance.checkpointsManager else null
        this.manager = manager
        val presentation = id?.let { manager?.presentation(it) }
        if (id == null || manager == null || presentation == null) {
            Logger.w("Checkpoint call '$id' no longer exists. Closing the checkpoint workflow.")
            finish()
            return
        }
        manager.onPresentationStarted(id, this)
        val resolution = presentation.resolution
        val options = PaywallOptions.Builder(dismissRequest = ::finish)
            .injectedWorkflow(resolution.workflow, resolution.offering, resolution.uiConfig)
            .setCustomVariables(presentation.customVariables)
            .setListener(outcomeListener)
            .build()
        setContent {
            Paywall(options)
        }
    }

    override fun onDestroy() {
        callId?.let { manager?.onActivityDestroyed(it, isChangingConfigurations) }
        super.onDestroy()
    }

    // Outcomes are recorded on the pending call (not this instance) so a configuration change doesn't reset
    // them.
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
        callId?.let { manager?.recordOutcome(it, outcome) }
    }
}
