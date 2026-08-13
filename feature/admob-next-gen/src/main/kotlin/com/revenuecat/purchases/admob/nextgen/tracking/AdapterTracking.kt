@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdLoadedData

internal fun AdTracker.trackFromAdapter(data: AdLoadedData) =
    trackAdLoaded(data, AdCaptureMethod.ADAPTER)

internal fun AdTracker.trackFromAdapter(data: AdFailedToLoadData) =
    trackAdFailedToLoad(data, AdCaptureMethod.ADAPTER)
