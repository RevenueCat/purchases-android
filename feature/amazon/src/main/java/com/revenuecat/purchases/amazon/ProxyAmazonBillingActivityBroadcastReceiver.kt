package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.VisibleForTesting

internal class ProxyAmazonBillingActivityBroadcastReceiver(private val activity: Activity) : BroadcastReceiver() {

    companion object {
        const val PURCHASE_FINISHED_ACTION = "com.revenuecat.purchases.purchase_finished"

        fun newPurchaseFinishedIntentFilter(): IntentFilter = IntentFilter(PURCHASE_FINISHED_ACTION)

        fun newPurchaseFinishedIntent(applicationContext: Context): Intent {
            return Intent(PURCHASE_FINISHED_ACTION).also { intent ->
                intent.setPackage(applicationContext.packageName)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var onReceiveCalled = false

    override fun onReceive(context: Context, intent: Intent) {
        onReceiveCalled = true
        activity.finish()
    }
}
