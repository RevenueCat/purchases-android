package com.revenuecat.purchases.amazon

import android.content.Context
import android.os.Parcelable
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId

internal interface PurchasingServiceProvider : Parcelable {

    public fun registerListener(context: Context, listener: PurchasingListener)
    public fun getProductData(skus: Set<String>): RequestId
    public fun getUserData(): RequestId
    public fun purchase(sku: String): RequestId
    public fun getPurchaseUpdates(reset: Boolean): RequestId
    public fun notifyFulfillment(receiptId: String, fulfillmentResult: FulfillmentResult)
}
