package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import com.amazon.device.iap.PurchasingService

class ProxyAmazonBillingActivity : Activity() {

    private lateinit var broadcastReceiver: BroadcastReceiver
    private val filter = IntentFilter("com.revenuecat.purchases").apply {
        addAction("purchase_finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcastReceiver = ProxyAmazonBillingActivityBroadcastReceiver(this)
        applicationContext.registerReceiver(broadcastReceiver, filter)

        val sku = intent.getStringExtra("sku")
        val resultReceiver = intent.getParcelableExtra("result_receiver") as? ResultReceiver

        val requestId = PurchasingService.purchase(sku)

        val bundle = Bundle().apply {
            putParcelable("request_id", requestId)
        }
        resultReceiver?.send(0, bundle)
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(broadcastReceiver)
    }
}
