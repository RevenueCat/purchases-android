package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener

internal interface IAPHelperProvider {

    @GalaxySerialOperation
    fun getProductsDetails(
        productIDs: String,
        onGetProductsDetailsListener: OnGetProductsDetailsListener,
    )
}
