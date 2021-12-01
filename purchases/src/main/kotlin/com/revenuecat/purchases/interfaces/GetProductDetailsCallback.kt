package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreProduct

internal interface GetProductDetailsCallback {
    /**
     * Will be called after ProductDetails have been fetched successfully
     *
     * @param productDetails List of [StoreProduct] retrieved after a successful call to fetch [StoreProduct]
     */
    fun onReceived(storeProducts: List<StoreProduct>)

    /**
     * Will be called after the purchase has completed with error
     *
     * @param error A [PurchasesError] containing the reason for the failure when fetching the [StoreProduct]
     */
    fun onError(error: PurchasesError)
}
