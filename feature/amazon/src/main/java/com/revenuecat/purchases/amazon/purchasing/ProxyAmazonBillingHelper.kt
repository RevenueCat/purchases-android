package com.revenuecat.purchases.amazon.purchasing

import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import com.revenuecat.purchases.amazon.ProxyAmazonBillingActivity
import com.revenuecat.purchases.amazon.PurchasingServiceProvider

class ProxyAmazonBillingHelper {

    fun startAmazonPurchase(intent: Intent) {
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
