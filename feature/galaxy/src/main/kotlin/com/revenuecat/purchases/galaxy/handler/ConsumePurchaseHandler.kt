package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.ConsumePurchaseResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import java.util.ArrayList

internal class ConsumePurchaseHandler(
    private val iapHelper: IAPHelperProvider,
) : ConsumePurchaseResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val transaction: StoreTransaction,
        val onSuccess: (ConsumeVo) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @OptIn(InternalRevenueCatAPI::class)
    @GalaxySerialOperation
    override fun consumePurchase(
        transaction: StoreTransaction,
        onSuccess: (ConsumeVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_CONSUME_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_CONSUME_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        this.inFlightRequest = Request(
            transaction = transaction,
            onSuccess = onSuccess,
            onError = onError,
        )

        log(LogIntent.PURCHASE) {
            PurchaseStrings.CONSUMING_PURCHASE.format(transaction.purchaseToken)
        }

        // Note: consumePurchasedItems() swallows all exceptions and returns failures to the listener
        val requestWasDispatched = iapHelper.consumePurchasedItems(
            purchaseIds = transaction.purchaseToken,
            onConsumePurchasedItemsListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_CONSUME_REQUEST }
            clearInFlightRequest()
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_CONSUME_REQUEST,
                ),
            )
            return
        }
    }

    override fun onConsumePurchasedItems(
        error: ErrorVo,
        consumptionResults: ArrayList<ConsumeVo?>,
    ) {
        super.onConsumePurchasedItems(error, consumptionResults)
        if (error.isError()) {
            handleUnsuccessfulConsumptionRequest(error = error)
        } else {
            handleSuccessfulConsumptionRequest(consumptionResults = consumptionResults)
        }
    }

    private fun handleSuccessfulConsumptionRequest(consumptionResults: ArrayList<ConsumeVo?>) {
        val nonNullConsumptionResults = consumptionResults.mapNotNull { it }
        if (nonNullConsumptionResults.isEmpty()) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.CONSUME_REQUEST_RETURNED_SUCCESS_BUT_NO_CONSUMPTION_RESULTS
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.CONSUME_REQUEST_RETURNED_SUCCESS_BUT_NO_CONSUMPTION_RESULTS,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        } else if (nonNullConsumptionResults.count() > 1) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.CONSUME_REQUEST_RETURNED_MORE_THAN_ONE_RESULT
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.CONSUME_REQUEST_RETURNED_MORE_THAN_ONE_RESULT,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        }

        val onSuccess = inFlightRequest?.onSuccess
        clearInFlightRequest()
        onSuccess?.invoke(nonNullConsumptionResults[0])
    }

    private fun handleUnsuccessfulConsumptionRequest(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.CONSUME_REQUEST_ERRORED.format(
                inFlightRequest?.transaction?.purchaseToken ?: "[none]",
                underlyingErrorMessage,
            )
        }

        val onError = inFlightRequest?.onError
        clearInFlightRequest()
        onError?.invoke(error.toPurchasesError())
    }

    private fun clearInFlightRequest() {
        inFlightRequest = null
    }
}
