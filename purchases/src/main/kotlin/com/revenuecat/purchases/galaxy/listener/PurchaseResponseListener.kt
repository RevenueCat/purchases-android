package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

internal interface PurchaseResponseListener : OnPaymentListener {

    override fun onPayment(error: ErrorVo, purchase: PurchaseVo?) {
        /* intentionally ignored. Use PurchaseDataHandler instead */
    }

    @SuppressWarnings("LongParameterList")
    fun purchase(
        appUserID: String,
        storeProduct: StoreProduct,
        onSuccess: (PurchaseVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
