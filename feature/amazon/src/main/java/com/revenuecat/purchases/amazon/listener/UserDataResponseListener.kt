package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserData

interface UserDataResponseListener : PurchasingListener {
    override fun onProductDataResponse(response: ProductDataResponse) {
        /* default implementation */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* default implementation */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* default implementation */
    }

    fun getUserData(onCompletion: (UserData) -> Unit)
}
