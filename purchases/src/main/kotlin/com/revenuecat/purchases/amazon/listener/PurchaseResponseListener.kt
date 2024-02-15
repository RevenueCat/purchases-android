package com.revenuecat.purchases.amazon.listener

import android.app.Activity
import android.os.Handler
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreProduct

internal interface PurchaseResponseListener : PurchasingListener {

    override fun onUserDataResponse(response: UserDataResponse) {
        /* intentionally ignored. Use UserDataResponseListener instead */
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        /* intentionally ignored. Use ProductDataResponseListener instead */
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        /* intentionally ignored. Use PurchaseUpdatesResponseListener instead */
    }

    @SuppressWarnings("LongParameterList")
    fun purchase(
        mainHandler: Handler,
        activity: Activity,
        appUserID: String,
        storeProduct: StoreProduct,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
