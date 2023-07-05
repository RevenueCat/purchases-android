package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.listener.PurchaseUpdatesResponseListener
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log

private typealias QueryPurchasesSuccessCallback = (List<Receipt>, UserData) -> Unit

private typealias QueryPurchasesCallbacks = Pair<QueryPurchasesSuccessCallback, PurchasesErrorCallback>

class PurchaseUpdatesHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider,
) : PurchaseUpdatesResponseListener {

    private val requests = mutableMapOf<RequestId, QueryPurchasesCallbacks>()

    override fun queryPurchases(
        onSuccess: QueryPurchasesSuccessCallback,
        onError: PurchasesErrorCallback,
    ) {
        val reset = true
        val requestId = purchasingServiceProvider.getPurchaseUpdates(reset)
        synchronized(this) {
            requests[requestId] = onSuccess to onError
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        // Amazon is catching all exceptions and swallowing them so we have to catch ourselves and log
        try {
            log(LogIntent.DEBUG, AmazonStrings.RETRIEVED_PRODUCT_DATA.format(response))

            val requestId = response.requestId

            val callbacks = synchronized(this) {
                requests.remove(requestId)
            }

            callbacks?.let { (onSuccess, onError) ->
                when (response.requestStatus) {
                    PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL ->
                        onSuccess(response.receipts, response.userData)
                    PurchaseUpdatesResponse.RequestStatus.FAILED ->
                        onError.invokeWithStoreProblem(AmazonStrings.ERROR_FAILED_PURCHASES_UPDATES)
                    PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED ->
                        onError.invokeWithStoreProblem(AmazonStrings.ERROR_UNSUPPORTED_PURCHASES_UPDATES)
                    null ->
                        onError.invokeWithStoreProblem(AmazonStrings.ERROR_PURCHASES_UPDATES_STORE_PROBLEM)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog("Exception in onPurchaseUpdatesResponse", e)
            throw e
        }
    }

    private fun PurchasesErrorCallback.invokeWithStoreProblem(message: String) {
        this(PurchasesError(PurchasesErrorCode.StoreProblemError, message))
    }
}
