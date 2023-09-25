package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreProduct

internal interface ProductDataResponseListener : PurchasingListener {
    override fun onUserDataResponse(response: UserDataResponse) {
        /* intentionally ignored. Use UserDataResponseListener instead */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* intentionally ignored. Use PurchaseResponseListener instead */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* intentionally ignored. Use PurchaseUpdatesResponseListener instead */
    }

    fun getProductData(
        skus: Set<String>,
        marketplace: String,
        onReceive: (List<StoreProduct>) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
