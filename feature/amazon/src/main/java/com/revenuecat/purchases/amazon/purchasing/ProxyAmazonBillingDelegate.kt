package com.revenuecat.purchases.amazon.purchasing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.amazon.ProxyAmazonBillingActivity
import com.revenuecat.purchases.amazon.ProxyAmazonBillingActivityBroadcastReceiver
import com.revenuecat.purchases.amazon.PurchasingServiceProvider

internal class ProxyAmazonBillingDelegate {

    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var broadcastReceiver: ProxyAmazonBillingActivityBroadcastReceiver? = null
    private val filter = ProxyAmazonBillingActivityBroadcastReceiver.newPurchaseFinishedIntentFilter()

    fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        broadcastReceiver = ProxyAmazonBillingActivityBroadcastReceiver(activity)
        activity.applicationContext.registerReceiver(broadcastReceiver, filter)
        if (savedInstanceState == null) {
            startAmazonPurchase(activity.intent)
        }
    }

    fun onDestroy(activity: Activity) {
        activity.applicationContext.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun startAmazonPurchase(intent: Intent) {
        val sku = intent.getStringExtra(ProxyAmazonBillingActivity.EXTRAS_SKU)
        val resultReceiver =
            intent.getParcelableExtra<ResultReceiver>(ProxyAmazonBillingActivity.EXTRAS_RESULT_RECEIVER)
        val purchasingServiceProvider =
            intent.getParcelableExtra<PurchasingServiceProvider>(ProxyAmazonBillingActivity.EXTRAS_SERVICE_PROVIDER)
        val requestId = purchasingServiceProvider.purchase(sku)
        val bundle = Bundle().apply {
            putParcelable(ProxyAmazonBillingActivity.EXTRAS_REQUEST_ID, requestId)
        }
        resultReceiver?.send(0, bundle)
    }
}
