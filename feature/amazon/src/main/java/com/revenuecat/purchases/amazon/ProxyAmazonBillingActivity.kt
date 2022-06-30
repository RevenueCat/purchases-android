package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.amazon.purchasing.ProxyAmazonBillingHelper

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

    private val filter = ProxyAmazonBillingActivityBroadcastReceiver.newPurchaseFinishedIntentFilter()

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
            val proxyAmazonBillingHelper = ProxyAmazonBillingHelper()
            proxyAmazonBillingHelper.startAmazonPurchase(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }
}
