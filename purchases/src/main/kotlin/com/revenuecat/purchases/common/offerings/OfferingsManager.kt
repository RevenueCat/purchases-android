package com.revenuecat.purchases.common.offerings

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.GetOfferingsErrorHandlingBehavior
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.utils.OfferingImagePreDownloader
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONObject
import java.util.Date
import kotlin.time.Duration

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class OfferingsManager(
    private val offeringsCache: OfferingsCache,
    private val backend: Backend,
    private val offeringsFactory: OfferingsFactory,
    private val offeringImagePreDownloader: OfferingImagePreDownloader,
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    private val offeringFontPreDownloader: OfferingFontPreDownloader,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) {
    fun getOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null,
        fetchCurrent: Boolean = false,
    ) {
        trackGetOfferingsStartedIfNeeded()
        val startTime = dateProvider.now
        val onErrorWithTracking: (PurchasesError, DiagnosticsTracker.CacheStatus) -> Unit = { error, cacheStatus ->
            trackGetOfferingsResultIfNeeded(startTime, cacheStatus, error, null, null)
            onError?.invoke(error)
        }
        val onSuccessWithTracking: (OfferingsResultData, DiagnosticsTracker.CacheStatus) -> Unit =
            { result, cacheStatus ->
                trackGetOfferingsResultIfNeeded(
                    startTime,
                    cacheStatus,
                    null,
                    result.requestedProductIds,
                    result.notFoundProductIds,
                )
                onSuccess?.invoke(result.offerings)
            }

        val cachedOfferings = offeringsCache.cachedOfferings
        if (fetchCurrent) {
            log(LogIntent.DEBUG) { OfferingStrings.FORCE_OFFERINGS_FETCHING_NETWORK }
            fetchAndCacheOfferings(
                appUserID,
                appInBackground,
                { onErrorWithTracking(it, DiagnosticsTracker.CacheStatus.NOT_CHECKED) },
                { onSuccessWithTracking(it, DiagnosticsTracker.CacheStatus.NOT_CHECKED) },
            )
        } else if (cachedOfferings == null) {
            log(LogIntent.DEBUG) { OfferingStrings.NO_CACHED_OFFERINGS_FETCHING_NETWORK }
            fetchAndCacheOfferings(
                appUserID,
                appInBackground,
                { onErrorWithTracking(it, DiagnosticsTracker.CacheStatus.NOT_FOUND) },
                { onSuccessWithTracking(it, DiagnosticsTracker.CacheStatus.NOT_FOUND) },
            )
        } else {
            log(LogIntent.DEBUG) { OfferingStrings.VENDING_OFFERINGS_CACHE }

            val isCacheStale = offeringsCache.isOfferingsCacheStale(appInBackground)
            trackGetOfferingsResultIfNeeded(
                startTime,
                if (isCacheStale) DiagnosticsTracker.CacheStatus.STALE else DiagnosticsTracker.CacheStatus.VALID,
                null,
                null,
                null,
            )
            dispatch { onSuccess?.invoke(cachedOfferings) }
            if (isCacheStale) {
                log(LogIntent.DEBUG) {
                    if (appInBackground) {
                        OfferingStrings.OFFERINGS_STALE_UPDATING_IN_BACKGROUND
                    } else {
                        OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND
                    }
                }
                fetchAndCacheOfferings(appUserID, appInBackground)
            }
        }
    }

    fun onAppForeground(appUserID: String) {
        if (offeringsCache.isOfferingsCacheStale(appInBackground = false)) {
            log(LogIntent.DEBUG) { OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND }
            fetchAndCacheOfferings(appUserID, appInBackground = false)
        }
    }

    fun fetchAndCacheOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((OfferingsResultData) -> Unit)? = null,
    ) {
        log(LogIntent.RC_SUCCESS) { OfferingStrings.OFFERINGS_START_UPDATE_FROM_NETWORK }
        backend.getOfferings(
            appUserID,
            appInBackground,
            { body, originalDataSource ->
                createAndCacheOfferings(
                    offeringsJSON = body,
                    originalDataSource = originalDataSource,
                    loadedFromDiskCache = false,
                    onError,
                    onSuccess,
                )
            },
            { backendError, errorBehavior ->
                when (errorBehavior) {
                    GetOfferingsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_OFFERINGS -> {
                        val cachedOfferingsResponse = offeringsCache.cachedOfferingsResponse
                        if (cachedOfferingsResponse == null) {
                            handleErrorFetchingOfferings(backendError, onError)
                        } else {
                            warnLog { OfferingStrings.ERROR_FETCHING_OFFERINGS_USING_DISK_CACHE }
                            val originalDataSource = cachedOfferingsResponse.optNullableString(
                                OfferingsCache.ORIGINAL_SOURCE_KEY,
                            )?.let {
                                try {
                                    HTTPResponseOriginalSource.valueOf(it)
                                } catch (e: IllegalArgumentException) {
                                    errorLog(e) { "Invalid original data source for cached offerings" }
                                    null
                                }
                            } ?: HTTPResponseOriginalSource.MAIN
                            createAndCacheOfferings(
                                offeringsJSON = cachedOfferingsResponse,
                                originalDataSource = originalDataSource,
                                loadedFromDiskCache = true,
                                onError,
                                onSuccess,
                            )
                        }
                    }
                    GetOfferingsErrorHandlingBehavior.SHOULD_NOT_FALLBACK -> {
                        handleErrorFetchingOfferings(backendError, onError)
                    }
                }
            },
        )
    }

    private fun createAndCacheOfferings(
        offeringsJSON: JSONObject,
        originalDataSource: HTTPResponseOriginalSource,
        loadedFromDiskCache: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((OfferingsResultData) -> Unit)? = null,
    ) {
        offeringsFactory.createOfferings(
            offeringsJSON,
            originalDataSource,
            loadedFromDiskCache,
            onError = { error ->
                handleErrorFetchingOfferings(error, onError)
            },
            onSuccess = { offeringsResultData ->
                offeringsResultData.offerings.current?.let {
                    offeringImagePreDownloader.preDownloadOfferingImages(it)
                }
                offeringFontPreDownloader.preDownloadOfferingFontsIfNeeded(offeringsResultData.offerings)
                offeringsCache.cacheOfferings(offeringsResultData.offerings, offeringsJSON)
                dispatch {
                    onSuccess?.invoke(offeringsResultData)
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

        log(if (errorCausedByPurchases) LogIntent.RC_ERROR else LogIntent.GOOGLE_ERROR) {
            OfferingStrings.FETCHING_OFFERINGS_ERROR.format(error)
        }

        offeringsCache.forceCacheStale()
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

    private fun trackGetOfferingsStartedIfNeeded() {
        diagnosticsTrackerIfEnabled?.trackGetOfferingsStarted()
    }

    private fun trackGetOfferingsResultIfNeeded(
        startTime: Date,
        cacheStatus: DiagnosticsTracker.CacheStatus,
        error: PurchasesError?,
        requestedProductIds: Set<String>?,
        notFoundProductIds: Set<String>?,
    ) {
        if (diagnosticsTrackerIfEnabled == null) return
        val responseTime = Duration.between(startTime, dateProvider.now)
        diagnosticsTrackerIfEnabled.trackGetOfferingsResult(
            requestedProductIds = requestedProductIds,
            notFoundProductIds = notFoundProductIds,
            errorMessage = error?.message,
            errorCode = error?.code?.code,
            // WIP Add verification result property once we expose verification result in Offerings object
            verificationResult = null,
            cacheStatus = cacheStatus,
            responseTime = responseTime,
        )
    }
}
