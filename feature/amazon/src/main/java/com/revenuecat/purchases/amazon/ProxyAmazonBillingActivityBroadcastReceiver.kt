package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting

internal class ProxyAmazonBillingActivityBroadcastReceiver(private val activity: Activity) : BroadcastReceiver() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var onReceiveCalled = false

    override fun onReceive(context: Context, intent: Intent) {
        onReceiveCalled = true
        activity.finish()
    }
}
