package com.revenuecat.purchases.galaxy.listener


import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import java.util.ArrayList

internal interface ProductDataResponseListener : OnGetProductsDetailsListener {
    override fun onGetProducts(error: ErrorVo, products: ArrayList<ProductVo>) {
        /* intentionally ignored. Use PurchaseUpdatesResponseListener instead */
    }

    fun getProductDetails(
        productIds: Set<String>,
        onReceive: (List<StoreProduct>) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}