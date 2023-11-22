package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PurchasesErrorCallback

internal class ConsumePurchaseUseCaseParams(
    val purchaseToken: String,
    val initiationSource: PostReceiptInitiationSource,
    appInBackground: Boolean,
) : UseCaseParams(appInBackground)

internal class ConsumePurchaseUseCase(
    private val useCaseParams: ConsumePurchaseUseCaseParams,
    val onReceive: (purchaseToken: String) -> Unit,
    val onError: PurchasesErrorCallback,
    val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<String>(useCaseParams, onError, executeRequestOnUIThread) {

    override val backoffForNetworkErrors: Boolean
        get() = when (useCaseParams.initiationSource) {
            PostReceiptInitiationSource.RESTORE,
            PostReceiptInitiationSource.PURCHASE,
            -> false
            PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES -> true
        }

    override val errorMessage: String
        get() = "Error consuming purchase"

    override fun executeAsync() {
        withConnectedClient {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(useCaseParams.purchaseToken)
                .build()
            consumeAsync(consumeParams, ::processResult)
        }
    }

    override fun onOk(received: String) {
        onReceive(received)
    }
}
