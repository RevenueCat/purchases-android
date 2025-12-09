package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener

internal interface IAPHelperProvider {
    fun getProductsDetails(
        productIDs: String,
        onGetProductsDetailsListener: OnGetProductsDetailsListener,
    )
}
