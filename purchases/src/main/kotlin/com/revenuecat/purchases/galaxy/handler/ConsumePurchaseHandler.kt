package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.ConsumePurchaseResponseListener
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import java.util.ArrayList

internal class ConsumePurchaseHandler(
    private val iapHelperProvider: IAPHelperProvider,
) : ConsumePurchaseResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val transaction: StoreTransaction,
        val onSuccess: (ConsumeVo) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @GalaxySerialOperation
    override fun consumePurchase(
        transaction: StoreTransaction,
        onSuccess: (ConsumeVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_CONSUMPTION_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_CONSUMPTION_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        this.inFlightRequest = Request(
            transaction = transaction,
            onSuccess = onSuccess,
            onError = onError,
        )

        val token = transaction.purchaseToken
        log(LogIntent.PURCHASE) { PurchaseStrings.CONSUMING_PURCHASE.format(token) }

        val requestWasDispatched = iapHelperProvider.consumePurchaseItems(
            purchaseIds = token,
            onConsumePurchasedItemsListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_CONSUMPTION_REQUEST }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_CONSUMPTION_REQUEST,
                ),
            )
            clearInFlightRequest()
            return
        }
    }

    override fun onConsumePurchasedItems(error: ErrorVo, consumptionResults: ArrayList<ConsumeVo>) {
        super.onConsumePurchasedItems(error, consumptionResults)

        if (error.isError()) {
            handleUnsuccessfulConsumptionRequest(error = error)
        } else {
            handleSuccessfulConsumptionRequest(consumptionResults = consumptionResults)
        }
    }

    private fun handleSuccessfulConsumptionRequest(consumptionResults: ArrayList<ConsumeVo>) {
        if (consumptionResults.isEmpty()) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.CONSUMPTION_REQUEST_RETURNED_SUCCESS_BUT_NO_CONSUMPTION_RESULTS
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.CONSUMPTION_REQUEST_RETURNED_SUCCESS_BUT_NO_CONSUMPTION_RESULTS,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        } else if (consumptionResults.count() > 1) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.CONSUMPTION_REQUEST_RETURNED_MORE_THAN_ONE_RESULT
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.CONSUMPTION_REQUEST_RETURNED_MORE_THAN_ONE_RESULT,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        }

        val onSuccess = inFlightRequest?.onSuccess
        clearInFlightRequest()
        onSuccess?.invoke(consumptionResults[0])
    }

    private fun handleUnsuccessfulConsumptionRequest(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.CONSUMPTION_REQUEST_ERRORED.format(
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
