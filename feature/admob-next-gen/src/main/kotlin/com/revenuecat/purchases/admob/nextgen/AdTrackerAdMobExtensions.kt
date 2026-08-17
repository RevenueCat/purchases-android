@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob.nextgen

import android.annotation.SuppressLint
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdLoadCallback
import com.revenuecat.purchases.admob.nextgen.tracking.trackAdFailedToLoad
import com.revenuecat.purchases.admob.nextgen.tracking.trackAdLoaded
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Loads an [InterstitialAd] and automatically tracks RevenueCat ad events.
 *
 * The loaded ad has event tracking installed before it is forwarded to [loadCallback].
 * Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Callback to receive load success and failure events.
 * @param adEventCallback Optional callback for interstitial lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<InterstitialAd>,
    adEventCallback: InterstitialAdEventCallback? = null,
) {
    val adUnitId = adRequest.adUnitId
    InterstitialAd.load(
        adRequest,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.INTERSTITIAL,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.installTrackingEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                )
            },
        ),
    )
}

/**
 * Loads an [InterstitialAd] using Google Mobile Ads' suspending API and automatically tracks RevenueCat ad events.
 *
 * The original [AdLoadResult] is returned unchanged. A successfully loaded ad has event tracking installed before
 * this function returns. Call via `Purchases.sharedInstance.adTracker`.
 *
 * @param adRequest The [AdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param adEventCallback Optional callback for interstitial lifecycle and paid events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackInterstitialAd(
    adRequest: AdRequest,
    placement: String? = null,
    adEventCallback: InterstitialAdEventCallback? = null,
): AdLoadResult<InterstitialAd> {
    val adUnitId = adRequest.adUnitId
    val result = InterstitialAd.load(adRequest)

    when (result) {
        is AdLoadResult.Success -> {
            trackAdLoaded(result.ad::getResponseInfo, AdFormat.INTERSTITIAL, placement, adUnitId)
            result.ad.installTrackingEventCallback(
                delegate = adEventCallback,
                placement = placement,
                adUnitId = adUnitId,
            )
        }
        is AdLoadResult.Failure -> {
            trackAdFailedToLoad(result.error, AdFormat.INTERSTITIAL, placement, adUnitId)
        }
    }

    return result
}
