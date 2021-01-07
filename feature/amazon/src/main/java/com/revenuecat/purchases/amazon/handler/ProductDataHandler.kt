package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.toProductDetails
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.common.PurchasesErrorCallback
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.ProductDetails

class ProductDataHandler : ProductDataResponseListener {

    data class Request(
        val skuList: List<String>,
        val marketplace: String,
        val onReceive: ProductDetailsListCallback,
        val onError: PurchasesErrorCallback
    )

    private val productDataRequests = mutableMapOf<RequestId, Request>()

    private val productDataCache = mutableMapOf<String, Product>()

    override fun getProductData(
        skuList: List<String>,
        marketplace: String,
        onReceive: (List<ProductDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG, AmazonStrings.REQUESTING_PRODUCTS.format(skuList.joinToString()))

        if (productDataCache.keys.containsAll(skuList)) {
            val cachedProducts: Map<String, Product> = productDataCache.filterKeys { skuList.contains(it) }
            handleSuccessfulProductDataResponse(cachedProducts, marketplace, onReceive)
        } else {
            val productDataRequestId = PurchasingService.getProductData(skuList.toSet())
            productDataRequests[productDataRequestId] = Request(skuList, marketplace, onReceive, onError)
        }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        log(LogIntent.DEBUG, AmazonStrings.PRODUCTS_REQUEST_FINISHED.format(response.requestStatus.name))

        if (response.unavailableSkus.isNotEmpty()) {
            log(LogIntent.DEBUG, AmazonStrings.PRODUCTS_REQUEST_UNAVAILABLE.format(response.unavailableSkus))
        }

        val requestId = response.requestId
        val request = productDataRequests[requestId]

        if (request != null) {
            val responseIsSuccessful = response.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL

            if (responseIsSuccessful) {
                productDataCache.putAll(response.productData)
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
