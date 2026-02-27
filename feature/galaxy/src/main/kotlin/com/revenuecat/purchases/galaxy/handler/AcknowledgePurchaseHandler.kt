package com.revenuecat.purchases.galaxy.handler

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.AcknowledgePurchaseResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.vo.AcknowledgeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import java.util.ArrayList

internal class AcknowledgePurchaseHandler(
    private val iapHelper: IAPHelperProvider,
    private val context: Context,
) : AcknowledgePurchaseResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val transaction: StoreTransaction,
        val onSuccess: (AcknowledgeVo) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @OptIn(InternalRevenueCatAPI::class)
    @Suppress("ReturnCount")
    @GalaxySerialOperation
    override fun acknowledgePurchase(
        transaction: StoreTransaction,
        onSuccess: (AcknowledgeVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_ACKNOWLEDGE_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_ACKNOWLEDGE_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        if (!iapHelper.isAcknowledgeAvailable(context = context)) {
            log(LogIntent.GALAXY_WARNING) {
                GalaxyStrings.WARNING_ACKNOWLEDGING_PURCHASES_UNAVAILABLE
            }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.UnsupportedError,
                    underlyingErrorMessage = GalaxyStrings.WARNING_ACKNOWLEDGING_PURCHASES_UNAVAILABLE,
                ),
            )
            return
        }

        this.inFlightRequest = Request(
            transaction = transaction,
            onSuccess = onSuccess,
            onError = onError,
        )

        log(LogIntent.PURCHASE) {
            PurchaseStrings.ACKNOWLEDGING_PURCHASE.format(transaction.purchaseToken)
        }

        val requestWasDispatched = iapHelper.acknowledgePurchases(
            purchaseIds = transaction.purchaseToken,
            onAcknowledgePurchasesListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_ACKNOWLEDGE_REQUEST }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_ACKNOWLEDGE_REQUEST,
                ),
            )
            clearInFlightRequest()
            return
        }
    }

    override fun onAcknowledgePurchases(
        error: ErrorVo,
        acknowledgementResults: ArrayList<AcknowledgeVo?>,
    ) {
        super.onAcknowledgePurchases(error, acknowledgementResults)

        if (error.isError()) {
            handleUnsuccessfulAcknowledgeRequest(error = error)
        } else {
            handleSuccessfulAcknowledgeRequest(acknowledgementResults = acknowledgementResults)
        }
    }

    private fun handleSuccessfulAcknowledgeRequest(acknowledgementResults: ArrayList<AcknowledgeVo?>) {
        val nonNullAcknowledgementResults = acknowledgementResults.mapNotNull { it }
        if (nonNullAcknowledgementResults.isEmpty()) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        } else if (nonNullAcknowledgementResults.count() > 1) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_MORE_THAN_ONE_RESULT
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_MORE_THAN_ONE_RESULT,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        }

        val onSuccess = inFlightRequest?.onSuccess
        clearInFlightRequest()
        onSuccess?.invoke(nonNullAcknowledgementResults[0])
    }

    private fun handleUnsuccessfulAcknowledgeRequest(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.ACKNOWLEDGE_REQUEST_ERRORED.format(
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
