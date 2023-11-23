package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.buildQueryPurchasesParams
import com.revenuecat.purchases.google.toRevenueCatProductType
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

internal data class QueryPurchasesByTypeUseCaseParams(
    val dateProvider: DateProvider = DefaultDateProvider(),
    val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    override val appInBackground: Boolean,
    @ProductType val productType: String,
) : UseCaseParams

internal class QueryPurchasesByTypeUseCase(
    private val useCaseParams: QueryPurchasesByTypeUseCaseParams,
    val onSuccess: (Map<String, StoreTransaction>) -> Unit,
    val onError: (PurchasesError) -> Unit,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<Map<String, StoreTransaction>>(useCaseParams, onError, executeRequestOnUIThread) {

    override val errorMessage: String
        get() = "Error when querying purchases of type ${useCaseParams.productType}"

    override fun executeAsync() {
        withConnectedClient {
            useCaseParams.productType.buildQueryPurchasesParams()?.let { queryPurchasesParams ->
                queryPurchasesAsyncWithTrackingEnsuringOneResponse(
                    this,
                    useCaseParams.productType,
                    queryPurchasesParams,
                ) { result, purchases ->
                    processResult(
                        result,
                        purchases.toMapOfGooglePurchaseWrapper(useCaseParams.productType),
                    )
                }
            } ?: run {
                errorLog(PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchasesByType"))
                val devErrorResponseCode = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                    .build()
                processResult(devErrorResponseCode, emptyMap())
            }
        }
    }

    override fun onOk(received: Map<String, StoreTransaction>) {
        onSuccess(received)
    }

    private fun queryPurchasesAsyncWithTrackingEnsuringOneResponse(
        billingClient: BillingClient,
        @ProductType productType: String,
        queryParams: QueryPurchasesParams,
        listener: PurchasesResponseListener,
    ) {
        val hasResponded = AtomicBoolean(false)
        val requestStartTime = useCaseParams.dateProvider.now
        billingClient.queryPurchasesAsync(queryParams) { billingResult, purchases ->
            if (hasResponded.getAndSet(true)) {
                log(
                    LogIntent.GOOGLE_ERROR,
                    OfferingStrings.EXTRA_QUERY_PURCHASES_RESPONSE.format(billingResult.responseCode),
                )
                return@queryPurchasesAsync
            }
            trackGoogleQueryPurchasesRequestIfNeeded(productType, billingResult, requestStartTime)
            listener.onQueryPurchasesResponse(billingResult, purchases)
        }
    }

    private fun trackGoogleQueryPurchasesRequestIfNeeded(
        @ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        useCaseParams.diagnosticsTrackerIfEnabled?.trackGoogleQueryPurchasesRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, useCaseParams.dateProvider.now),
        )
    }

    private fun List<Purchase>.toMapOfGooglePurchaseWrapper(
        @ProductType productType: String,
    ): Map<String, StoreTransaction> {
        return this.associate { purchase ->
            val hash = purchase.purchaseToken.sha1()
            hash to purchase.toStoreTransaction(productType.toRevenueCatProductType())
        }
    }
}
