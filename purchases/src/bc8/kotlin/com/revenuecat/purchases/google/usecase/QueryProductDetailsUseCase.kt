package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.UnfetchedProduct.StatusCode
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.QueryProductDetailsParamsBuilderException
import com.revenuecat.purchases.google.buildQueryProductDetailsParams
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.google.toStoreProducts
import com.revenuecat.purchases.strings.OfferingStrings
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

internal data class QueryProductDetailsUseCaseParams(
    val dateProvider: DateProvider = DefaultDateProvider(),
    val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    val productIds: Set<String>,
    val productType: ProductType,
    override val appInBackground: Boolean,
) : UseCaseParams

internal class QueryProductDetailsUseCase(
    private val useCaseParams: QueryProductDetailsUseCaseParams,
    val onReceive: StoreProductsCallback,
    val onError: PurchasesErrorCallback,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<QueryProductDetailsResult>(useCaseParams, onError, executeRequestOnUIThread) {

    override val errorMessage: String
        get() = "Error when fetching products"

    override fun executeAsync() {
        val nonEmptyProductIds = useCaseParams.productIds.filter { it.isNotEmpty() }.toSet()

        if (nonEmptyProductIds.isEmpty()) {
            log(LogIntent.DEBUG) { OfferingStrings.EMPTY_PRODUCT_ID_LIST }
            onReceive(emptyList())
            return
        }
        withConnectedClient {
            val googleType: String = useCaseParams.productType.toGoogleProductType() ?: BillingClient.ProductType.INAPP

            try {
                queryProductDetailsAsyncEnsuringOneResponse(
                    this,
                    googleType,
                    nonEmptyProductIds,
                    ::processResult,
                )
            } catch (e: QueryProductDetailsParamsBuilderException) {
                onError(
                    PurchasesError(
                        code = PurchasesErrorCode.StoreProblemError,
                        underlyingErrorMessage = "${e.message}: ${e.cause?.message}",
                    ),
                )
            }
        }
    }

    override fun onOk(received: QueryProductDetailsResult) {
        log(LogIntent.DEBUG) {
            OfferingStrings.FETCHING_PRODUCTS_FINISHED.format(useCaseParams.productIds.joinToString())
        }
        log(LogIntent.PURCHASE) {
            OfferingStrings.RETRIEVED_PRODUCTS.format(received.productDetailsList.joinToString { it.toString() })
        }
        received.unfetchedProductList.takeIf { it.isNotEmpty() }?.let {
            log(LogIntent.INFO) {
                OfferingStrings.MISSING_PRODUCT_DETAILS.format(
                    received.unfetchedProductList.joinToString { it.toString() },
                )
            }
        }

        received.productDetailsList.takeUnless { it.isEmpty() }?.forEach {
            log(LogIntent.PURCHASE) { OfferingStrings.LIST_PRODUCTS.format(it.productId, it) }
        }
        received.unfetchedProductList.takeUnless { it.isEmpty() }?.forEach {
            log(LogIntent.INFO) {
                OfferingStrings.LIST_UNFETCHED_PRODUCTS.format(
                    it.productId,
                    it.productType,
                    convertUnfetchedProductStatusCodeToString(it.statusCode),
                    it.serializedDocid,
                )
            }
        }

        val storeProducts = received.productDetailsList.toStoreProducts()
        onReceive(storeProducts)
    }

    @Synchronized
    private fun queryProductDetailsAsyncEnsuringOneResponse(
        billingClient: BillingClient,
        @BillingClient.ProductType productType: String,
        productIds: Set<String>,
        listener: ProductDetailsResponseListener,
    ) {
        val params = productType.buildQueryProductDetailsParams(productIds)
        val hasResponded = AtomicBoolean(false)
        val requestStartTime = useCaseParams.dateProvider.now
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (hasResponded.getAndSet(true)) {
                log(LogIntent.GOOGLE_ERROR) {
                    OfferingStrings.EXTRA_QUERY_PRODUCT_DETAILS_RESPONSE.format(billingResult.responseCode)
                }
                return@queryProductDetailsAsync
            }
            trackGoogleQueryProductDetailsRequestIfNeeded(productIds, productType, billingResult, requestStartTime)
            listener.onProductDetailsResponse(billingResult, productDetailsList)
        }
    }

    private fun trackGoogleQueryProductDetailsRequestIfNeeded(
        requestedProductIds: Set<String>,
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        useCaseParams.diagnosticsTrackerIfEnabled?.trackGoogleQueryProductDetailsRequest(
            requestedProductIds,
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, useCaseParams.dateProvider.now),
        )
    }

    private fun convertUnfetchedProductStatusCodeToString(statusCode: Int): String {
        return when (statusCode) {
            StatusCode.UNKNOWN -> "UNKNOWN"
            StatusCode.PRODUCT_NOT_FOUND -> "PRODUCT_NOT_FOUND"
            StatusCode.INVALID_PRODUCT_ID_FORMAT -> "INVALID_PRODUCT_ID_FORMAT"
            StatusCode.NO_ELIGIBLE_OFFER -> "NO_ELIGIBLE_OFFER"
            else -> "UNKNOWN_STATUS_CODE: $statusCode"
        }
    }
}
