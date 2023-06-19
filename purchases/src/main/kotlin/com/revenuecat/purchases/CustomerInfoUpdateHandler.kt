package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.CustomerInfoStrings

/**
 * This class is responsible for updating the customer info cache and notifying the listeners.
 */
internal class CustomerInfoUpdateHandler(
    private val deviceCache: DeviceCache,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    var updatedCustomerInfoListener: UpdatedCustomerInfoListener? = null
        @Synchronized get
        set(value) {
            synchronized(this@CustomerInfoUpdateHandler) {
                field = value
            }
            afterSetListener(value)
        }

    private var lastSentCustomerInfo: CustomerInfo? = null

    fun cacheAndNotifyListeners(customerInfo: CustomerInfo) {
        deviceCache.cacheCustomerInfo(identityManager.currentAppUserID, customerInfo)
        notifyListeners(customerInfo)
    }

    fun notifyListeners(customerInfo: CustomerInfo) {
        synchronized(this@CustomerInfoUpdateHandler) { updatedCustomerInfoListener to lastSentCustomerInfo }
            .let { (listener, lastSentCustomerInfo) ->
                if (listener != null && lastSentCustomerInfo != customerInfo) {
                    if (lastSentCustomerInfo != null) {
                        log(LogIntent.DEBUG, CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER)
                    } else {
                        log(LogIntent.DEBUG, CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER)
                    }
                    synchronized(this@CustomerInfoUpdateHandler) {
                        this.lastSentCustomerInfo = customerInfo
                    }
                    dispatch { listener.onReceived(customerInfo) }
                }
            }
    }

    private fun afterSetListener(listener: UpdatedCustomerInfoListener?) {
        if (listener != null) {
            log(LogIntent.DEBUG, ConfigureStrings.LISTENER_SET)
            getCachedCustomerInfo(identityManager.currentAppUserID)?.let {
                notifyListeners(it)
            }
        }
    }

    private fun getCachedCustomerInfo(appUserID: String): CustomerInfo? {
        return offlineEntitlementsManager.offlineCustomerInfo
            ?: deviceCache.getCachedCustomerInfo(appUserID)
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != handler.looper.thread) {
            handler.post(action)
        } else {
            action()
        }
    }
}
