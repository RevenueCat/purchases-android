package com.revenuecat.purchases.google.usecase

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.toHumanReadableDescription
import com.revenuecat.purchases.strings.PurchaseStrings

internal class AcknowledgePurchaseUseCaseParams(
    public val purchaseToken: String,
    public val initiationSource: PostReceiptInitiationSource,
    override val appInBackground: Boolean,
) : UseCaseParams

internal class AcknowledgePurchaseUseCase(
    private val useCaseParams: AcknowledgePurchaseUseCaseParams,
    public val onReceive: (purchaseToken: String) -> Unit,
    public val onError: PurchasesErrorCallback,
    public val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
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
        get() = "Error acknowledging purchase"

    override fun executeAsync() {
        withConnectedClient {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(useCaseParams.purchaseToken)
                .build()
            acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                processResult(
                    billingResult,
                    useCaseParams.purchaseToken,
                    onError = { errorBillingResult ->
                        val underlyingErrorMessage: String
                        if (errorBillingResult.responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED &&
                            useCaseParams.initiationSource == PostReceiptInitiationSource.RESTORE
                        ) {
                            underlyingErrorMessage = PurchaseStrings.ACKNOWLEDGING_PURCHASE_ERROR_RESTORE
                            log(LogIntent.GOOGLE_WARNING) { underlyingErrorMessage }
                        } else {
                            underlyingErrorMessage =
                                "$errorMessage - ${errorBillingResult.toHumanReadableDescription()}"
                            log(LogIntent.GOOGLE_ERROR) { underlyingErrorMessage }
                        }
                        onError(
                            errorBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage),
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
