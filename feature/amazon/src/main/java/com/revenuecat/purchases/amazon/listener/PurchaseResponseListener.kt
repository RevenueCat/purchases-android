package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.ProductDetails

interface PurchaseResponseListener : PurchasingListener {

    fun purchase(
        appUserID: String,
        productDetails: ProductDetails,
        presentedOfferingIdentifier: String?,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit
    )

    override fun onUserDataResponse(response: UserDataResponse) {
        /* default implementation */
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        /* default implementation */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* default implementation */
    }
}
