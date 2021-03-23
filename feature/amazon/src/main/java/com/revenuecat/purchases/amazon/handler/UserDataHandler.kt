package com.revenuecat.purchases.amazon.handler

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
import com.revenuecat.purchases.common.log

typealias UserDataCallbacks = Pair<(UserData) -> Unit, PurchasesErrorCallback>

class UserDataHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider
) : UserDataResponseListener {

    private val requests = mutableMapOf<RequestId, UserDataCallbacks>()

    override fun onUserDataResponse(response: UserDataResponse) {
        log(LogIntent.DEBUG, AmazonStrings.USER_DATA_REQUEST_FINISHED.format(response.requestStatus.name))

        val callbacks = synchronized(this) { requests.remove(response.requestId) }

        callbacks?.let { (onSuccess, onError) ->
            when (response.requestStatus) {
                UserDataResponse.RequestStatus.SUCCESSFUL -> onSuccess(response.userData)
                UserDataResponse.RequestStatus.FAILED ->
                    onError.invokeWithStoreProblem(AmazonStrings.ERROR_FAILED_USER_DATA)
                UserDataResponse.RequestStatus.NOT_SUPPORTED ->
                    onError.invokeWithStoreProblem(AmazonStrings.ERROR_UNSUPPORTED_USER_DATA)
                else -> onError.invokeWithStoreProblem(AmazonStrings.ERROR_USER_DATA_STORE_PROBLEM)
            }
        }
    }

    @SuppressWarnings("ForbiddenComment")
    override fun getUserData(
        onSuccess: (UserData) -> Unit,
        onError: PurchasesErrorCallback
    ) {
        val userDataRequestId = purchasingServiceProvider.getUserData()
        requests[userDataRequestId] = onSuccess to onError
    }

    private fun PurchasesErrorCallback.invokeWithStoreProblem(message: String) {
        this(PurchasesError(PurchasesErrorCode.StoreProblemError, message))
    }
}
