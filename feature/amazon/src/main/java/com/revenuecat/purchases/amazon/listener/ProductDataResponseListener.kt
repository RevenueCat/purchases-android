package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.ProductDetails

interface ProductDataResponseListener : PurchasingListener {
    override fun onUserDataResponse(response: UserDataResponse) {
        /* default implementation */
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        /* default implementation */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* default implementation */
    }

    fun getProductData(
        skuList: List<String>,
        marketplace: String,
        onReceive: (List<ProductDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    )
}
