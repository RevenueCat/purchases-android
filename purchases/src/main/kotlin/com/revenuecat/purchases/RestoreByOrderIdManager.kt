package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.RestoreByOrderIdListener
import com.revenuecat.purchases.utils.RateLimiter
import kotlin.time.Duration.Companion.seconds

internal class RestoreByOrderIdManager(
    private val backend: Backend,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
    private val rateLimiter: RateLimiter = RateLimiter(maxCallsInPeriod = 5, periodSeconds = 60.seconds),
) {
    fun restoreByOrderId(
        orderId: String,
        listener: RestoreByOrderIdListener,
    ) {
        if (!rateLimiter.shouldProceed()) {
            debugLog { "Rate limit exceeded for restorePurchaseByOrderId." }
            dispatchResult(listener, RestoreByOrderIdListener.Result.RateLimitExceeded)
            return
        }
        debugLog { "Starting restore purchase by order ID: $orderId" }
        backend.postRestoreByOrderId(
            appUserID = identityManager.currentAppUserID,
            orderId = orderId,
            onResultHandler = { result ->
                when (result) {
                    is RestoreByOrderIdListener.Result.Success -> {
                        debugLog { "Successfully restored purchase by order ID. Updating customer info." }
                        offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                        customerInfoUpdateHandler.cacheAndNotifyListeners(result.customerInfo)
                        dispatchResult(listener, result)
                    }
                    else -> {
                        errorLog { "Error restoring purchase by order ID: $result" }
                        dispatchResult(listener, result)
                    }
                }
            },
        )
    }

    private fun dispatchResult(
        listener: RestoreByOrderIdListener,
        result: RestoreByOrderIdListener.Result,
    ) {
        dispatch { listener.handleResult(result) }
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val handler = mainHandler ?: Handler(Looper.getMainLooper())
            handler.post(action)
        } else {
            action()
        }
    }
}
