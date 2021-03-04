package com.revenuecat.purchases.amazon.helpers

import android.content.Context
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.amazon.PurchasingServiceProvider

class PurchasingServiceProviderForTest : PurchasingServiceProvider {

    internal val listeners = mutableListOf<PurchasingListener>()

    internal var getProductDataRequestId: String? = null
    internal var getUserDataRequestId: String? = null
    internal var getPurchaseRequestId: String? = null
    internal var getPurchaseUpdatesRequestId: String? = null

    internal var getProductDataCalled: Int = 0
    internal var getUserDataCalled: Boolean = false
    internal var purchaseCalled: Boolean = false
    internal var getPurchaseUpdatesCalled: Boolean = false
    internal var notifyFulfillmentCalled: Boolean = false

    override fun registerListener(
        context: Context,
        listener: PurchasingListener
    ) {
        listeners.add(listener)
    }

    override fun getProductData(
        skus: Set<String>
    ): RequestId {
        getProductDataCalled++
        return RequestId.fromString(getProductDataRequestId ?: "${System.currentTimeMillis()}")
    }

    override fun getUserData(): RequestId {
        getUserDataCalled = true
        return RequestId.fromString(getUserDataRequestId ?: "${System.currentTimeMillis()}")
    }

    override fun purchase(sku: String): RequestId {
        purchaseCalled = true
        return RequestId.fromString(getPurchaseRequestId ?: "${System.currentTimeMillis()}")
    }

    override fun getPurchaseUpdates(reset: Boolean): RequestId {
        getPurchaseUpdatesCalled = true
        return RequestId.fromString(getPurchaseUpdatesRequestId ?: "${System.currentTimeMillis()}")
    }

    override fun notifyFulfillment(receiptId: String, fulfillmentResult: FulfillmentResult) {
        notifyFulfillmentCalled = true
    }
}
