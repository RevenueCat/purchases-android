package com.revenuecat.purchases.common.offerings

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.OfferingStrings

class OfferingsManager(
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val offeringsFactory: OfferingsFactory,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper())
) {
    fun getOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null
    ) {
        val cachedOfferings = deviceCache.cachedOfferings
        if (cachedOfferings == null) {
            log(LogIntent.DEBUG, OfferingStrings.NO_CACHED_OFFERINGS_FETCHING_NETWORK)
            fetchAndCacheOfferings(appUserID, appInBackground, onError, onSuccess)
        } else {
            log(LogIntent.DEBUG, OfferingStrings.VENDING_OFFERINGS_CACHE)
            dispatch {
                onSuccess?.invoke(cachedOfferings)
            }
            if (isOfferingsCacheStale(appInBackground)) {
                log(
                    LogIntent.DEBUG,
                    if (appInBackground) OfferingStrings.OFFERINGS_STALE_UPDATING_IN_BACKGROUND
                    else OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND
                )
                fetchAndCacheOfferings(appUserID, appInBackground)
                log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
            }
        }
    }

    fun onAppForeground(appUserID: String) {
        if (isOfferingsCacheStale(appInBackground = false)) {
            log(LogIntent.DEBUG, OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND)
            fetchAndCacheOfferings(appUserID, appInBackground = false)
            log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
        }
    }

    fun fetchAndCacheOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null
    ) {
        deviceCache.setOfferingsCacheTimestampToNow()
        backend.getOfferings(
            appUserID,
            appInBackground,
            { offeringsJSON ->
                offeringsFactory.createOfferings(
                    offeringsJSON,
                    onError = { error ->
                        handleErrorFetchingOfferings(error, onError)
                    },
                    onSuccess = { offerings ->
                        synchronized(this@OfferingsManager) {
                            deviceCache.cacheOfferings(offerings)
                        }
                        dispatch {
                            onSuccess?.invoke(offerings)
                        }
                    }
                )
            }, { error ->
                handleErrorFetchingOfferings(error, onError)
            })
    }

    private fun isOfferingsCacheStale(appInBackground: Boolean) = deviceCache.isOfferingsCacheStale(appInBackground)

    private fun handleErrorFetchingOfferings(
        error: PurchasesError,
        onError: ((PurchasesError) -> Unit)?
    ) {
        val errorCausedByPurchases = setOf(
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.UnexpectedBackendResponseError
        )
            .contains(error.code)

        log(
            if (errorCausedByPurchases) LogIntent.RC_ERROR else LogIntent.GOOGLE_ERROR,
            OfferingStrings.FETCHING_OFFERINGS_ERROR.format(error)
        )

        deviceCache.clearOfferingsCacheTimestamp()
        dispatch {
            onError?.invoke(error)
        }
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val handler = mainHandler ?: Handler(Looper.getMainLooper())
            handler.post(action)
        } else {
            action()
        }
    }
}
