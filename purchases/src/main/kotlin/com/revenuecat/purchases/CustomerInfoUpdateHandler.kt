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
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager

/**
 * This class is responsible for updating the customer info cache and notifying the listeners.
 */
@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class CustomerInfoUpdateHandler constructor(
    private val deviceCache: DeviceCache,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val appConfig: AppConfig,
    private val diagnosticsTracker: DiagnosticsTracker?,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val promotionalEntitlementExpirationScheduler: PromotionalEntitlementExpirationScheduler =
        PromotionalEntitlementExpirationScheduler(handler),
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

    // Counts the CustomerInfos notified, to tell whether a newer one arrived meanwhile. Only compared for
    // equality, so overflowing is harmless.
    private var notificationCount = 0

    fun cacheAndNotifyListeners(customerInfo: CustomerInfo, appUserID: String) {
        deviceCache.cacheCustomerInfo(appUserID, customerInfo)
        subscriberAttributesManager.mergeAttributes(customerInfo, appUserID)
        notifyListeners(customerInfo, appUserID)
    }

    fun notifyListeners(customerInfo: CustomerInfo, appUserID: String) {
        notifyListeners(customerInfo, appUserID, expectedNotificationCount = null)
    }

    private fun notifyListeners(customerInfo: CustomerInfo, appUserID: String, expectedNotificationCount: Int?) {
        val currentAppUserID = identityManager.currentAppUserID
        if (appUserID != currentAppUserID) {
            log(LogIntent.DEBUG) {
                CustomerInfoStrings.NOT_NOTIFYING_LISTENER_CUSTOMERINFO_FOR_PREVIOUS_USER.format(
                    appUserID,
                    currentAppUserID,
                )
            }
            return
        }
        synchronized(this@CustomerInfoUpdateHandler) {
            if (expectedNotificationCount != null && expectedNotificationCount != notificationCount) return
            notificationCount++
            // Inside the lock so that the tracked CustomerInfo is always the last one notified.
            trackPromotionalEntitlementExpirations(customerInfo)
            updatedCustomerInfoListener to lastSentCustomerInfo
        }
            .let { (listener, lastSentCustomerInfo) ->
                if (lastSentCustomerInfo != customerInfo) {
                    diagnosticsTracker?.trackCustomerInfoVerificationResultIfNeeded(customerInfo)
                }
                if (listener != null && lastSentCustomerInfo != customerInfo) {
                    if (lastSentCustomerInfo != null) {
                        log(LogIntent.DEBUG) { CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER }
                    } else {
                        log(LogIntent.DEBUG) { CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER }
                    }
                    synchronized(this@CustomerInfoUpdateHandler) {
                        this.lastSentCustomerInfo = customerInfo
                    }
                    dispatch { listener.onReceived(customerInfo) }
                }
            }
    }

    /**
     * Call when the app comes to the foreground: the expiration is not reported while the device is in deep sleep.
     */
    fun checkPromotionalEntitlementExpirations() {
        promotionalEntitlementExpirationScheduler.reschedule()
    }

    fun close() {
        promotionalEntitlementExpirationScheduler.close()
    }

    private fun afterSetListener(listener: UpdatedCustomerInfoListener?) {
        if (listener != null) {
            log(LogIntent.DEBUG) { ConfigureStrings.LISTENER_SET }
            if (!appConfig.customEntitlementComputation) {
                val appUserID = identityManager.currentAppUserID
                getCachedCustomerInfo(appUserID)?.let {
                    notifyListeners(it, appUserID)
                }
            }
        }
    }

    // DeviceCache builds the cached CustomerInfo again on every read, which makes the expired entitlement inactive.
    private fun onPromotionalEntitlementExpirationReached() {
        // Without a listener there is nobody to notify. Setting one later sends it the cached CustomerInfo
        // and tracks that, so this expiration is not lost.
        val notificationCountBeforeReading = synchronized(this) {
            if (updatedCustomerInfoListener == null) return
            notificationCount
        }
        val appUserID = identityManager.currentAppUserID
        getCachedCustomerInfo(appUserID)?.let {
            // A CustomerInfo notified while reading the cache is newer than the one just read.
            notifyListeners(it, appUserID, expectedNotificationCount = notificationCountBeforeReading)
        }
    }

    private fun trackPromotionalEntitlementExpirations(customerInfo: CustomerInfo) {
        if (appConfig.customEntitlementComputation || appConfig.uiPreviewMode) return
        promotionalEntitlementExpirationScheduler.track(
            customerInfo,
            ::onPromotionalEntitlementExpirationReached,
        )
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
