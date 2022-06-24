package com.revenuecat.purchases.amazon

import android.app.Activity
import android.os.Bundle
import android.os.ResultReceiver
import com.amazon.device.iap.PurchasingService

class ProxyAmazonBillingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sku = intent.getStringExtra("sku")
        val resultReceiver = intent.getParcelableExtra("result_receiver") as? ResultReceiver

        val requestId = PurchasingService.purchase(sku)

        val bundle = Bundle().apply {
            putParcelable("request_id", requestId)
        }
        resultReceiver?.send(0, bundle)
    }
}
