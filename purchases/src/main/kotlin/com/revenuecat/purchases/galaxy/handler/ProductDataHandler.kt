package com.revenuecat.purchases.galaxy.handler

import android.os.Handler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import com.revenuecat.purchases.galaxy.toStoreProduct
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import java.util.ArrayList

internal class ProductDataHandler(
    private val iapHelper: IapHelper,
    private val mainHandler: Handler,
) : ProductDataResponseListener {

    companion object {
        private const val GET_PRODUCT_DATA_TIMEOUT_MILLIS = 10_000L
    }

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val productIds: Set<String>,
        val onReceive: StoreProductsCallback,
        val onError: PurchasesErrorCallback,
    )

    @get:Synchronized
    internal val productDataCache = mutableMapOf<String, ProductVo>()

    override fun getProductDetails(
        productIds: Set<String>,
        onReceive: (List<StoreProduct>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (productIds.isEmpty()) {
            // Note: This early exit is more than just an optimization. If we call iapHelper.getProductsDetails
            // with an empty string param (this would happen if productIds is empty), then iapHelper.getProductsDetails
            // returns all products for the app, which wouldn't give us the product we want (none).
            log(LogIntent.DEBUG) { GalaxyStrings.EMPTY_GET_PRODUCT_DETAILS_REQUEST }
            onReceive(emptyList())
            return
        }

        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_GET_PRODUCT_DETAILS_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = "Only one Galaxy Store product request is allowed at a time."
            )
            onError(error)
            return
        }

        // TODO: Use a GalaxyStrings string
        log(LogIntent.DEBUG) { GalaxyStrings.REQUESTING_PRODUCTS.format(productIds.joinToString()) }

        synchronized(lock = this) { productDataCache.toMap() }.let { productDataCache ->
            if (productDataCache.keys.containsAll(productIds)) {
                val cachedProducts = productIds.mapNotNull(productDataCache::get)
                handleSuccessfulProductsResponse(
                    products = cachedProducts
                )
            } else {
                // When requesting products from the Samsung IAP SDK, the `_productIds` param is a string where
                // the following contents product the following results:
                // - An empty string: queries all products
                // - A string with one product ID in it: queries for that one product
                // - A string with multiple product IDs in it, delimited by a comma
                val productIdRequestString = productIds.joinToString(separator = ",")
                iapHelper.getProductsDetails(
                    productIdRequestString,
                    this
                )

                val request = Request(productIds = productIds, onReceive = onReceive, onError = onError)
                synchronized(this) {
                    this.inFlightRequest = request
                    addTimeoutToProductDataRequest()
                }
            }
        }
    }

    override fun onGetProducts(error: ErrorVo, products: ArrayList<ProductVo>) {
        super.onGetProducts(error, products)

        if (error.isError()) {
            handleUnsuccessfulProductDataResponse(error = error)
        } else {
            handleSuccessfulProductsResponse(products = products)
        }
    }


    private fun handleSuccessfulProductsResponse(
        products: List<ProductVo>
    ) {
        synchronized(this) {
            products.forEach { product ->
                productDataCache[product.itemId] = product
            }
        }


        val storeProducts: List<StoreProduct> = products
            .map { it.toStoreProduct() }

        inFlightRequest?.onReceive?.invoke(storeProducts)
        clearInFlightRequest()
    }

    private fun handleUnsuccessfulProductDataResponse(
        error: ErrorVo,
    ) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.GET_PRODUCT_DETAILS_REQUEST_ERRORED.format(
                inFlightRequest?.productIds?.joinToString() ?: "[none]",
                underlyingErrorMessage
            )
        }

        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, underlyingErrorMessage)
        inFlightRequest?.onError?.invoke(purchasesError)
        clearInFlightRequest()
    }

    private fun clearInFlightRequest() {
        inFlightRequest = null
    }

    private fun addTimeoutToProductDataRequest() {
        mainHandler.postDelayed(
            {
                val request = synchronized(this) {
                    inFlightRequest?.also { clearInFlightRequest() }
                } ?: return@postDelayed

                val errorString = GalaxyStrings.ERROR_TIMEOUT_GETTING_PRODUCT_DETAILS.format(request)
                log(LogIntent.GALAXY_ERROR) { errorString }
                val error = PurchasesError(
                    code = PurchasesErrorCode.UnknownError,
                    underlyingErrorMessage = errorString,
                )
                request.onError(error)
            },
            GET_PRODUCT_DATA_TIMEOUT_MILLIS,
        )
    }
}