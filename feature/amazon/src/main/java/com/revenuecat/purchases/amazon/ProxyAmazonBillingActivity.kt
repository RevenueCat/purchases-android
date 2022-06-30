package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting

internal class ProxyAmazonBillingActivity : Activity() {

    companion object {

        const val EXTRAS_RESULT_RECEIVER = "result_receiver"
        const val EXTRAS_SKU = "sku"
        const val EXTRAS_SERVICE_PROVIDER = "service_provider"
        const val EXTRAS_REQUEST_ID = "request_id"

        fun newStartIntent(
            context: Context,
            resultReceiver: ResultReceiver,
            sku: String,
            purchasingServiceProvider: PurchasingServiceProvider
        ): Intent {
            val intent = Intent(context, ProxyAmazonBillingActivity::class.java)
            intent.putExtra(EXTRAS_RESULT_RECEIVER, resultReceiver)
            intent.putExtra(EXTRAS_SKU, sku)
            intent.putExtra(EXTRAS_SERVICE_PROVIDER, purchasingServiceProvider)
            return intent
        }
    }

    @JvmSynthetic
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
            val sku = intent.getStringExtra(EXTRAS_SKU)
            val resultReceiver = intent.getParcelableExtra<ResultReceiver>(EXTRAS_RESULT_RECEIVER)
            val purchasingServiceProvider =
                intent.getParcelableExtra<PurchasingServiceProvider>(EXTRAS_SERVICE_PROVIDER)
            val requestId = purchasingServiceProvider.purchase(sku)
            val bundle = Bundle().apply {
                putParcelable(EXTRAS_REQUEST_ID, requestId)
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
