package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import com.revenuecat.purchases.galaxy.toStoreProduct
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import java.util.ArrayList

internal class ProductDataHandler(
    private val iapHelper: IAPHelperProvider,
) : ProductDataResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val productIds: Set<String>,
        val productType: ProductType,
        val onReceive: StoreProductsCallback,
        val onError: PurchasesErrorCallback,
    )

    @get:Synchronized
    internal val productsCache = mutableMapOf<String, StoreProduct>()

    @GalaxySerialOperation
    override fun getProductDetails(
        productIds: Set<String>,
        productType: ProductType,
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
                underlyingErrorMessage = "Only one Galaxy Store product request is allowed at a time.",
            )
            onError(error)
            return
        }

        log(LogIntent.DEBUG) { GalaxyStrings.REQUESTING_PRODUCTS.format(productIds.joinToString()) }

        val request = Request(
            productIds = productIds,
            productType = productType,
            onReceive = onReceive,
            onError = onError,
        )

        if (productsCache.keys.containsAll(productIds)) {
            val cachedProducts = productIds.mapNotNull(productsCache::get)
            this.inFlightRequest = request

            handleStoreProducts(
                storeProducts = cachedProducts,
            )
        } else {
            // When requesting products from the Samsung IAP SDK, the `_productIds` param is a string where
            // the following contents product the following results:
            // - An empty string: queries all products
            // - A string with one product ID in it: queries for that one product
            // - A string with multiple product IDs in it, delimited by a comma
            val productIdRequestString = productIds.joinToString(separator = ",")
            iapHelper.getProductsDetails(
                productIDs = productIdRequestString,
                onGetProductsDetailsListener = this,
            )

            this.inFlightRequest = request
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
        products: List<ProductVo>,
    ) {
        val storeProducts: List<StoreProduct> = products
            .map { it.toStoreProduct() }

        storeProducts.forEach { product ->
            productsCache[product.id] = product
        }

        handleStoreProducts(storeProducts = storeProducts)
    }

    private fun handleStoreProducts(storeProducts: List<StoreProduct>) {
        val storeProductsOfMatchingType = storeProducts.filter { it.type == inFlightRequest?.productType }
        val onReceive = inFlightRequest?.onReceive
        clearInFlightRequest()
        onReceive?.invoke(storeProductsOfMatchingType)
    }

    private fun handleUnsuccessfulProductDataResponse(
        error: ErrorVo,
    ) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.GET_PRODUCT_DETAILS_REQUEST_ERRORED.format(
                inFlightRequest?.productIds?.joinToString() ?: "[none]",
                underlyingErrorMessage,
            )
        }

        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, underlyingErrorMessage)
        val onError = inFlightRequest?.onError
        clearInFlightRequest()
        onError?.invoke(purchasesError)
    }

    private fun clearInFlightRequest() {
        inFlightRequest = null
    }
}
