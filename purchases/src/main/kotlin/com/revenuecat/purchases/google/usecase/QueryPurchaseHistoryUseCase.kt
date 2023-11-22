package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.google.buildQueryPurchaseHistoryParams
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

internal data class QueryPurchaseHistoryUseCaseParams(
    val dateProvider: DateProvider = DefaultDateProvider(),
    val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    @BillingClient.ProductType val productType: String,
    override val appInBackground: Boolean,
) : UseCaseParams

internal class QueryPurchaseHistoryUseCase(
    private val useCaseParams: QueryPurchaseHistoryUseCaseParams,
    val onReceive: (List<PurchaseHistoryRecord>) -> Unit,
    val onError: PurchasesErrorCallback,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<List<PurchaseHistoryRecord>?>(useCaseParams, onError, executeRequestOnUIThread) {

    override val errorMessage: String
        get() = "Error receiving purchase history"

    override fun executeAsync() {
        withConnectedClient {
            val hasResponded = AtomicBoolean(false)
            val requestStartTime = useCaseParams.dateProvider.now

            useCaseParams.productType.buildQueryPurchaseHistoryParams()?.let { queryPurchaseHistoryParams ->
                queryPurchaseHistoryAsync(queryPurchaseHistoryParams) { billingResult, purchaseHistory ->
                    if (hasResponded.getAndSet(true)) {
                        log(
                            LogIntent.GOOGLE_ERROR,
                            RestoreStrings.EXTRA_QUERY_PURCHASE_HISTORY_RESPONSE.format(billingResult.responseCode),
                        )
                    } else {
                        trackGoogleQueryPurchaseHistoryRequestIfNeeded(
                            useCaseParams.productType,
                            billingResult,
                            requestStartTime,
                        )
                        processResult(billingResult, purchaseHistory)
                    }
                }
            } ?: run {
                errorLog(PurchaseStrings.INVALID_PRODUCT_TYPE.format("getPurchaseType"))
                val devErrorResponseCode = BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                    .build()
                processResult(devErrorResponseCode, null)
            }
        }
    }

    override fun onOk(received: List<PurchaseHistoryRecord>?) {
        received.takeUnless { it.isNullOrEmpty() }?.forEach {
            log(
                LogIntent.RC_PURCHASE_SUCCESS,
                RestoreStrings.PURCHASE_HISTORY_RETRIEVED
                    .format(it.toHumanReadableDescription()),
            )
        } ?: log(LogIntent.DEBUG, RestoreStrings.PURCHASE_HISTORY_EMPTY)
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
