package com.revenuecat.purchases.amazon.handler

import android.os.Handler
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.listener.UserDataResponseListener
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log

class UserDataHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider,
    private val mainHandler: Handler
) : UserDataResponseListener {

    companion object {
        private const val GET_USER_DATA_TIMEOUT_MILLIS = 10000L
    }

    private data class Request(
        val onReceive: (UserData) -> Unit,
        val onError: PurchasesErrorCallback
    )

    private val requests = mutableMapOf<RequestId, Request>()

    override fun onUserDataResponse(response: UserDataResponse) {
        // Amazon is catching all exceptions and swallowing them so we have to catch ourselves and log
        try {
            log(LogIntent.DEBUG, AmazonStrings.USER_DATA_REQUEST_FINISHED.format(response.requestStatus.name))

            val request = getRequest(response.requestId) ?: return

            when (response.requestStatus) {
                UserDataResponse.RequestStatus.SUCCESSFUL -> request.onReceive(response.userData)
                UserDataResponse.RequestStatus.FAILED ->
                    request.onError.invokeWithStoreProblem(AmazonStrings.ERROR_FAILED_USER_DATA)
                UserDataResponse.RequestStatus.NOT_SUPPORTED ->
                    request.onError.invokeWithStoreProblem(AmazonStrings.ERROR_UNSUPPORTED_USER_DATA)
                else -> request.onError.invokeWithStoreProblem(AmazonStrings.ERROR_USER_DATA_STORE_PROBLEM)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog("Exception in onUserDataResponse", e)
            throw e
        }
    }

    @SuppressWarnings("ForbiddenComment")
    override fun getUserData(
        onSuccess: (UserData) -> Unit,
        onError: PurchasesErrorCallback
    ) {
        val userDataRequestId = purchasingServiceProvider.getUserData()
        val request = Request(onSuccess, onError)
        synchronized(this) {
            requests[userDataRequestId] = request
            addTimeoutToUserDataRequest(userDataRequestId)
        }
    }

    private fun PurchasesErrorCallback.invokeWithStoreProblem(message: String) {
        this(PurchasesError(PurchasesErrorCode.StoreProblemError, message))
    }

    private fun addTimeoutToUserDataRequest(requestId: RequestId) {
        mainHandler.postDelayed(
            {
                val request = getRequest(requestId) ?: return@postDelayed
                val error = PurchasesError(
                    PurchasesErrorCode.UnknownError,
                    AmazonStrings.ERROR_TIMEOUT_GETTING_USER_DATA
                )
                request.onError(error)
            },
            GET_USER_DATA_TIMEOUT_MILLIS
        )
    }

    @Synchronized
    private fun getRequest(requestId: RequestId): Request? {
        return requests.remove(requestId)
    }
}
