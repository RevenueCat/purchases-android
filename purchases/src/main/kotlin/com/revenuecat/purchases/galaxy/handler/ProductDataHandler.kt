package com.revenuecat.purchases.galaxy.handler

import android.os.Handler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
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
        // TODO: Use a GalaxyStrings string
        log(LogIntent.DEBUG) { AmazonStrings.REQUESTING_PRODUCTS.format(productIds.joinToString()) }

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
                    addTimeoutToProductDataRequest(productIds)
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
            .mapNotNull { it }

        inFlightRequest?.onReceive
    }

    private fun handleUnsuccessfulProductDataResponse(
        error: ErrorVo,
    ) {
        val underlyingErrorMessage = error.errorString
        // TODO: Log error string
        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, underlyingErrorMessage)
        inFlightRequest?.onError(purchasesError)
        clearInFlightRequest()
    }

    private fun clearInFlightRequest() {
        inFlightRequest = null
    }

    private fun addTimeoutToProductDataRequest() {
        mainHandler.postDelayed(
            {
                val request = synchronized(this) {
                    inFlightRequest?.also { inFlightRequest = null }
                } ?: return@postDelayed

                val error = PurchasesError(
                    PurchasesErrorCode.UnknownError,
                    AmazonStrings.ERROR_TIMEOUT_GETTING_PRODUCT_DATA.format(request),
                )
                request.onError(error)
            },
            GET_PRODUCT_DATA_TIMEOUT_MILLIS,
        )
    }
}