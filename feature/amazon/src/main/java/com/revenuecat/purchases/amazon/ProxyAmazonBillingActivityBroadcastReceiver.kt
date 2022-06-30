package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.VisibleForTesting

internal class ProxyAmazonBillingActivityBroadcastReceiver(private val activity: Activity) : BroadcastReceiver() {

    companion object {
        const val INTENT_FILTER_ACTION = "com.revenuecat.purchases.purchase_finished"

        fun newIntentFilter(): IntentFilter = IntentFilter(INTENT_FILTER_ACTION)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var onReceiveCalled = false

    override fun onReceive(context: Context, intent: Intent) {
        onReceiveCalled = true
        activity.finish()
    }
}
