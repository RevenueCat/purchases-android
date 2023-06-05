package com.revenuecat.purchases.amazon.purchasing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.R

internal class ProxyAmazonBillingActivity : Activity() {

    companion object {
        const val EXTRAS_RESULT_RECEIVER = "result_receiver"
        const val EXTRAS_SKU = "sku"
        const val EXTRAS_PURCHASING_SERVICE_PROVIDER = "purchasing_service_provider"
        const val EXTRAS_REQUEST_ID = "request_id"

        fun newStartIntent(
            context: Context,
            resultReceiver: ResultReceiver,
            sku: String,
            purchasingServiceProvider: PurchasingServiceProvider,
        ): Intent {
            val intent = Intent(context, ProxyAmazonBillingActivity::class.java)
            intent.putExtra(EXTRAS_RESULT_RECEIVER, resultReceiver)
            intent.putExtra(EXTRAS_SKU, sku)
            intent.putExtra(EXTRAS_PURCHASING_SERVICE_PROVIDER, purchasingServiceProvider)
            return intent
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var proxyAmazonBillingDelegate: ProxyAmazonBillingDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Applying theme programmatically because when applying via AndroidManifest, theme is not being
        // applied correctly.
        // What happens is that applying @android:style/Theme.Translucent.NoTitleBar in the manifest works
        // but applying a theme that has that theme as parent, the Activity is not translucent
        // Parent theme also has to be set in the AndroidManifest.xml otherwise it won't be translucent
        setTheme(R.style.ProxyAmazonBillingActivityTheme)
        super.onCreate(savedInstanceState)

        proxyAmazonBillingDelegate = ProxyAmazonBillingDelegate()
        proxyAmazonBillingDelegate?.onCreate(this, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        proxyAmazonBillingDelegate?.onDestroy(this)
        proxyAmazonBillingDelegate = null
    }
}
