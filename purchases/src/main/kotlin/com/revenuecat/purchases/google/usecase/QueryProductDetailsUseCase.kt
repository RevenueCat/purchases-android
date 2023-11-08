package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.BillingResponse
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.buildQueryProductDetailsParams
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.google.toHumanReadableDescription
import com.revenuecat.purchases.google.toStoreProducts
import com.revenuecat.purchases.strings.OfferingStrings
import java.util.Date
import kotlin.time.Duration

internal class QueryProductDetailsUseCase(
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
) {

    private val maxRetries: Int = 3

    fun queryProductDetailsAsync(
        billingClient: BillingClient,
        productType: ProductType,
        nonEmptyProductIds: Set<String>,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
        retryConnection: (ProductType, Set<String>, StoreProductsCallback, PurchasesErrorCallback) -> Unit,
        retryAttempt: Int = 0,
    ) {
        val googleType: String = productType.toGoogleProductType() ?: BillingClient.ProductType.INAPP
        val params = googleType.buildQueryProductDetailsParams(nonEmptyProductIds)

        queryProductDetailsAsyncEnsuringOneResponse(
            billingClient,
            googleType,
            params,
        ) { billingResult, productDetailsList ->
            when (BillingResponse.fromCode(billingResult.responseCode)) {
                BillingResponse.OK -> {
                    log(
                        LogIntent.DEBUG,
                        OfferingStrings.FETCHING_PRODUCTS_FINISHED
                            .format(productIds.joinToString()),
                    )
                    log(
                        LogIntent.PURCHASE,
                        OfferingStrings.RETRIEVED_PRODUCTS
                            .format(productDetailsList.joinToString { it.toString() }),
                    )
                    productDetailsList.takeUnless { it.isEmpty() }?.forEach {
                        log(LogIntent.PURCHASE, OfferingStrings.LIST_PRODUCTS.format(it.productId, it))
                    }

                    val storeProducts = productDetailsList.toStoreProducts()
                    onReceive(storeProducts)
                }

                BillingResponse.ServiceDisconnected -> {
                    retryConnection(productType, productIds, onReceive, onError)
                }

                BillingResponse.NetworkError,
                BillingResponse.ServiceUnavailable,
                BillingResponse.Error,
                -> {
                    if (retryAttempt < maxRetries) {
                        queryProductDetailsAsync(
                            billingClient,
                            productType,
                            nonEmptyProductIds,
                            productIds,
                            onReceive,
                            onError,
                            retryConnection,
                            retryAttempt + 1,
                        )
                    } else {
                        forwardError(billingResult, onError)
                    }
                }

                else -> {
                    forwardError(billingResult, onError)
                }
            }
        }
    }

    @Synchronized
    private fun queryProductDetailsAsyncEnsuringOneResponse(
        billingClient: BillingClient,
        @BillingClient.ProductType productType: String,
        params: QueryProductDetailsParams,
        listener: ProductDetailsResponseListener,
    ) {
        var hasResponded = false
        val requestStartTime = dateProvider.now
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (hasResponded) {
                log(
                    LogIntent.GOOGLE_ERROR,
                    OfferingStrings.EXTRA_QUERY_PRODUCT_DETAILS_RESPONSE.format(billingResult.responseCode),
                )
                return@queryProductDetailsAsync
            }
            hasResponded = true
            trackGoogleQueryProductDetailsRequestIfNeeded(productType, billingResult, requestStartTime)
            listener.onProductDetailsResponse(billingResult, productDetailsList)
        }
    }

    private fun forwardError(billingResult: BillingResult, onError: PurchasesErrorCallback) {
        log(
            LogIntent.GOOGLE_ERROR,
            OfferingStrings.FETCHING_PRODUCTS_ERROR
                .format(billingResult.toHumanReadableDescription()),
        )
        onError(
            billingResult.responseCode.billingResponseToPurchasesError(
                "Error when fetching products. ${billingResult.toHumanReadableDescription()}",
            ).also { errorLog(it) },
        )
    }

    private fun trackGoogleQueryProductDetailsRequestIfNeeded(
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        diagnosticsTrackerIfEnabled?.trackGoogleQueryProductDetailsRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, dateProvider.now),
        )
    }
}
