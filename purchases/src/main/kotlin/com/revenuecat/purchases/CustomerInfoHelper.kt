package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.strings.CustomerInfoStrings
import java.util.Date
import kotlin.time.Duration

/**
 * Wrapper for [ReceiveCustomerInfoCallback] to hold extra information for diagnostics.
 */
internal class ReceiveCustomerInfoFullCallback(
    private val onCustomerInfoReceived: (CustomerInfo, Boolean?) -> Unit,
    private val onError: (PurchasesError, Boolean?) -> Unit,
) : ReceiveCustomerInfoCallback {
    var hadUnsyncedPurchases: Boolean? = null

    override fun onReceived(customerInfo: CustomerInfo) {
        onCustomerInfoReceived(customerInfo, hadUnsyncedPurchases)
    }

    override fun onError(error: PurchasesError) {
        onError(error, hadUnsyncedPurchases)
    }
}

internal class CustomerInfoHelper(
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    private val postPendingTransactionsHelper: PostPendingTransactionsHelper,
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {

    fun retrieveCustomerInfo(
        appUserID: String,
        fetchPolicy: CacheFetchPolicy,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        trackDiagnostics: Boolean = false,
        callback: ReceiveCustomerInfoCallback? = null,
    ) {
        debugLog(CustomerInfoStrings.RETRIEVING_CUSTOMER_INFO.format(fetchPolicy))
        trackGetCustomerInfoStartedIfNeeded(trackDiagnostics)
        val startTime = dateProvider.now

        val callbackWithDiagnostics = ReceiveCustomerInfoFullCallback(
            onCustomerInfoReceived = { customerInfo, hadUnsyncedPurchases ->
                trackGetCustomerInfoResultIfNeeded(
                    trackDiagnostics,
                    startTime,
                    customerInfo.entitlements.verification,
                    fetchPolicy,
                    hadUnsyncedPurchases,
                    null,
                )
                callback?.onReceived(customerInfo)
            },
            onError = { error, hadUnsyncedPurchases ->
                trackGetCustomerInfoResultIfNeeded(
                    trackDiagnostics,
                    startTime,
                    null,
                    fetchPolicy,
                    hadUnsyncedPurchases,
                    error,
                )
                callback?.onError(error)
            }
        )

        when (fetchPolicy) {
            CacheFetchPolicy.CACHE_ONLY -> getCustomerInfoCacheOnly(appUserID, callbackWithDiagnostics)
            CacheFetchPolicy.FETCH_CURRENT -> postPendingPurchasesAndFetchCustomerInfo(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callbackWithDiagnostics,
            )
            CacheFetchPolicy.CACHED_OR_FETCHED -> getCustomerInfoCachedOrFetched(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callbackWithDiagnostics,
            )
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT -> getCustomerInfoNotStaledCachedOrFetched(
                appUserID,
                appInBackground,
                allowSharingPlayStoreAccount,
                callbackWithDiagnostics,
            )
        }
    }

    private fun getCustomerInfoCacheOnly(
        appUserID: String,
        callback: ReceiveCustomerInfoCallback,
    ) {
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
        callback: ReceiveCustomerInfoFullCallback? = null,
    ) {
        postPendingTransactionsHelper.syncPendingPurchaseQueue(
            allowSharingPlayStoreAccount,
            onError = { _, hadUnsyncedPurchases ->
                callback?.hadUnsyncedPurchases = hadUnsyncedPurchases
                getCustomerInfoFetchOnly(appUserID, appInBackground, callback)
            },
            onSuccess = { customerInfo, hadUnsyncedPurchases ->
                callback?.hadUnsyncedPurchases = hadUnsyncedPurchases
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
        callback: ReceiveCustomerInfoFullCallback? = null,
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
        callback: ReceiveCustomerInfoFullCallback? = null,
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
                } else {
                    CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND
                },
            )
            postPendingPurchasesAndFetchCustomerInfo(appUserID, appInBackground, allowSharingPlayStoreAccount)
        }
    }

    private fun trackGetCustomerInfoStartedIfNeeded(trackDiagnostics: Boolean) {
        if (!trackDiagnostics || diagnosticsTrackerIfEnabled == null) return
        diagnosticsTrackerIfEnabled.trackGetCustomerInfoStarted()
    }

    private fun trackGetCustomerInfoResultIfNeeded(
        trackDiagnostics: Boolean,
        startTime: Date,
        verificationResult: VerificationResult?,
        cacheFetchPolicy: CacheFetchPolicy,
        hadUnsyncedPurchasesBefore: Boolean?,
        error: PurchasesError?,
    ) {
        if (!trackDiagnostics || diagnosticsTrackerIfEnabled == null) return
        val responseTime = Duration.between(startTime, dateProvider.now)
        diagnosticsTrackerIfEnabled.trackGetCustomerInfoResult(
            cacheFetchPolicy,
            verificationResult,
            hadUnsyncedPurchasesBefore,
            errorMessage = error?.message,
            errorCode = error?.code?.code,
            responseTime = responseTime,
        )
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != handler.looper.thread) {
            handler.post(action)
        } else {
            action()
        }
    }
}
