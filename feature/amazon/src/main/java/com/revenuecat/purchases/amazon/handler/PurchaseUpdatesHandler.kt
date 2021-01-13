package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.listener.PurchaseUpdatesResponseListener
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log

class PurchaseUpdatesHandler : PurchaseUpdatesResponseListener {

    private val requests = mutableMapOf<RequestId, Pair<(List<Receipt>, UserData) -> Unit, (PurchasesError) -> Unit>>()

    override fun queryPurchases(
        onSuccess: (List<Receipt>, UserData) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val reset = true
        val requestId = PurchasingService.getPurchaseUpdates(reset)
        synchronized(this) {
            requests[requestId] = onSuccess to onError
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        log(LogIntent.DEBUG, AmazonStrings.RETRIEVED_PRODUCT_DATA.format(response))

        val requestId = response.requestId

        val callbacks = synchronized(this) {
            requests.remove(requestId)
        }

        callbacks?.let { (onSuccess, onError) ->
            when (response.requestStatus) {
                PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> onSuccess(response.receipts, response.userData)
                PurchaseUpdatesResponse.RequestStatus.FAILED -> onFailedPurchaseUpdates(onError)
                PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED -> onNotSupportedPurchaseUpdates(onError)
                null -> onUnknownError(onError)
            }
        }
    }

    private fun onFailedPurchaseUpdates(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.StoreProblemError,
            "Failed to get purchase updates."
        ).let {
            onError(it)
        }
    }

    private fun onNotSupportedPurchaseUpdates(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.StoreProblemError,
            "Failed to get purchase updates. Call is not supported. Request will retry."
        ).let {
            onError(it)
        }
    }

    private fun onUnknownError(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.StoreProblemError,
            "Failed to get purchase updates. There was an Amazon store problem"
        ).let {
            onError(it)
        }
    }
}
