package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.CustomerInfoStrings

/**
 * This class is responsible for updating the customer info cache and notifying the listeners.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class CustomerInfoUpdateHandler constructor(
    private val deviceCache: DeviceCache,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val appConfig: AppConfig,
    private val diagnosticsTracker: DiagnosticsTracker?,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    @Deprecated("Use addUpdatedCustomerInfoListener/removeUpdatedCustomerInfoListener instead")
    var updatedCustomerInfoListener: UpdatedCustomerInfoListener? = null
        @Synchronized get
        set(value) {
            synchronized(this@CustomerInfoUpdateHandler) {
                field = value
            }
            afterSetLegacyListener(value)
        }

    private val listeners = mutableListOf<UpdatedCustomerInfoListener>()

    private var lastSentCustomerInfo: CustomerInfo? = null

    fun addUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.add(listener)
        }
        log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
        if (!appConfig.customEntitlementComputation) {
            getCachedCustomerInfo(identityManager.currentAppUserID)?.let { cachedInfo ->
                sendToSingleListener(listener, cachedInfo)
            }
        }
    }

    fun removeUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.remove(listener)
        }
    }

    @Suppress("DEPRECATION")
    fun removeAllListeners() {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.clear()
            updatedCustomerInfoListener = null
        }
    }

    fun cacheAndNotifyListeners(customerInfo: CustomerInfo) {
        deviceCache.cacheCustomerInfo(identityManager.currentAppUserID, customerInfo)
        notifyListeners(customerInfo)
    }

    @Suppress("DEPRECATION")
    fun notifyListeners(customerInfo: CustomerInfo) {
        val (legacyListener, addedListeners, lastSent) = synchronized(this@CustomerInfoUpdateHandler) {
            Triple(updatedCustomerInfoListener, listeners.toList(), lastSentCustomerInfo)
        }
        if (lastSent != customerInfo) {
            diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(customerInfo)
            if (lastSent != null) {
                log(LogIntent.DEBUG) { CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER }
            } else {
                log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
            }
            synchronized(this@CustomerInfoUpdateHandler) {
                this.lastSentCustomerInfo = customerInfo
            }
            legacyListener?.let { dispatch { it.onReceived(customerInfo) } }
            addedListeners.forEach { listener ->
                dispatch { listener.onReceived(customerInfo) }
            }
        }
    }

    private fun afterSetLegacyListener(listener: UpdatedCustomerInfoListener?) {
        if (listener != null) {
            log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
            if (!appConfig.customEntitlementComputation) {
                getCachedCustomerInfo(identityManager.currentAppUserID)?.let {
                    notifyListeners(it)
                }
            }
        }
    }

    private fun sendToSingleListener(listener: UpdatedCustomerInfoListener, customerInfo: CustomerInfo) {
        synchronized(this@CustomerInfoUpdateHandler) {
            if (lastSentCustomerInfo != customerInfo) {
                diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(customerInfo)
                this.lastSentCustomerInfo = customerInfo
            }
        }
        log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
        dispatch { listener.onReceived(customerInfo) }
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
