package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.models.StoreTransaction

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
        onQueryInAppsSuccess: (Map<String, StoreTransaction>) -> Unit,
        onQueryInAppsError: (PurchasesError) -> Unit,
    ) {
        QueryPurchasesByTypeUseCase(
            useCaseParams = QueryPurchasesByTypeUseCaseParams(
                dateProvider = useCaseParams.dateProvider,
                diagnosticsTrackerIfEnabled = useCaseParams.diagnosticsTrackerIfEnabled,
                appInBackground = useCaseParams.appInBackground,
                productType = ProductType.INAPP,
            ),
            onSuccess = onQueryInAppsSuccess,
            onError = onQueryInAppsError,
            withConnectedClient = withConnectedClient,
            executeRequestOnUIThread = executeRequestOnUIThread,
        ).run()
    }

    private fun querySubscriptions(
        onQuerySubscriptionsSuccess: (Map<String, StoreTransaction>) -> Unit,
        onQuerySubscriptionsError: (PurchasesError) -> Unit,
    ) {
        QueryPurchasesByTypeUseCase(
            useCaseParams = QueryPurchasesByTypeUseCaseParams(
                dateProvider = useCaseParams.dateProvider,
                diagnosticsTrackerIfEnabled = useCaseParams.diagnosticsTrackerIfEnabled,
                appInBackground = useCaseParams.appInBackground,
                productType = ProductType.SUBS,
            ),
            onSuccess = onQuerySubscriptionsSuccess,
            onError = onQuerySubscriptionsError,
            withConnectedClient = withConnectedClient,
            executeRequestOnUIThread = executeRequestOnUIThread,
        ).run()
    }

    override fun executeAsync() {
        withConnectedClient {
            querySubscriptions(
                onQuerySubscriptionsSuccess = { activeSubs ->
                    queryInApps(
                        onQueryInAppsSuccess = { unconsumedInApps ->
                            onOk(activeSubs + unconsumedInApps)
                        },
                        onError,
                    )
                },
                onError,
            )
        }
    }

    override fun onOk(received: Map<String, StoreTransaction>) {
        onSuccess(received)
    }
}
