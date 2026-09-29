@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData

/**
 * Adapter-internal wrappers around the public [AdTracker] `trackAd*` API that stamp
 * [AdCaptureMethod.ANDROID_ADMOB_NEXT_GEN_ADAPTER], so events auto-captured by the Next-Gen
 * AdMob adapter are distinguishable from developer-invoked (`manual`) events.
 */
internal fun AdTracker.trackFromAdapter(data: AdLoadedData) =
    trackAdLoaded(data, AdCaptureMethod.ANDROID_ADMOB_NEXT_GEN_ADAPTER)

internal fun AdTracker.trackFromAdapter(data: AdDisplayedData) =
    trackAdDisplayed(data, AdCaptureMethod.ANDROID_ADMOB_NEXT_GEN_ADAPTER)

internal fun AdTracker.trackFromAdapter(data: AdOpenedData) =
    trackAdOpened(data, AdCaptureMethod.ANDROID_ADMOB_NEXT_GEN_ADAPTER)

internal fun AdTracker.trackFromAdapter(data: AdRevenueData) =
    trackAdRevenue(data, AdCaptureMethod.ANDROID_ADMOB_NEXT_GEN_ADAPTER)

internal fun AdTracker.trackFromAdapter(data: AdFailedToLoadData) =
    trackAdFailedToLoad(data, AdCaptureMethod.ANDROID_ADMOB_NEXT_GEN_ADAPTER)
