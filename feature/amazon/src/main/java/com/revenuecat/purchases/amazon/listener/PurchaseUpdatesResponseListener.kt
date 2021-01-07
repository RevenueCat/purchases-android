package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.UserDataResponse

interface PurchaseUpdatesResponseListener : PurchasingListener {
    override fun onUserDataResponse(response: UserDataResponse) {
        /* default implementation */
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        /* default implementation */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* default implementation */
    }
}
