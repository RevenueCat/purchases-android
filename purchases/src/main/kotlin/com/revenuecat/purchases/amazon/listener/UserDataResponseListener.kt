package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.PurchasesError

internal interface UserDataResponseListener : PurchasingListener {
    override fun onProductDataResponse(response: ProductDataResponse) {
        /* intentionally ignored. Use ProductDataResponseListener instead */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* intentionally ignored. Use PurchaseResponseListener instead */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* intentionally ignored. Use PurchaseUpdatesResponseListener instead */
    }

    fun getUserData(onSuccess: (UserData) -> Unit, onError: (PurchasesError) -> Unit)
}
