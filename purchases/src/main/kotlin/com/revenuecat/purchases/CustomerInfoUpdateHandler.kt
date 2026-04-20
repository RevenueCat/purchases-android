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
        var pendingInitialDeliveryId: Long? = null
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
    private var nextInitialDeliveryId: Long = 0

    fun addUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
        val listenerState = synchronized(this@CustomerInfoUpdateHandler) {
            listeners.firstOrNull { it.listener === listener }
                ?: ListenerState(listener).also { listeners.add(it) }
        }
        if (!appConfig.customEntitlementComputation) {
            val initialDeliveryId = reserveInitialDelivery(listenerState)
            if (initialDeliveryId != null) {
                getCachedCustomerInfo(identityManager.currentAppUserID)?.let { cachedInfo ->
                    sendToSingleListener(listenerState, initialDeliveryId, cachedInfo)
                } ?: clearInitialDeliveryReservation(listenerState, initialDeliveryId)
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
                            listenerState.pendingInitialDeliveryId = null
                            add(listenerState)
                        } else {
                            listenerState.pendingInitialDeliveryId = null
                        }
                    }
                    listeners.forEach { listenerState ->
                        if (listenerState.lastDeliveredCustomerInfo != customerInfo) {
                            listenerState.lastDeliveredCustomerInfo = customerInfo
                            listenerState.pendingInitialDeliveryId = null
                            add(listenerState)
                        } else {
                            listenerState.pendingInitialDeliveryId = null
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
        val initialDeliveryId = reserveInitialDelivery(listenerState)
        if (initialDeliveryId != null) {
            val cachedInfo = getCachedCustomerInfo(identityManager.currentAppUserID)
            if (cachedInfo != null) {
                if (synchronized(this@CustomerInfoUpdateHandler) { lastSentCustomerInfo } != cachedInfo) {
                    diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(cachedInfo)
                }
                sendToSingleListener(listenerState, initialDeliveryId, cachedInfo)
            } else {
                clearInitialDeliveryReservation(listenerState, initialDeliveryId)
            }
        }
    }

    private fun sendToSingleListener(
        listenerState: ListenerState,
        initialDeliveryId: Long,
        customerInfo: CustomerInfo,
    ) {
        log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
        dispatch {
            val listener = synchronized(this@CustomerInfoUpdateHandler) {
                if (
                    !contains(listenerState) ||
                    listenerState.pendingInitialDeliveryId != initialDeliveryId
                ) {
                    null
                } else {
                    listenerState.pendingInitialDeliveryId = null
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

    private fun reserveInitialDelivery(listenerState: ListenerState): Long? {
        return synchronized(this@CustomerInfoUpdateHandler) {
            if (listenerState.lastDeliveredCustomerInfo != null || listenerState.pendingInitialDeliveryId != null) {
                null
            } else {
                (++nextInitialDeliveryId).also { listenerState.pendingInitialDeliveryId = it }
            }
        }
    }

    private fun clearInitialDeliveryReservation(listenerState: ListenerState, initialDeliveryId: Long) {
        synchronized(this@CustomerInfoUpdateHandler) {
            if (listenerState.pendingInitialDeliveryId == initialDeliveryId) {
                listenerState.pendingInitialDeliveryId = null
            }
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
