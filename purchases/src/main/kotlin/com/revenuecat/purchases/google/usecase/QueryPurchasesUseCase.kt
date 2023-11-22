package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.buildQueryPurchasesParams
import com.revenuecat.purchases.google.toHumanReadableDescription
import com.revenuecat.purchases.google.toRevenueCatProductType
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

internal data class QueryPurchasesUseCaseParams(
    val dateProvider: DateProvider = DefaultDateProvider(),
    val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    override val appInBackground: Boolean,
) : UseCaseParams

internal class QueryPurchasesUseCase(
    private val useCaseParams: QueryPurchasesUseCaseParams,
    val onSuccess: (Map<String, StoreTransaction>) -> Unit,
    val onError: (PurchasesError) -> Unit,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<Map<String, StoreTransaction>>(useCaseParams, onError, executeRequestOnUIThread) {

    override val errorMessage: String
        get() = "Error when querying purchases"

    private fun queryInApps(
        billingClient: BillingClient,
        onQueryInAppsSuccess: (Map<String, StoreTransaction>) -> Unit,
        onQueryInAppsError: (BillingResult) -> Unit,
    ) {
        val queryInAppsPurchasesParams = BillingClient.ProductType.INAPP.buildQueryPurchasesParams()
        if (queryInAppsPurchasesParams == null) {
            val purchasesError = PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchases"),
            )
            onError(purchasesError)
            return
        }

        queryPurchasesAsyncWithTrackingEnsuringOneResponse(
            billingClient = billingClient,
            productType = BillingClient.ProductType.INAPP,
            queryParams = queryInAppsPurchasesParams,
            listener = { unconsumedInAppsResult, unconsumedInAppsPurchases ->
                processResult(
                    unconsumedInAppsResult,
                    unconsumedInAppsPurchases.toMapOfGooglePurchaseWrapper(BillingClient.ProductType.INAPP),
                    onQueryInAppsSuccess,
                    onQueryInAppsError,
                )
            },
        )
    }

    private fun querySubscriptions(
        billingClient: BillingClient,
        onQuerySubscriptionsSuccess: (Map<String, StoreTransaction>) -> Unit,
        onQuerySubscriptionsError: (BillingResult) -> Unit,
    ) {
        val querySubsPurchasesParams = BillingClient.ProductType.SUBS.buildQueryPurchasesParams()
        if (querySubsPurchasesParams == null) {
            val purchasesError = PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchases"),
            )
            onError(purchasesError)
            return
        }
        queryPurchasesAsyncWithTrackingEnsuringOneResponse(
            billingClient,
            BillingClient.ProductType.SUBS,
            querySubsPurchasesParams,
        ) { activeSubsResult, activeSubsPurchases ->
            processResult(
                activeSubsResult,
                activeSubsPurchases.toMapOfGooglePurchaseWrapper(BillingClient.ProductType.SUBS),
                onQuerySubscriptionsSuccess,
                onQuerySubscriptionsError,
            )
        }
    }

    override fun executeAsync() {
        withConnectedClient {
            querySubscriptions(
                this,
                onQuerySubscriptionsSuccess = { activeSubs ->
                    queryInApps(
                        this,
                        onQueryInAppsSuccess = { unconsumedInApps ->
                            onOk(activeSubs + unconsumedInApps)
                        },
                        onQueryInAppsError = { received ->
                            forwardError(
                                received,
                                RestoreStrings.QUERYING_INAPP_ERROR.format(
                                    received.toHumanReadableDescription(),
                                ),
                            )
                        },
                    )
                },
                onQuerySubscriptionsError = { received ->
                    forwardError(
                        received,
                        RestoreStrings.QUERYING_SUBS_ERROR.format(received.toHumanReadableDescription()),
                    )
                },
            )
        }
    }

    override fun onOk(received: Map<String, StoreTransaction>) {
        onSuccess(received)
    }

    private fun forwardError(billingResult: BillingResult, underlyingErrorMessage: String) {
        val purchasesError = billingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)
        onError(purchasesError)
    }

    private fun queryPurchasesAsyncWithTrackingEnsuringOneResponse(
        billingClient: BillingClient,
        @BillingClient.ProductType productType: String,
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
        @BillingClient.ProductType productType: String,
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
        @BillingClient.ProductType productType: String,
    ): Map<String, StoreTransaction> {
        return this.associate { purchase ->
            val hash = purchase.purchaseToken.sha1()
            hash to purchase.toStoreTransaction(productType.toRevenueCatProductType())
        }
    }
}
