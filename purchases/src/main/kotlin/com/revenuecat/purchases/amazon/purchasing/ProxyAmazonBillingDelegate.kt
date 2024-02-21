package com.revenuecat.purchases.amazon.purchasing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.common.errorLog

internal class ProxyAmazonBillingDelegate {

    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var broadcastReceiver: ProxyAmazonBillingActivityBroadcastReceiver? = null
    private val filter = ProxyAmazonBillingActivityBroadcastReceiver.newPurchaseFinishedIntentFilter()

    @SuppressLint("WrongConstant")
    fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        broadcastReceiver = ProxyAmazonBillingActivityBroadcastReceiver(activity)
        ContextCompat.registerReceiver(activity, broadcastReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        if (savedInstanceState == null) {
            val requestId = startAmazonPurchase(activity.intent)
            if (requestId == null) {
                activity.finish()
            }
        }
    }

    fun onDestroy(activity: Activity) {
        activity.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun startAmazonPurchase(intent: Intent): RequestId? {
        val sku = intent.getStringExtra(ProxyAmazonBillingActivity.EXTRAS_SKU)
        val resultReceiver =
            intent.getParcelableExtra<ResultReceiver>(ProxyAmazonBillingActivity.EXTRAS_RESULT_RECEIVER)
        val purchasingServiceProvider =
            intent.getParcelableExtra<PurchasingServiceProvider>(
                ProxyAmazonBillingActivity.EXTRAS_PURCHASING_SERVICE_PROVIDER,
            )
        if (sku == null || resultReceiver == null || purchasingServiceProvider == null) {
            val purchaseInvalidError = PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                String.format(AmazonStrings.ERROR_PURCHASE_INVALID_PROXY_ACTIVITY_ARGUMENTS, intent.toUri(0)),
            )
            errorLog(purchaseInvalidError)
            return null
        }
        val requestId = purchasingServiceProvider.purchase(sku)
        val bundle = Bundle().apply {
            putParcelable(ProxyAmazonBillingActivity.EXTRAS_REQUEST_ID, requestId)
        }
        resultReceiver.send(0, bundle)
        return requestId
    }
}
