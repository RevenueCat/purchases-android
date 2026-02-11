
package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.google.buildQueryPurchasesParams
import com.revenuecat.purchases.google.toRevenueCatProductType
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

internal data class QueryPurchaseHistoryUseCaseParams(
    public val dateProvider: DateProvider = DefaultDateProvider(),
    public val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    public @BillingClient.ProductType val productType: String,
    override val appInBackground: Boolean,
) : UseCaseParams

/**
 * Use case to query the purchase history from Google Play Billing.
 * This doesn't actually return the purchase history starting in Billing Library 8.0.0, however, we keep the name,
 * since we support older versions of the billing client in the bc7 flavor, and we need to ensure that the same name
 * is used.
 */
internal class QueryPurchaseHistoryUseCase(
    private val useCaseParams: QueryPurchaseHistoryUseCaseParams,
    public val onReceive: (List<StoreTransaction>) -> Unit,
    public val onError: PurchasesErrorCallback,
    public val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<List<StoreTransaction>?>(useCaseParams, onError, executeRequestOnUIThread) {

    override val errorMessage: String
        get() = "Error receiving purchase history"

    override fun executeAsync() {
        withConnectedClient {
            val hasResponded = AtomicBoolean(false)
            val requestStartTime = useCaseParams.dateProvider.now

            useCaseParams.productType.buildQueryPurchasesParams()?.let { queryPurchasesParams ->
                queryPurchasesAsync(queryPurchasesParams) { billingResult, activePurchases ->
                    if (hasResponded.getAndSet(true)) {
                        log(LogIntent.GOOGLE_ERROR) {
                            RestoreStrings.EXTRA_QUERY_PURCHASE_HISTORY_RESPONSE.format(billingResult.responseCode)
                        }
                    } else {
                        trackGoogleQueryPurchaseHistoryRequestIfNeeded(
                            useCaseParams.productType,
                            billingResult,
                            requestStartTime,
                        )
                        activePurchases.takeUnless { it.isEmpty() }?.forEach {
                            log(LogIntent.RC_PURCHASE_SUCCESS) {
                                RestoreStrings.PURCHASE_HISTORY_RETRIEVED
                                    .format(it.toHumanReadableDescription())
                            }
                        } ?: log(LogIntent.DEBUG) { RestoreStrings.PURCHASE_HISTORY_EMPTY }
                        processResult(
                            billingResult,
                            activePurchases.map {
                                it.toStoreTransaction(useCaseParams.productType.toRevenueCatProductType())
                            },
                        )
                    }
                }
            } ?: run {
                errorLog { PurchaseStrings.INVALID_PRODUCT_TYPE.format("queryPurchaseHistory") }
                val devErrorResponseCode = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                    .build()
                processResult(devErrorResponseCode, null)
            }
        }
    }

    override fun onOk(received: List<StoreTransaction>?) {
        onReceive(received ?: emptyList())
    }

    private fun trackGoogleQueryPurchaseHistoryRequestIfNeeded(
        @BillingClient.ProductType productType: String,
        billingResult: BillingResult,
        requestStartTime: Date,
    ) {
        useCaseParams.diagnosticsTrackerIfEnabled?.trackGoogleQueryPurchaseHistoryRequest(
            productType,
            billingResult.responseCode,
            billingResult.debugMessage,
            responseTime = Duration.between(requestStartTime, useCaseParams.dateProvider.now),
        )
    }
}
