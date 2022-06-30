package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting

class ProxyAmazonBillingActivity : Activity() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var broadcastReceiver: ProxyAmazonBillingActivityBroadcastReceiver? = null

    private val filter = IntentFilter("com.revenuecat.purchases").apply {
        addAction("purchase_finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Applying theme programmatically because when applying via AndroidManifest, theme is not being
        // applied correctly.
        // What happens is that applying @android:style/Theme.Translucent.NoTitleBar in the manifest works
        // but applying a theme that has that theme as parent, the Activity is not translucent
        setTheme(R.style.ProxyAmazonBillingActivityTheme)
        super.onCreate(savedInstanceState)
        broadcastReceiver = ProxyAmazonBillingActivityBroadcastReceiver(this)
        applicationContext.registerReceiver(broadcastReceiver, filter)

        if (savedInstanceState == null) {
            val sku = intent.getStringExtra("sku")
            val resultReceiver = intent.getParcelableExtra<ResultReceiver>("result_receiver")
            val purchasingServiceProvider =
                intent.getParcelableExtra<PurchasingServiceProvider>("service_provider")
            val requestId = purchasingServiceProvider.purchase(sku)
            val bundle = Bundle().apply {
                putParcelable("request_id", requestId)
            }
            resultReceiver?.send(0, bundle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }
}
