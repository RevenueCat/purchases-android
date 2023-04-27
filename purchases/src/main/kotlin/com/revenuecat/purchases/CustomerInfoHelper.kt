package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.CustomerInfoStrings

@Suppress("TooManyFunctions")
internal class CustomerInfoHelper(
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val identityManager: IdentityManager,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

    var updatedCustomerInfoListener: UpdatedCustomerInfoListener? = null
        @Synchronized get
        set(value) {
            synchronized(this@CustomerInfoHelper) {
                field = value
            }
            afterSetListener(value)
        }

    private var lastSentCustomerInfo: CustomerInfo? = null

    fun retrieveCustomerInfo(
        appUserID: String,
        fetchPolicy: CacheFetchPolicy,
        appInBackground: Boolean,
        callback: ReceiveCustomerInfoCallback? = null
    ) {
        debugLog(CustomerInfoStrings.RETRIEVING_CUSTOMER_INFO.format(fetchPolicy))
        when (fetchPolicy) {
            CacheFetchPolicy.CACHE_ONLY -> getCustomerInfoCacheOnly(appUserID, callback)
            CacheFetchPolicy.FETCH_CURRENT -> getCustomerInfoFetchOnly(
                appUserID,
                appInBackground,
                callback
            )
            CacheFetchPolicy.CACHED_OR_FETCHED -> getCustomerInfoCachedOrFetched(
                appUserID,
                appInBackground,
                callback
            )
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT -> getCustomerInfoNotStaledCachedOrFetched(
                appUserID,
                appInBackground,
                callback
            )
        }
    }

    @Synchronized
    fun cacheCustomerInfo(info: CustomerInfo) {
        deviceCache.cacheCustomerInfo(identityManager.currentAppUserID, info)
    }

    fun sendUpdatedCustomerInfoToDelegateIfChanged(info: CustomerInfo) {
        synchronized(this@CustomerInfoHelper) { updatedCustomerInfoListener to lastSentCustomerInfo }
            .let { (listener, lastSentCustomerInfo) ->
                if (listener != null && lastSentCustomerInfo != info) {
                    if (lastSentCustomerInfo != null) {
                        log(LogIntent.DEBUG, CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER)
                    } else {
                        log(LogIntent.DEBUG, CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER)
                    }
                    synchronized(this@CustomerInfoHelper) {
                        this.lastSentCustomerInfo = info
                    }
                    dispatch { listener.onReceived(info) }
                }
            }
    }

    private fun afterSetListener(listener: UpdatedCustomerInfoListener?) {
        if (listener != null) {
            log(LogIntent.DEBUG, ConfigureStrings.LISTENER_SET)
            getCachedCustomerInfo(identityManager.currentAppUserID)?.let {
                sendUpdatedCustomerInfoToDelegateIfChanged(it)
            }
        }
    }

    private fun getCustomerInfoCacheOnly(
        appUserID: String,
        callback: ReceiveCustomerInfoCallback? = null
    ) {
        if (callback == null) return
        val cachedCustomerInfo = getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG, CustomerInfoStrings.VENDING_CACHE)
            dispatch { callback.onReceived(cachedCustomerInfo) }
        } else {
            val error = PurchasesError(
                PurchasesErrorCode.CustomerInfoError,
                CustomerInfoStrings.MISSING_CACHED_CUSTOMER_INFO
            )
            errorLog(error)
            dispatch { callback.onError(error) }
        }
    }

    private fun getCustomerInfoFetchOnly(
        appUserID: String,
        appInBackground: Boolean,
        callback: ReceiveCustomerInfoCallback? = null
    ) {
        deviceCache.setCustomerInfoCacheTimestampToNow(appUserID)
        backend.getCustomerInfo(
            appUserID,
            appInBackground,
            { info ->
                log(LogIntent.RC_SUCCESS, CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_NETWORK)
                offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                cacheCustomerInfo(info)
                sendUpdatedCustomerInfoToDelegateIfChanged(info)
                dispatch { callback?.onReceived(info) }
            },
            { backendError, isServerError ->
                errorLog(CustomerInfoStrings.ERROR_FETCHING_CUSTOMER_INFO.format(backendError))
                deviceCache.clearCustomerInfoCacheTimestamp(appUserID)
                if (offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
                        isServerError,
                        appUserID
                    )) {
                    offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
                        appUserID,
                        onSuccess = { offlineComputedCustomerInfo ->
                            sendUpdatedCustomerInfoToDelegateIfChanged(offlineComputedCustomerInfo)
                            dispatch { callback?.onReceived(offlineComputedCustomerInfo) }
                        },
                        onError = {
                            dispatch { callback?.onError(backendError) }
                        }
                    )
                } else {
                    dispatch { callback?.onError(backendError) }
                }
            })
    }

    private fun getCustomerInfoCachedOrFetched(
        appUserID: String,
        appInBackground: Boolean,
        callback: ReceiveCustomerInfoCallback? = null
    ) {
        val cachedCustomerInfo = getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG, CustomerInfoStrings.VENDING_CACHE)
            dispatch { callback?.onReceived(cachedCustomerInfo) }
            updateCachedCustomerInfoIfStale(appUserID, appInBackground)
        } else {
            log(LogIntent.DEBUG, CustomerInfoStrings.NO_CACHED_CUSTOMERINFO)
            getCustomerInfoFetchOnly(appUserID, appInBackground, callback)
        }
    }

    private fun getCustomerInfoNotStaledCachedOrFetched(
        appUserID: String,
        appInBackground: Boolean,
        callback: ReceiveCustomerInfoCallback? = null
    ) {
        if (deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground)) {
            getCustomerInfoFetchOnly(appUserID, appInBackground, callback)
        } else {
            getCustomerInfoCachedOrFetched(appUserID, appInBackground, callback)
        }
    }

    private fun getCachedCustomerInfo(appUserID: String): CustomerInfo? {
        return offlineEntitlementsManager.offlineCustomerInfo
            ?: deviceCache.getCachedCustomerInfo(appUserID)
    }

    private fun updateCachedCustomerInfoIfStale(
        appUserID: String,
        appInBackground: Boolean
    ) {
        if (deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground)) {
            log(
                LogIntent.DEBUG,
                if (appInBackground) CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_BACKGROUND
                else CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND)
            getCustomerInfoFetchOnly(appUserID, appInBackground)
        }
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != handler.looper.thread) {
            handler.post(action)
        } else {
            action()
        }
    }
}
