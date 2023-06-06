package com.revenuecat.purchases.common.offerings

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONObject

class OfferingsManager(
    private val offeringsCache: OfferingsCache,
    private val backend: Backend,
    private val offeringsFactory: OfferingsFactory,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) {
    fun getOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null,
    ) {
        val cachedOfferings = offeringsCache.cachedOfferings
        if (cachedOfferings == null) {
            log(LogIntent.DEBUG, OfferingStrings.NO_CACHED_OFFERINGS_FETCHING_NETWORK)
            fetchAndCacheOfferings(appUserID, appInBackground, onError, onSuccess)
        } else {
            log(LogIntent.DEBUG, OfferingStrings.VENDING_OFFERINGS_CACHE)
            dispatch {
                onSuccess?.invoke(cachedOfferings)
            }
            if (offeringsCache.isOfferingsCacheStale(appInBackground)) {
                log(
                    LogIntent.DEBUG,
                    if (appInBackground) {
                        OfferingStrings.OFFERINGS_STALE_UPDATING_IN_BACKGROUND
                    } else OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND,
                )
                fetchAndCacheOfferings(appUserID, appInBackground)
                log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
            }
        }
    }

    fun onAppForeground(appUserID: String) {
        if (offeringsCache.isOfferingsCacheStale(appInBackground = false)) {
            log(LogIntent.DEBUG, OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND)
            fetchAndCacheOfferings(appUserID, appInBackground = false)
            log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
        }
    }

    fun fetchAndCacheOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null,
    ) {
        offeringsCache.setOfferingsCacheTimestampToNow()
        backend.getOfferings(
            appUserID,
            appInBackground,
            { createAndCacheOfferings(it, onError, onSuccess) },
            { backendError, isServerError ->
                if (isServerError) {
                    val cachedOfferingsResponse = offeringsCache.cachedOfferingsResponse
                    if (cachedOfferingsResponse == null) {
                        handleErrorFetchingOfferings(backendError, onError)
                    } else {
                        warnLog(OfferingStrings.ERROR_FETCHING_OFFERINGS_USING_DISK_CACHE)
                        createAndCacheOfferings(cachedOfferingsResponse, onError, onSuccess)
                    }
                } else {
                    handleErrorFetchingOfferings(backendError, onError)
                }
            },
        )
    }

    private fun createAndCacheOfferings(
        offeringsJSON: JSONObject,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null,
    ) {
        offeringsFactory.createOfferings(
            offeringsJSON,
            onError = { error ->
                handleErrorFetchingOfferings(error, onError)
            },
            onSuccess = { offerings ->
                offeringsCache.cacheOfferings(offerings, offeringsJSON)
                dispatch {
                    onSuccess?.invoke(offerings)
                }
            },
        )
    }

    private fun handleErrorFetchingOfferings(
        error: PurchasesError,
        onError: ((PurchasesError) -> Unit)?,
    ) {
        val errorCausedByPurchases = setOf(
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.UnexpectedBackendResponseError,
        )
            .contains(error.code)

        log(
            if (errorCausedByPurchases) LogIntent.RC_ERROR else LogIntent.GOOGLE_ERROR,
            OfferingStrings.FETCHING_OFFERINGS_ERROR.format(error),
        )

        offeringsCache.clearOfferingsCacheTimestamp()
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
