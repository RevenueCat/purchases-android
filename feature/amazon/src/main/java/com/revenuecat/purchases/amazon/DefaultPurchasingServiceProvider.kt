package com.revenuecat.purchases.amazon

import android.content.Context
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId
import kotlinx.parcelize.Parcelize

@Parcelize
class DefaultPurchasingServiceProvider : PurchasingServiceProvider {

    override fun registerListener(
        context: Context,
        listener: PurchasingListener,
    ) {
        PurchasingService.registerListener(context, listener)
    }

    override fun getUserData(): RequestId {
        return PurchasingService.getUserData()
    }

    override fun purchase(sku: String): RequestId {
        return PurchasingService.purchase(sku)
    }

    override fun getProductData(
        skus: Set<String>,
    ): RequestId {
        return PurchasingService.getProductData(skus)
    }

    override fun getPurchaseUpdates(
        reset: Boolean,
    ): RequestId {
        return PurchasingService.getPurchaseUpdates(reset)
    }

    override fun notifyFulfillment(
        receiptId: String,
        fulfillmentResult: FulfillmentResult,
    ) {
        return PurchasingService.notifyFulfillment(receiptId, fulfillmentResult)
    }
}
