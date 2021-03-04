package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.toProductDetails
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.ProductDetails

class ProductDataHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider
) : ProductDataResponseListener {

    data class Request(
        val skuList: List<String>,
        val marketplace: String,
        val onReceive: ProductDetailsListCallback,
        val onError: PurchasesErrorCallback
    )

    @get:Synchronized
    private val productDataRequests = mutableMapOf<RequestId, Request>()

    @get:Synchronized
    internal val productDataCache = mutableMapOf<String, Product>()

    override fun getProductData(
        skus: Set<String>,
        marketplace: String,
        onReceive: (List<ProductDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG, AmazonStrings.REQUESTING_PRODUCTS.format(skus.joinToString()))

        synchronized(this) { productDataCache.toMap() }.let { productDataCache ->
            if (productDataCache.keys.containsAll(skus)) {
                val cachedProducts: Map<String, Product> = productDataCache.filterKeys { skus.contains(it) }
                handleSuccessfulProductDataResponse(cachedProducts, marketplace, onReceive)
            } else {
                val productDataRequestId = purchasingServiceProvider.getProductData(skus)
                productDataRequests[productDataRequestId] = Request(skus.toList(), marketplace, onReceive, onError)
            }
        }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        log(LogIntent.DEBUG, AmazonStrings.PRODUCTS_REQUEST_FINISHED.format(response.requestStatus.name))

        if (response.unavailableSkus.isNotEmpty()) {
            log(LogIntent.DEBUG, AmazonStrings.PRODUCTS_REQUEST_UNAVAILABLE.format(response.unavailableSkus))
        }

        val requestId = response.requestId
        val request = synchronized(this) { productDataRequests.remove(requestId) }

        if (request != null) {
            val responseIsSuccessful = response.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL

            if (responseIsSuccessful) {
                synchronized(this) {
                    productDataCache.putAll(response.productData)
                }
                handleSuccessfulProductDataResponse(response.productData, request.marketplace, request.onReceive)
            } else {
                handleUnsuccessfulProductDataResponse(response, request.onError)
            }
        }
    }

    private fun handleSuccessfulProductDataResponse(
        productData: Map<String, Product>,
        marketplace: String,
        onReceive: ProductDetailsListCallback
    ) {
        log(LogIntent.DEBUG, AmazonStrings.RETRIEVED_PRODUCT_DATA.format(productData))

        if (productData.isEmpty()) {
            log(LogIntent.DEBUG, AmazonStrings.RETRIEVED_PRODUCT_DATA_EMPTY)
        }

        val productDetailsList = productData.values.map { it.toProductDetails(marketplace) }
        onReceive(productDetailsList)
    }

    private fun handleUnsuccessfulProductDataResponse(
        response: ProductDataResponse,
        onError: PurchasesErrorCallback
    ) {
        val underlyingErrorMessage =
            if (response.requestStatus == ProductDataResponse.RequestStatus.NOT_SUPPORTED) {
                "Couldn't fetch product data, since it's not supported."
            } else {
                "Error when fetching product data."
            }

        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, underlyingErrorMessage)

        onError(purchasesError)
    }
}
