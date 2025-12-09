package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener

internal class DefaultIAPHelperProvider(
    val iapHelper: IapHelper
): IAPHelperProvider {

    override fun getProductsDetails(
        productIDs: String,
        onGetProductsDetailsListener: OnGetProductsDetailsListener
    ) {
        iapHelper.getProductsDetails(
            productIDs,
            onGetProductsDetailsListener,
        )
    }
}