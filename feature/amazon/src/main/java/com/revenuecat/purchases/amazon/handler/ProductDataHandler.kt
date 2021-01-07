package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.toProductDetails
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.common.PurchasesErrorCallback
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
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
        debugLog("Requesting products with identifiers: ${skuList.joinToString()}")

        if (productDataCache.keys.containsAll(skuList)) {
            val cachedProducts: Map<String, Product> = productDataCache.filterKeys { skuList.contains(it) }
            handleSuccessfulProductDataResponse(cachedProducts, marketplace, onReceive)
        } else {
            val productDataRequestId = PurchasingService.getProductData(skuList.toSet())
            productDataRequests[productDataRequestId] = Request(skuList, marketplace, onReceive, onError)
        }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        debugLog("Products request finished with result ${response.requestStatus.name}")

        if (response.unavailableSkus.isNotEmpty()) {
            debugLog("Unavailable products: ${response.unavailableSkus}")
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
        debugLog("Retrieved productData: $productData")

        if (productData.isEmpty()) {
            debugLog("Product data is empty")
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

        errorLog(purchasesError)

        onError(purchasesError)
    }
}
