package com.revenuecat.purchases.amazon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.ResultReceiver
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId

class DefaultPurchasingServiceProvider : PurchasingServiceProvider {

    override fun registerListener(
        context: Context,
        listener: PurchasingListener
    ) {
        PurchasingService.registerListener(context, listener)
    }

    override fun getUserData(): RequestId {
        return PurchasingService.getUserData()
    }

    override fun purchase(
        activity: Activity,
        sku: String,
        resultReceiver: ResultReceiver
    ) {
        val intent = Intent(activity, ProxyAmazonBillingActivity::class.java)
        intent.putExtra("result_receiver", resultReceiver)
        intent.putExtra("sku", sku)
        activity.startActivity(intent)
    }

    override fun getProductData(
        skus: Set<String>
    ): RequestId {
        return PurchasingService.getProductData(skus)
    }

    override fun getPurchaseUpdates(
        reset: Boolean
    ): RequestId {
        return PurchasingService.getPurchaseUpdates(reset)
    }

    override fun notifyFulfillment(
        receiptId: String,
        fulfillmentResult: FulfillmentResult
    ) {
        return PurchasingService.notifyFulfillment(receiptId, fulfillmentResult)
    }
}
