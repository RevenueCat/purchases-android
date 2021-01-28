package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.UserDataResponse

interface PurchaseUpdatesResponseListener : PurchasingListener {
    override fun onUserDataResponse(response: UserDataResponse) {
        /* intentionally ignored. Use UserDataResponseListener instead */
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        /* intentionally ignored. Use ProductDataResponseListener instead */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* intentionally ignored. Use PurchaseResponseListener instead */
    }
}
