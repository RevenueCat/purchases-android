package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import android.os.ResultReceiver
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId

interface PurchasingServiceProvider {

    fun registerListener(context: Context, listener: PurchasingListener)
    fun getProductData(skus: Set<String>): RequestId
    fun getUserData(): RequestId
    fun purchase(activity: Activity, sku: String, resultReceiver: ResultReceiver)
    fun getPurchaseUpdates(reset: Boolean): RequestId
    fun notifyFulfillment(receiptId: String, fulfillmentResult: FulfillmentResult)
    fun onPurchaseCompleted(activity: Activity)
}
