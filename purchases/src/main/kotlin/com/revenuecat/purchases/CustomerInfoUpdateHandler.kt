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
 *
 * All registered listeners live in a single [listeners] list, including the one set through the
 * deprecated [updatedCustomerInfoListener] property. That property is just a single-slot view onto
 * the same list, tracked by [legacyListenerState], so that registering the same listener through
 * both APIs cannot deliver twice and either API can remove it.
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
     * already received a value through its initial delivery is not notified again by a subsequent
     * broadcast of that same value. It is only ever set at delivery time, so a listener is never
     * marked as having received a value it did not actually receive.
     *
     * [initialDeliveryPending] guards the initial cached-info delivery, which is dispatched
     * asynchronously. If a broadcast reaches this listener while that delivery is still queued,
     * the broadcast clears the flag and the queued delivery drops itself, so a stale cached value
     * can never land after a newer one.
     */
    private class ListenerState(
        val listener: UpdatedCustomerInfoListener,
    ) {
        var lastDeliveredCustomerInfo: CustomerInfo? = null
        var initialDeliveryPending: Boolean = false
    }

    private val listeners = mutableListOf<ListenerState>()

    /** The entry in [listeners] currently owned by the deprecated single-listener property. */
    private var legacyListenerState: ListenerState? = null

    private var lastSentCustomerInfo: CustomerInfo? = null

    /**
     * Verification diagnostics describe a customer info *value*, not a listener, so they are
     * deduped by value here rather than piggybacking on the delivery gate.
     */
    private var lastTrackedCustomerInfo: CustomerInfo? = null

    @Deprecated("Use addUpdatedCustomerInfoListener/removeUpdatedCustomerInfoListener instead")
    var updatedCustomerInfoListener: UpdatedCustomerInfoListener?
        @Synchronized get() = legacyListenerState?.listener
        set(value) {
            val listenerState = synchronized(this@CustomerInfoUpdateHandler) {
                val previousLegacy = legacyListenerState
                if (value == null) {
                    previousLegacy?.let { listeners.remove(it) }
                    legacyListenerState = null
                    null
                } else {
                    // Reuse the existing entry when this listener is already registered, so
                    // re-assigning the same instance stays a no-op instead of re-delivering.
                    val existing = listeners.firstOrNull { it.listener === value }
                    if (previousLegacy != null && previousLegacy !== existing) {
                        listeners.remove(previousLegacy)
                    }
                    (existing ?: ListenerState(value).also { listeners.add(it) })
                        .also { legacyListenerState = it }
                }
            }
            if (listenerState != null) {
                log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
                sendCachedCustomerInfoToNewListener(listenerState)
            }
        }

    fun addUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
        val listenerState = synchronized(this@CustomerInfoUpdateHandler) {
            listeners.firstOrNull { it.listener === listener }
                ?: ListenerState(listener).also { listeners.add(it) }
        }
        sendCachedCustomerInfoToNewListener(listenerState)
    }

    /**
     * Removes [listener] however it was registered, including through the deprecated property.
     * Otherwise a listener set through the property could never be removed by reference, and
     * would keep receiving callbacks while being strongly held.
     */
    fun removeUpdatedCustomerInfoListener(listener: UpdatedCustomerInfoListener) {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.firstOrNull { it.listener === listener }?.let { listenerState ->
                listeners.remove(listenerState)
                if (legacyListenerState === listenerState) {
                    legacyListenerState = null
                }
            }
        }
    }

    fun removeAllListeners() {
        synchronized(this@CustomerInfoUpdateHandler) {
            listeners.clear()
            legacyListenerState = null
        }
    }

    fun cacheAndNotifyListeners(customerInfo: CustomerInfo) {
        cacheAndNotifyListeners(customerInfo, identityManager.currentAppUserID)
    }

    fun cacheAndNotifyListeners(customerInfo: CustomerInfo, appUserID: String) {
        deviceCache.cacheCustomerInfo(appUserID, customerInfo)
        notifyListeners(customerInfo)
    }

    fun notifyListeners(customerInfo: CustomerInfo) {
        // Claim the broadcast under a single lock. Listeners are selected here but only marked as
        // delivered at delivery time, so one listener throwing cannot make the update look
        // delivered to the listeners behind it.
        val notification = synchronized(this@CustomerInfoUpdateHandler) {
            val previouslySent = lastSentCustomerInfo
            if (previouslySent == customerInfo) {
                null
            } else {
                lastSentCustomerInfo = customerInfo
                val statesToNotify = listeners.filter { it.lastDeliveredCustomerInfo != customerInfo }
                // Any broadcast supersedes a queued initial delivery.
                statesToNotify.forEach { it.initialDeliveryPending = false }
                previouslySent to statesToNotify
            }
        } ?: return

        val (previouslySent, statesToNotify) = notification
        trackVerificationResultIfNeeded(customerInfo)
        if (previouslySent != null) {
            log(LogIntent.DEBUG) { CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER }
        } else {
            log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
        }
        statesToNotify.forEach { listenerState ->
            deliver(listenerState, customerInfo)
        }
    }

    /**
     * Delivers the currently cached customer info to a freshly registered listener, without
     * touching the shared [lastSentCustomerInfo] gate. That gate is only ever advanced by
     * [notifyListeners]; rolling it backwards here would re-broadcast to every other listener.
     */
    private fun sendCachedCustomerInfoToNewListener(listenerState: ListenerState) {
        if (appConfig.customEntitlementComputation) return
        if (!reserveInitialDelivery(listenerState)) return

        val cachedInfo = getCachedCustomerInfo(identityManager.currentAppUserID)
        if (cachedInfo == null) {
            clearInitialDeliveryReservation(listenerState)
        } else {
            trackVerificationResultIfNeeded(cachedInfo)
            log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
            deliver(listenerState, cachedInfo, isInitialDelivery = true)
        }
    }

    /**
     * Dispatches [customerInfo] to a single listener, re-checking under the lock at delivery time:
     * the listener may have been removed, or a broadcast may have overtaken a queued initial
     * delivery and cleared its reservation.
     */
    private fun deliver(
        listenerState: ListenerState,
        customerInfo: CustomerInfo,
        isInitialDelivery: Boolean = false,
    ) {
        dispatch {
            val listener = synchronized(this@CustomerInfoUpdateHandler) {
                val superseded = isInitialDelivery && !listenerState.initialDeliveryPending
                if (
                    !listeners.contains(listenerState) ||
                    superseded ||
                    listenerState.lastDeliveredCustomerInfo == customerInfo
                ) {
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

    private fun trackVerificationResultIfNeeded(customerInfo: CustomerInfo) {
        val shouldTrack = synchronized(this@CustomerInfoUpdateHandler) {
            if (lastTrackedCustomerInfo == customerInfo) {
                false
            } else {
                lastTrackedCustomerInfo = customerInfo
                true
            }
        }
        if (shouldTrack) {
            diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(customerInfo)
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
