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

    /**
     * Per-listener delivery bookkeeping.
     *
     * [lastDeliveredCustomerInfo] lets each listener dedup independently, so a listener that
     * already received a value through its initial delivery is not notified again by a
     * subsequent broadcast of that same value.
     *
     * [initialDeliveryPending] guards the initial cached-info delivery, which is dispatched
     * asynchronously. If a broadcast reaches this listener while that delivery is still queued,
     * the broadcast clears the flag and the queued delivery is dropped, so a stale cached value
     * can never land after a newer one.
     */
    private class ListenerState(
        val listener: UpdatedCustomerInfoListener,
    ) {
        var lastDeliveredCustomerInfo: CustomerInfo? = null
        var initialDeliveryPending: Boolean = false
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
            sendCachedCustomerInfoToNewListener(listenerState, trackVerification = false)
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
        // Claim the broadcast and mark every listener under a single lock, so the shared dedup
        // gate can never advance without the per-listener bookkeeping advancing with it.
        val notification = synchronized(this@CustomerInfoUpdateHandler) {
            val previouslySent = lastSentCustomerInfo
            if (previouslySent == customerInfo) {
                null
            } else {
                lastSentCustomerInfo = customerInfo
                val statesToNotify = buildList {
                    (listOfNotNull(legacyListenerState) + listeners).forEach { listenerState ->
                        // Any broadcast supersedes a queued initial delivery.
                        listenerState.initialDeliveryPending = false
                        if (listenerState.lastDeliveredCustomerInfo != customerInfo) {
                            listenerState.lastDeliveredCustomerInfo = customerInfo
                            add(listenerState)
                        }
                    }
                }
                previouslySent to statesToNotify
            }
        } ?: return

        val (previouslySent, statesToNotify) = notification
        diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(customerInfo)
        if (previouslySent != null) {
            log(LogIntent.DEBUG) { CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER }
        } else {
            log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
        }
        statesToNotify.forEach { listenerState ->
            dispatch { listenerState.listener.onReceived(customerInfo) }
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
        if (listenerState != null) {
            log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
            if (!appConfig.customEntitlementComputation) {
                sendCachedCustomerInfoToNewListener(listenerState, trackVerification = true)
            }
        }
    }

    /**
     * Delivers the currently cached customer info to a freshly registered listener, without
     * touching the shared [lastSentCustomerInfo] gate. That gate is only ever advanced by
     * [notifyListeners]; rolling it backwards here would re-broadcast to every other listener.
     */
    private fun sendCachedCustomerInfoToNewListener(
        listenerState: ListenerState,
        trackVerification: Boolean,
    ) {
        if (!reserveInitialDelivery(listenerState)) return

        val cachedInfo = getCachedCustomerInfo(identityManager.currentAppUserID)
        if (cachedInfo == null) {
            clearInitialDeliveryReservation(listenerState)
            return
        }
        if (trackVerification &&
            synchronized(this@CustomerInfoUpdateHandler) { lastSentCustomerInfo } != cachedInfo
        ) {
            diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(cachedInfo)
        }
        sendToSingleListener(listenerState, cachedInfo)
    }

    private fun sendToSingleListener(listenerState: ListenerState, customerInfo: CustomerInfo) {
        log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
        dispatch {
            // Re-check under the lock at delivery time: the listener may have been removed, or a
            // broadcast may have overtaken this queued delivery and cleared the reservation.
            val listener = synchronized(this@CustomerInfoUpdateHandler) {
                if (!contains(listenerState) || !listenerState.initialDeliveryPending) {
                    null
                } else {
                    listenerState.initialDeliveryPending = false
                    listenerState.lastDeliveredCustomerInfo = customerInfo
                    listenerState.listener
                }
            }
            listener?.onReceived(customerInfo)
        }
    }

    /**
     * Reserves the one-shot initial delivery for [listenerState], returning false when the
     * listener has already received a value or already has a delivery in flight.
     */
    private fun reserveInitialDelivery(listenerState: ListenerState): Boolean {
        return synchronized(this@CustomerInfoUpdateHandler) {
            if (listenerState.lastDeliveredCustomerInfo != null || listenerState.initialDeliveryPending) {
                false
            } else {
                listenerState.initialDeliveryPending = true
                true
            }
        }
    }

    private fun clearInitialDeliveryReservation(listenerState: ListenerState) {
        synchronized(this@CustomerInfoUpdateHandler) {
            listenerState.initialDeliveryPending = false
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
