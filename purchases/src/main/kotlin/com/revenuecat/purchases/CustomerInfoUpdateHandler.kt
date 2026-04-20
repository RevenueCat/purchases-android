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
@Suppress("TooManyFunctions")
internal class CustomerInfoUpdateHandler constructor(
    private val deviceCache: DeviceCache,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val appConfig: AppConfig,
    private val diagnosticsTracker: DiagnosticsTracker?,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    private class ListenerState(
        val listener: UpdatedCustomerInfoListener,
    ) {
        var lastDeliveredCustomerInfo: CustomerInfo? = null
        var pendingInitialCustomerInfo: CustomerInfo? = null
    }

    private var legacyUpdatedCustomerInfoListener: UpdatedCustomerInfoListener? = null

    @Deprecated("Use addUpdatedCustomerInfoListener/removeUpdatedCustomerInfoListener instead")
    var updatedCustomerInfoListener: UpdatedCustomerInfoListener?
        @Synchronized get() = legacyUpdatedCustomerInfoListener
        set(value) {
            synchronized(this@CustomerInfoUpdateHandler) {
                legacyUpdatedCustomerInfoListener = value
            }
            afterSetLegacyListener(value)
        }

    private val listeners = mutableListOf<ListenerState>()
    private var legacyListenerState: ListenerState? = null

    private var lastSentCustomerInfo: CustomerInfo? = null

    fun addUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
        val listenerState = synchronized(this@CustomerInfoUpdateHandler) {
            listeners.firstOrNull { it.listener === listener }
                ?: ListenerState(listener).also { listeners.add(it) }
        }
        if (!appConfig.customEntitlementComputation) {
            if (synchronized(this@CustomerInfoUpdateHandler) { listenerState.lastDeliveredCustomerInfo == null }) {
                getCachedCustomerInfo(identityManager.currentAppUserID)?.let { cachedInfo ->
                    sendToSingleListener(listenerState, cachedInfo)
                }
            }
        }
    }

    fun removeUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.indexOfFirst { it.listener === listener }
                .takeIf { it >= 0 }
                ?.let { listeners.removeAt(it) }
        }
    }

    @Suppress("DEPRECATION")
    fun removeAllListeners() {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.clear()
            legacyListenerState = null
            legacyUpdatedCustomerInfoListener = null
        }
    }

    fun cacheAndNotifyListeners(customerInfo: CustomerInfo) {
        deviceCache.cacheCustomerInfo(identityManager.currentAppUserID, customerInfo)
        notifyListeners(customerInfo)
    }

    @Suppress("DEPRECATION")
    fun notifyListeners(customerInfo: CustomerInfo) {
        val (listenerStatesToNotify, lastSent) = synchronized(this@CustomerInfoUpdateHandler) {
            val currentLastSent = lastSentCustomerInfo
            if (currentLastSent == customerInfo) {
                emptyList<ListenerState>() to currentLastSent
            } else {
                val listenerStates = buildList {
                    legacyListenerState?.let { listenerState ->
                        if (listenerState.lastDeliveredCustomerInfo != customerInfo) {
                            listenerState.lastDeliveredCustomerInfo = customerInfo
                            listenerState.pendingInitialCustomerInfo = null
                            add(listenerState)
                        } else {
                            listenerState.pendingInitialCustomerInfo = null
                        }
                    }
                    listeners.forEach { listenerState ->
                        if (listenerState.lastDeliveredCustomerInfo != customerInfo) {
                            listenerState.lastDeliveredCustomerInfo = customerInfo
                            listenerState.pendingInitialCustomerInfo = null
                            add(listenerState)
                        } else {
                            listenerState.pendingInitialCustomerInfo = null
                        }
                    }
                }
                listenerStates to currentLastSent
            }
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
            listenerStatesToNotify.forEach { listenerState ->
                dispatch { listenerState.listener.onReceived(customerInfo) }
            }
        }
    }

    private fun afterSetLegacyListener(listener: UpdatedCustomerInfoListener?) {
        val listenerState = synchronized(this@CustomerInfoUpdateHandler) {
            if (listener == null) {
                legacyListenerState = null
                null
            } else {
                ListenerState(listener).also { legacyListenerState = it }
            }
        }
        if (listener != null) {
            log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
            sendCachedCustomerInfoToLegacyListener(listenerState)
        }
    }

    private fun sendCachedCustomerInfoToLegacyListener(listenerState: ListenerState?) {
        if (appConfig.customEntitlementComputation || listenerState == null) return
        val cachedInfo = getCachedCustomerInfo(identityManager.currentAppUserID) ?: return
        if (synchronized(this@CustomerInfoUpdateHandler) { lastSentCustomerInfo } != cachedInfo) {
            diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(cachedInfo)
        }
        sendToSingleListener(listenerState, cachedInfo)
    }

    private fun sendToSingleListener(listenerState: ListenerState, customerInfo: CustomerInfo) {
        synchronized(this@CustomerInfoUpdateHandler) {
            if (!contains(listenerState)) return
            listenerState.pendingInitialCustomerInfo = customerInfo
        }
        log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
        dispatch {
            val listener = synchronized(this@CustomerInfoUpdateHandler) {
                if (!contains(listenerState) || listenerState.pendingInitialCustomerInfo != customerInfo) {
                    null
                } else {
                    listenerState.pendingInitialCustomerInfo = null
                    if (listenerState.lastDeliveredCustomerInfo == customerInfo) {
                        null
                    } else {
                        listenerState.lastDeliveredCustomerInfo = customerInfo
                        listenerState.listener
                    }
                }
            }
            listener?.onReceived(customerInfo)
        }
    }

    private fun getCachedCustomerInfo(appUserID: String): CustomerInfo? {
        return offlineEntitlementsManager.offlineCustomerInfo
            ?: deviceCache.getCachedCustomerInfo(appUserID)
    }

    private fun contains(listenerState: ListenerState): Boolean {
        return legacyListenerState === listenerState || listeners.contains(listenerState)
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != handler.looper.thread) {
            handler.post(action)
        } else {
            action()
        }
    }
}
