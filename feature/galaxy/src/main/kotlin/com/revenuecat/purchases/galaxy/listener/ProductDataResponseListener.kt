package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import java.util.ArrayList

internal interface ProductDataResponseListener : OnGetProductsDetailsListener {
    override fun onGetProducts(error: ErrorVo, products: ArrayList<ProductVo?>) {
        /* intentionally ignored. Use ProductDataHandler instead */
    }

    @GalaxySerialOperation
    fun getProductDetails(
        productIds: Set<String>,
        productType: ProductType,
        onReceive: (List<StoreProduct>) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
