package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.amazon.listener.UserDataResponseListener

class UserDataHandler : UserDataResponseListener {

    private val requests = mutableMapOf<RequestId, (UserData) -> Unit>()

    @SuppressWarnings("ForbiddenComment")
    override fun onUserDataResponse(response: UserDataResponse) {
        // TODO: handle status of response
        requests.remove(response.requestId)?.let { completion ->
            completion(response.userData)
        }
    }

    @SuppressWarnings("ForbiddenComment")
    override fun getUserData(
        onCompletion: (UserData) -> Unit
    ) {
        // TODO: add cache
        val userDataRequestId = PurchasingService.getUserData()
        requests[userDataRequestId] = onCompletion
    }
}
