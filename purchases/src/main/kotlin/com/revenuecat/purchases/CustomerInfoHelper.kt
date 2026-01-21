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
import com.revenuecat.purchases.utils.Result
import java.util.Date
import kotlin.time.Duration

/**
 * Wrapper around Result<CustomerInfo, BackendError> to hold some additional information useful for diagnostics
 * @property result The result of the of CustomerInfo query
 * @property hadUnsyncedPurchasesBefore Whether or not there were purchases to sync. A null value means that this
 * wasn't checked (e.g. due to a failure to query the purchases or because autoSyncPurchases being disabled)
 */
private data class CustomerInfoDataResult(
    val result: Result<CustomerInfo, PurchasesError>,
    val hadUnsyncedPurchasesBefore: Boolean? = null,
)

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList", "TooManyFunctions")
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
        debugLog { CustomerInfoStrings.RETRIEVING_CUSTOMER_INFO.format(fetchPolicy) }
        trackGetCustomerInfoStartedIfNeeded(trackDiagnostics)
        val startTime = dateProvider.now

        val callbackWithDiagnostics: ((CustomerInfoDataResult) -> Unit)? = if (callback != null || trackDiagnostics) {
            {
                    customerInfoDataResult ->
                trackGetCustomerInfoResultIfNeeded(trackDiagnostics, startTime, customerInfoDataResult, fetchPolicy)

                callback?.let {
                    when (customerInfoDataResult.result) {
                        is Result.Success -> {
                            it.onReceived(customerInfoDataResult.result.value)
                        }
                        is Result.Error -> {
                            it.onError(customerInfoDataResult.result.value)
                        }
                    }
                }
            }
        } else {
            null
        }

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
        callback: ((CustomerInfoDataResult) -> Unit)?,
    ) {
        if (callback == null) return
        val cachedCustomerInfo = getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG) { CustomerInfoStrings.VENDING_CACHE }
            dispatch { callback(CustomerInfoDataResult(Result.Success(cachedCustomerInfo))) }
        } else {
            val error = PurchasesError(
                PurchasesErrorCode.CustomerInfoError,
                CustomerInfoStrings.MISSING_CACHED_CUSTOMER_INFO,
            )
            errorLog(error)
            dispatch { callback(CustomerInfoDataResult(Result.Error(error))) }
        }
    }

    private fun postPendingPurchasesAndFetchCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        callback: ((CustomerInfoDataResult) -> Unit)? = null,
    ) {
        postPendingTransactionsHelper.syncPendingPurchaseQueue(
            allowSharingPlayStoreAccount,
            { syncResult ->
                when (syncResult) {
                    is SyncPendingPurchaseResult.Success -> {
                        log(LogIntent.RC_SUCCESS) {
                            CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_SYNCING_PENDING_PURCHASES
                        }
                        dispatch {
                            callback?.invoke(
                                CustomerInfoDataResult(
                                    Result.Success(syncResult.customerInfo),
                                    hadUnsyncedPurchasesBefore = true,
                                ),
                            )
                        }
                    }
                    is SyncPendingPurchaseResult.Error -> {
                        getCustomerInfoFetchOnly(
                            appUserID,
                            appInBackground,
                            { result ->
                                callback?.invoke(CustomerInfoDataResult(result, hadUnsyncedPurchasesBefore = true))
                            },
                        )
                    }
                    is SyncPendingPurchaseResult.AutoSyncDisabled -> {
                        getCustomerInfoFetchOnly(
                            appUserID,
                            appInBackground,
                            { result ->
                                callback?.invoke(CustomerInfoDataResult(result))
                            },
                        )
                    }
                    is SyncPendingPurchaseResult.NoPendingPurchasesToSync -> {
                        getCustomerInfoFetchOnly(
                            appUserID,
                            appInBackground,
                            { result ->
                                callback?.invoke(CustomerInfoDataResult(result, hadUnsyncedPurchasesBefore = false))
                            },
                        )
                    }
                }
            },
        )
    }

    private fun getCustomerInfoFetchOnly(
        appUserID: String,
        appInBackground: Boolean,
        callback: ((Result<CustomerInfo, PurchasesError>) -> Unit)? = null,
    ) {
        deviceCache.setCustomerInfoCacheTimestampToNow(appUserID)
        backend.getCustomerInfo(
            appUserID,
            appInBackground,
            { info ->
                log(LogIntent.RC_SUCCESS) { CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_NETWORK }
                offlineEntitlementsManager.resetOfflineCustomerInfoCache()
                customerInfoUpdateHandler.cacheAndNotifyListeners(info)
                dispatch { callback?.invoke(Result.Success(info)) }
            },
            { backendError, isServerError ->
                errorLog { CustomerInfoStrings.ERROR_FETCHING_CUSTOMER_INFO.format(backendError) }
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
                            dispatch { callback?.invoke(Result.Success(offlineComputedCustomerInfo)) }
                        },
                        onError = {
                            dispatch { callback?.invoke(Result.Error(backendError)) }
                        },
                    )
                } else {
                    dispatch { callback?.invoke(Result.Error(backendError)) }
                }
            },
        )
    }

    private fun getCustomerInfoCachedOrFetched(
        appUserID: String,
        appInBackground: Boolean,
        allowSharingPlayStoreAccount: Boolean,
        callback: ((CustomerInfoDataResult) -> Unit)? = null,
    ) {
        val cachedCustomerInfo = getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG) { CustomerInfoStrings.VENDING_CACHE }
            dispatch { callback?.invoke(CustomerInfoDataResult(Result.Success(cachedCustomerInfo))) }
            updateCachedCustomerInfoIfStale(appUserID, appInBackground, allowSharingPlayStoreAccount)
        } else {
            log(LogIntent.DEBUG) { CustomerInfoStrings.NO_CACHED_CUSTOMERINFO }
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
        callback: ((CustomerInfoDataResult) -> Unit)? = null,
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
            log(LogIntent.DEBUG) {
                if (appInBackground) {
                    CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_BACKGROUND
                } else {
                    CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND
                }
            }
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
        customerInfoDataResult: CustomerInfoDataResult,
        cacheFetchPolicy: CacheFetchPolicy,
    ) {
        if (!trackDiagnostics || diagnosticsTrackerIfEnabled == null) return
        val responseTime = Duration.between(startTime, dateProvider.now)

        val customerInfo: CustomerInfo? = when (customerInfoDataResult.result) {
            is Result.Success -> customerInfoDataResult.result.value
            is Result.Error -> null
        }

        val error: PurchasesError? = when (customerInfoDataResult.result) {
            is Result.Success -> null
            is Result.Error -> customerInfoDataResult.result.value
        }

        diagnosticsTrackerIfEnabled.trackGetCustomerInfoResult(
            cacheFetchPolicy,
            customerInfo?.entitlements?.verification,
            customerInfoDataResult.hadUnsyncedPurchasesBefore,
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
