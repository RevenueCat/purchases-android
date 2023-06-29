package com.revenuecat.purchases.amazon.handler

import android.os.Handler
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.listener.ProductDataResponseListener
import com.revenuecat.purchases.amazon.toStoreProduct
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.StoreProduct

internal class ProductDataHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider,
    private val mainHandler: Handler,
) : ProductDataResponseListener {

    companion object {
        private const val GET_PRODUCT_DATA_TIMEOUT_MILLIS = 10_000L
    }

    private data class Request(
        val skuList: List<String>,
        val marketplace: String,
        val onReceive: StoreProductsCallback,
        val onError: PurchasesErrorCallback,
    )

    @get:Synchronized
    private val productDataRequests = mutableMapOf<RequestId, Request>()

    @get:Synchronized
    internal val productDataCache = mutableMapOf<String, Product>()

    override fun getProductData(
        skus: Set<String>,
        marketplace: String,
        onReceive: (List<StoreProduct>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG, AmazonStrings.REQUESTING_PRODUCTS.format(skus.joinToString()))

        synchronized(this) { productDataCache.toMap() }.let { productDataCache ->
            if (productDataCache.keys.containsAll(skus)) {
                val cachedProducts: Map<String, Product> = productDataCache.filterKeys { skus.contains(it) }
                handleSuccessfulProductDataResponse(cachedProducts, marketplace, onReceive)
            } else {
                val productDataRequestId = purchasingServiceProvider.getProductData(skus)
                val request = Request(skus.toList(), marketplace, onReceive, onError)
                synchronized(this) {
                    productDataRequests[productDataRequestId] = request
                    addTimeoutToProductDataRequest(productDataRequestId)
                }
            }
        }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        // Amazon is catching all exceptions and swallowing them so we have to catch ourselves and log
        try {
            log(LogIntent.DEBUG, AmazonStrings.PRODUCTS_REQUEST_FINISHED.format(response.requestStatus.name))

            if (response.unavailableSkus.isNotEmpty()) {
                log(LogIntent.DEBUG, AmazonStrings.PRODUCTS_REQUEST_UNAVAILABLE.format(response.unavailableSkus))
            }

            val requestId = response.requestId
            val request = getRequest(requestId) ?: return

            val responseIsSuccessful = response.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL

            if (responseIsSuccessful) {
                synchronized(this) {
                    productDataCache.putAll(response.productData)
                }
                handleSuccessfulProductDataResponse(response.productData, request.marketplace, request.onReceive)
            } else {
                handleUnsuccessfulProductDataResponse(response, request.onError)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog("Exception in onProductDataResponse", e)
            throw e
        }
    }

    private fun handleSuccessfulProductDataResponse(
        productData: Map<String, Product>,
        marketplace: String,
        onReceive: StoreProductsCallback,
    ) {
        log(LogIntent.DEBUG, AmazonStrings.RETRIEVED_PRODUCT_DATA.format(productData))

        if (productData.isEmpty()) {
            log(LogIntent.DEBUG, AmazonStrings.RETRIEVED_PRODUCT_DATA_EMPTY)
        }

        val storeProducts = productData.values.mapNotNull { it.toStoreProduct(marketplace) }
        onReceive(storeProducts)
    }

    private fun handleUnsuccessfulProductDataResponse(
        response: ProductDataResponse,
        onError: PurchasesErrorCallback,
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

    private fun addTimeoutToProductDataRequest(requestId: RequestId) {
        mainHandler.postDelayed(
            {
                val request = getRequest(requestId) ?: return@postDelayed
                val error = PurchasesError(
                    PurchasesErrorCode.UnknownError,
                    AmazonStrings.ERROR_TIMEOUT_GETTING_PRODUCT_DATA.format(request.skuList.toString()),
                )
                request.onError(error)
            },
            GET_PRODUCT_DATA_TIMEOUT_MILLIS,
        )
    }

    @Synchronized
    private fun getRequest(requestId: RequestId): Request? {
        return productDataRequests.remove(requestId)
    }
}
