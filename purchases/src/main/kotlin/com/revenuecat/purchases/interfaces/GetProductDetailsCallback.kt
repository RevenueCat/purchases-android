package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.ProductDetails

interface GetProductDetailsCallback {
    /**
     * Will be called after ProductDetails have been fetched successfully
     *
     * @param productDetails List of [ProductDetails] retrieved after a successful call to fetch [ProductDetails]
     */
    fun onReceived(productDetailsList: List<ProductDetails>)

    /**
     * Will be called after the purchase has completed with error
     *
     * @param error A [PurchasesError] containing the reason for the failure when fetching the [ProductDetails]
     */
    fun onError(error: PurchasesError)
}
