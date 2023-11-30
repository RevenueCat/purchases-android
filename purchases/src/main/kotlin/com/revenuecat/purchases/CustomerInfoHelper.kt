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
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.strings.CustomerInfoStrings

internal class CustomerInfoHelper(
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    private val postPendingTransactionsHelper: PostPendingTransactionsHelper,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    fun retrieveCustomerInfo(
        appUserID: String,
        fetchPolicy: CacheFetchPolicy,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        debugLog(CustomerInfoStrings.RETRIEVING_CUSTOMER_INFO.format(fetchPolicy))
        when (fetchPolicy) {
            CacheFetchPolicy.CACHE_ONLY -> getCustomerInfoCacheOnly(appUserID, callback)
            CacheFetchPolicy.FETCH_CURRENT -> postPendingPurchasesAndFetchCustomerInfo(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callback,
            )
            CacheFetchPolicy.CACHED_OR_FETCHED -> getCustomerInfoCachedOrFetched(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callback,
            )
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT -> getCustomerInfoNotStaledCachedOrFetched(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callback,
            )
        }
    }

    private fun getCustomerInfoCacheOnly(
        appUserID: String,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        if (callback == null) return
        val cachedCustomerInfo = getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG, CustomerInfoStrings.VENDING_CACHE)
            dispatch { callback.onReceived(cachedCustomerInfo) }
        } else {
            val error = PurchasesError(
                PurchasesErrorCode.CustomerInfoError,
                CustomerInfoStrings.MISSING_CACHED_CUSTOMER_INFO,
            )
            errorLog(error)
            dispatch { callback.onError(error) }
        }
    }

    private fun postPendingPurchasesAndFetchCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        postPendingTransactionsHelper.syncPendingPurchaseQueue(
            allowSharingPlayStoreAccount,
            onError = { getCustomerInfoFetchOnly(appUserID, appInBackground, callback) },
            onSuccess = { customerInfo ->
                if (customerInfo == null) {
                    getCustomerInfoFetchOnly(appUserID, appInBackground, callback)
                } else {
                    log(LogIntent.RC_SUCCESS, CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_SYNCING_PENDING_PURCHASES)
                    dispatch { callback?.onReceived(customerInfo) }
                }
            },
        )
    }

    private fun getCustomerInfoFetchOnly(
        appUserID: String,
        appInBackground: Boolean,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        deviceCache.setCustomerInfoCacheTimestampToNow(appUserID)
        backend.getCustomerInfo(
            appUserID,
            appInBackground,
            { info ->
                log(LogIntent.RC_SUCCESS, CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_NETWORK)
                offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                customerInfoUpdateHandler.cacheAndNotifyListeners(info)
                dispatch { callback?.onReceived(info) }
            },
            { backendError, isServerError ->
                errorLog(CustomerInfoStrings.ERROR_FETCHING_CUSTOMER_INFO.format(backendError))
                deviceCache.clearCustomerInfoCacheTimestamp(appUserID)
                if (offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
                        isServerError,
                        appUserID,
                    )
                ) {
                    offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
                        appUserID,
                        onSuccess = { offlineComputedCustomerInfo ->
                            customerInfoUpdateHandler.notifyListeners(offlineComputedCustomerInfo)
                            dispatch { callback?.onReceived(offlineComputedCustomerInfo) }
                        },
                        onError = {
                            dispatch { callback?.onError(backendError) }
                        },
                    )
                } else {
                    dispatch { callback?.onError(backendError) }
                }
            },
        )
    }

    private fun getCustomerInfoCachedOrFetched(
        appUserID: String,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        val cachedCustomerInfo = getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG, CustomerInfoStrings.VENDING_CACHE)
            dispatch { callback?.onReceived(cachedCustomerInfo) }
            updateCachedCustomerInfoIfStale(appUserID, appInBackground, allowSharingPlayStoreAccount)
        } else {
            log(LogIntent.DEBUG, CustomerInfoStrings.NO_CACHED_CUSTOMERINFO)
            postPendingPurchasesAndFetchCustomerInfo(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callback,
            )
        }
    }

    private fun getCustomerInfoNotStaledCachedOrFetched(
        appUserID: String,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        if (deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground)) {
            postPendingPurchasesAndFetchCustomerInfo(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callback,
            )
        } else {
            getCustomerInfoCachedOrFetched(appUserID, appInBackground, allowSharingPlayStoreAccount, callback)
        }
    }

    private fun getCachedCustomerInfo(appUserID: String): CustomerInfo? {
        return offlineEntitlementsManager.offlineCustomerInfo
            ?: deviceCache.getCachedCustomerInfo(appUserID)
    }

    private fun updateCachedCustomerInfoIfStale(
        appUserID: String,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
    ) {
        if (deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground)) {
            log(
                LogIntent.DEBUG,
                if (appInBackground) {
                    CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_BACKGROUND
                } else CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND,
            )
            postPendingPurchasesAndFetchCustomerInfo(appUserID, appInBackground, allowSharingPlayStoreAccount)
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
