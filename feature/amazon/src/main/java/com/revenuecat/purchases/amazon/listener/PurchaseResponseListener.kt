package com.revenuecat.purchases.amazon.listener

import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreProduct

interface PurchaseResponseListener : PurchasingListener {

    override fun onUserDataResponse(response: UserDataResponse) {
        /* intentionally ignored. Use UserDataResponseListener instead */
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        /* intentionally ignored. Use ProductDataResponseListener instead */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* intentionally ignored. Use PurchaseUpdatesResponseListener instead */
    }

    fun purchase(
        appUserID: String,
        storeProduct: StoreProduct,
        presentedOfferingIdentifier: String?,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit
    )
}
