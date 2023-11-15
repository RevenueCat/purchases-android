package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.buildQueryProductDetailsParams
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.google.toHumanReadableDescription
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
)

internal class QueryProductDetailsUseCase(
    private val useCaseParams: QueryProductDetailsUseCaseParams,
    val onReceive: StoreProductsCallback,
    val onError: PurchasesErrorCallback,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ((PurchasesError?) -> Unit) -> Unit,
) : UseCase<List<ProductDetails>>(onError, executeRequestOnUIThread) {

    override fun executeAsync() {
        val nonEmptyProductIds = useCaseParams.productIds.filter { it.isNotEmpty() }.toSet()

        if (nonEmptyProductIds.isEmpty()) {
            log(LogIntent.DEBUG, OfferingStrings.EMPTY_PRODUCT_ID_LIST)
            onReceive(emptyList())
            return
        }
        withConnectedClient {
            val googleType: String = useCaseParams.productType.toGoogleProductType() ?: BillingClient.ProductType.INAPP
            val params = googleType.buildQueryProductDetailsParams(nonEmptyProductIds)

            queryProductDetailsAsyncEnsuringOneResponse(
                this,
                googleType,
                params,
            ) { billingResult, productDetailsList ->
                processResult(billingResult, productDetailsList)
            }
        }
    }

    override fun onOk(received: List<ProductDetails>) {
        log(
            LogIntent.DEBUG,
            OfferingStrings.FETCHING_PRODUCTS_FINISHED.format(useCaseParams.productIds.joinToString()),
        )
        log(
            LogIntent.PURCHASE,
            OfferingStrings.RETRIEVED_PRODUCTS.format(received.joinToString { it.toString() }),
        )
        received.takeUnless { it.isEmpty() }?.forEach {
            log(LogIntent.PURCHASE, OfferingStrings.LIST_PRODUCTS.format(it.productId, it))
        }

        val storeProducts = received.toStoreProducts()
        onReceive(storeProducts)
    }

    override fun forwardError(billingResult: BillingResult, onError: PurchasesErrorCallback) {
        log(
            LogIntent.GOOGLE_ERROR,
            OfferingStrings.FETCHING_PRODUCTS_ERROR.format(billingResult.toHumanReadableDescription()),
        )
        onError(
            billingResult.responseCode.billingResponseToPurchasesError(
                "Error when fetching products. ${billingResult.toHumanReadableDescription()}",
            ).also { errorLog(it) },
        )
    }

    @Synchronized
    private fun queryProductDetailsAsyncEnsuringOneResponse(
        billingClient: BillingClient,
        @BillingClient.ProductType productType: String,
        params: QueryProductDetailsParams,
        listener: ProductDetailsResponseListener,
    ) {
        val hasResponded = AtomicBoolean(false)
        val requestStartTime = useCaseParams.dateProvider.now
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (hasResponded.getAndSet(true)) {
                log(
                    LogIntent.GOOGLE_ERROR,
                    OfferingStrings.EXTRA_QUERY_PRODUCT_DETAILS_RESPONSE.format(billingResult.responseCode),
                )
                return@queryProductDetailsAsync
            }
            trackGoogleQueryProductDetailsRequestIfNeeded(productType, billingResult, requestStartTime)
            listener.onProductDetailsResponse(billingResult, productDetailsList)
        }
    }

    private fun trackGoogleQueryProductDetailsRequestIfNeeded(
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        useCaseParams.diagnosticsTrackerIfEnabled?.trackGoogleQueryProductDetailsRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, useCaseParams.dateProvider.now),
        )
    }
}
