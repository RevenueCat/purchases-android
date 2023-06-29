package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError

internal interface PurchaseUpdatesResponseListener : PurchasingListener {
    override fun onUserDataResponse(response: UserDataResponse) {
        /* intentionally ignored. Use UserDataResponseListener instead */
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        /* intentionally ignored. Use ProductDataResponseListener instead */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* intentionally ignored. Use PurchaseResponseListener instead */
    }

    fun queryPurchases(onSuccess: (List<Receipt>, UserData) -> Unit, onError: (PurchasesError) -> Unit)
}
