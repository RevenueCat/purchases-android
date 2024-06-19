package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.toHumanReadableDescription
import com.revenuecat.purchases.strings.PurchaseStrings

internal class ConsumePurchaseUseCaseParams(
    val purchaseToken: String,
    val initiationSource: PostReceiptInitiationSource,
    override val appInBackground: Boolean,
) : UseCaseParams

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
            consumeAsync(consumeParams) { billingResult, purchaseToken ->
                processResult(
                    billingResult,
                    purchaseToken,
                    onError = {
                        val underlyingErrorMessage: String
                        if (it.responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED &&
                            useCaseParams.initiationSource == PostReceiptInitiationSource.RESTORE
                        ) {
                            underlyingErrorMessage = PurchaseStrings.CONSUMING_PURCHASE_ERROR_RESTORE
                            log(LogIntent.GOOGLE_WARNING, underlyingErrorMessage)
                        } else {
                            underlyingErrorMessage = "$errorMessage - ${billingResult.toHumanReadableDescription()}"
                            log(LogIntent.GOOGLE_ERROR, underlyingErrorMessage)
                        }
                        onError(
                            billingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage),
                        )
                    },
                )
            }
        }
    }

    override fun onOk(received: String) {
        onReceive(received)
    }
}
