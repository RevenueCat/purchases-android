package com.revenuecat.purchases.amazon

import android.content.Context
import android.os.Parcelable
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId

interface PurchasingServiceProvider : Parcelable {

    fun registerListener(context: Context, listener: PurchasingListener)
    fun getProductData(skus: Set<String>): RequestId
    fun getUserData(): RequestId
    fun purchase(sku: String): RequestId
    fun getPurchaseUpdates(reset: Boolean): RequestId
    fun notifyFulfillment(receiptId: String, fulfillmentResult: FulfillmentResult)
}
