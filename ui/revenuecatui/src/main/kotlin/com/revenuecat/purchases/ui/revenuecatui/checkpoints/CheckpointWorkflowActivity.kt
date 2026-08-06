package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Presents the workflow resolved for a checkpoint and reports the terminal [CheckpointPaywallOutcome] back to
 * [CheckpointsCoordinator] exactly once. Terminal purchase/restore events are recorded as they happen and
 * delivered when the paywall dismisses (or the activity is otherwise finished), mirroring
 * [com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivity]'s result handling.
 */
internal class CheckpointWorkflowActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALL_ID = "checkpoint_call_id"
    }

    private var callId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        callId = intent.getStringExtra(EXTRA_CALL_ID)
        val resolution = callId?.let { CheckpointsCoordinator.resolution(it) } ?: run {
            // Process death or stray relaunch: the pending call died with the process, so there is nothing
            // to report to.
            Logger.w("Checkpoint call '$callId' no longer exists. Closing the checkpoint workflow.")
            finish()
            return
        }
        val options = PaywallOptions.Builder(dismissRequest = ::finish)
            .injectedWorkflow(resolution.workflow, resolution.offering, resolution.uiConfig)
            .setListener(outcomeListener)
            .build()
        setContent {
            Paywall(options)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            callId?.let { CheckpointsCoordinator.onPaywallFinished(it) }
        }
        super.onDestroy()
    }

    // Outcomes are recorded on the pending call (not this instance) so a configuration change doesn't reset
    // them.
    private val outcomeListener = object : PaywallListener {
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            recordOutcome(CheckpointPaywallOutcome.Purchased(customerInfo))
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
    }

    private fun recordOutcome(outcome: CheckpointPaywallOutcome) {
        callId?.let { CheckpointsCoordinator.recordOutcome(it, outcome) }
    }
}
