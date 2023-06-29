package com.revenuecat.purchases.amazon.purchasing

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.VisibleForTesting
import java.lang.ref.WeakReference

internal class ProxyAmazonBillingActivityBroadcastReceiver(activity: Activity) : BroadcastReceiver() {

    companion object {
        const val PURCHASE_FINISHED_ACTION = "com.revenuecat.purchases.purchase_finished"

        fun newPurchaseFinishedIntentFilter(): IntentFilter = IntentFilter(PURCHASE_FINISHED_ACTION)

        fun newPurchaseFinishedIntent(packageName: String): Intent {
            return Intent(PURCHASE_FINISHED_ACTION).also { intent ->
                intent.setPackage(packageName)
            }
        }
    }

    private val activity: WeakReference<Activity>

    init {
        this.activity = WeakReference(activity)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var onReceiveCalled = false

    override fun onReceive(context: Context, intent: Intent) {
        onReceiveCalled = true
        activity.get()?.finish()
    }
}
