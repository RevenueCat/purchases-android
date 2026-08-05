package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Presents the workflow resolved for a checkpoint and reports the terminal [CheckpointPaywallOutcome] back to
 * the core module exactly once. Terminal purchase/restore events are cached as they happen and delivered when
 * the paywall dismisses (or the activity is otherwise finished), mirroring
 * [com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivity]'s result handling.
 */
internal class CheckpointWorkflowActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALL_ID = "checkpoint_call_id"
    }

    private var callId: String? = null
    private var entry: CheckpointCallStore.Entry? = null
    private var reported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        callId = intent.getStringExtra(EXTRA_CALL_ID)
        entry = callId?.let { CheckpointCallStore.get(it) }
        val presentation = entry?.presentation ?: run {
            // Process death or stray relaunch: the core-side pending call died with the process, so there is
            // nothing to report to.
            Logger.w("Checkpoint call '$callId' no longer exists. Closing the checkpoint workflow.")
            finish()
            return
        }
        val options = PaywallOptions.Builder(dismissRequest = ::finish)
            .injectedWorkflow(presentation.workflow, presentation.offering, presentation.uiConfig)
            .setListener(outcomeListener)
            .build()
        setContent {
            Paywall(options)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            report()
        }
        super.onDestroy()
    }

    // Outcomes are cached on the store entry (not this instance) so a configuration change doesn't reset them.
    private val outcomeListener = object : PaywallListener {
        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            entry?.outcome = CheckpointPaywallOutcome.Purchased(customerInfo)
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            entry?.outcome = CheckpointPaywallOutcome.Restored(customerInfo)
        }

        override fun onPurchaseError(error: PurchasesError) {
            if (error.code != PurchasesErrorCode.PurchaseCancelledError) {
                entry?.outcome = CheckpointPaywallOutcome.Error(error)
            }
        }

        override fun onRestoreError(error: PurchasesError) {
            entry?.outcome = CheckpointPaywallOutcome.Error(error)
        }
    }

    private fun report() {
        val callId = callId
        val entry = entry
        if (reported || callId == null || entry == null) return
        reported = true
        CheckpointCallStore.remove(callId)
        entry.delegate.onCheckpointPaywallFinished(callId, entry.outcome)
    }
}
