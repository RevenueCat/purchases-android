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
import com.revenuecat.purchases.utils.DefaultTimestampProvider
import com.revenuecat.purchases.utils.TimestampProvider

internal class UserDataHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider,
    private val mainHandler: Handler,
    private val timestampProvider: TimestampProvider = DefaultTimestampProvider(),
) : UserDataResponseListener {

    companion object {
        private const val GET_USER_DATA_TIMEOUT_MILLIS = 10_000L
        private const val CACHE_EXPIRATION_TIME_MILLIS = 300_000L
    }

    private data class Request(
        val onReceive: (UserData) -> Unit,
        val onError: PurchasesErrorCallback,
    )

    @get:Synchronized
    private val requests = mutableMapOf<RequestId, Request>()

    @get:Synchronized @set:Synchronized
    private var userDataCache: UserData? = null

    @get:Synchronized @set:Synchronized
    private var lastUserDataRequestTimestamp: Long? = null

    override fun onUserDataResponse(response: UserDataResponse) {
        // Amazon is catching all exceptions and swallowing them so we have to catch ourselves and log
        try {
            log(LogIntent.DEBUG, AmazonStrings.USER_DATA_REQUEST_FINISHED.format(response.requestStatus.name))

            val request = getRequest(response.requestId) ?: return

            when (response.requestStatus) {
                UserDataResponse.RequestStatus.SUCCESSFUL -> {
                    synchronized(this) {
                        lastUserDataRequestTimestamp = timestampProvider.currentTimeMillis
                        userDataCache = response.userData
                    }
                    request.onReceive(response.userData)
                }
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
        onError: PurchasesErrorCallback,
    ) {
        getCachedUserDataIfAvailable()?.let { cachedUserData ->
            log(LogIntent.DEBUG, AmazonStrings.USER_DATA_REQUEST_FROM_CACHE)
            onSuccess(cachedUserData)
            return
        }
        val userDataRequestId = purchasingServiceProvider.getUserData()
        val request = Request(onSuccess, onError)
        synchronized(this) {
            requests[userDataRequestId] = request
            addTimeoutToUserDataRequest(userDataRequestId)
        }
    }

    @Synchronized
    private fun getCachedUserDataIfAvailable(): UserData? {
        userDataCache?.let { userData ->
            lastUserDataRequestTimestamp?.let { lastUserDataRequestTimestamp ->
                if (timestampProvider.currentTimeMillis - lastUserDataRequestTimestamp < CACHE_EXPIRATION_TIME_MILLIS) {
                    return userData
                }
            }
        }
        return null
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
                    AmazonStrings.ERROR_TIMEOUT_GETTING_USER_DATA,
                )
                request.onError(error)
            },
            GET_USER_DATA_TIMEOUT_MILLIS,
        )
    }

    @Synchronized
    private fun getRequest(requestId: RequestId): Request? {
        return requests.remove(requestId)
    }
}
