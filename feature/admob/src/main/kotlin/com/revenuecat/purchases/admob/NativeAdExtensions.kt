@file:JvmName("RCAdMobNativeAd")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob

import android.annotation.SuppressLint
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.nativead.NativeAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName

/**
 * Configures RevenueCat ad-event tracking for native ads on this [AdLoader.Builder].
 *
 * This is a direct 1:1 replacement for AdMob's [AdLoader.Builder.forNativeAd]:
 * swap `forNativeAd` for `forNativeAdWithTracking` and the rest of the builder
 * chain stays unchanged.
 *
 * Tracks loaded, displayed, opened, revenue, and failed-to-load events automatically.
 * The [onNativeAdLoaded] lambda delivers the [NativeAd] instance — identical to
 * the callback in [AdLoader.Builder.forNativeAd].
 *
 * @param adUnitId The AdMob ad unit ID, used for RevenueCat event tracking.
 * @param placement A placement identifier for RevenueCat tracking.
 * @param adListener Optional [AdListener] to receive ad lifecycle events.
 *   RevenueCat tracking for impression, click, and failed-to-load is injected
 *   transparently before each delegate call.
 * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
 *   RevenueCat revenue tracking is called first, then forwarded to this listener.
 * @param onNativeAdLoaded Called with the loaded [NativeAd] (already tracked).
 * @return This [AdLoader.Builder] for chaining.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdLoader.Builder.forNativeAdWithTracking(
    adUnitId: String,
    placement: String? = null,
    adListener: AdListener? = null,
    onPaidEventListener: OnPaidEventListener? = null,
    onNativeAdLoaded: (NativeAd) -> Unit = {},
): AdLoader.Builder {
    var loadedNativeAd: NativeAd? = null
    val responseInfoProvider: () -> ResponseInfo? = { loadedNativeAd?.responseInfo }

    this.forNativeAd { nativeAd ->
        loadedNativeAd = nativeAd

        trackIfConfigured {
            adTracker.trackAdLoaded(
                AdLoadedData(
                    networkName = nativeAd.responseInfo?.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = AdFormat.NATIVE,
                    placement = placement,
                    adUnitId = adUnitId,
                    impressionId = nativeAd.responseInfo?.responseId.orEmpty(),
                ),
            )
        }

        nativeAd.setOnPaidEventListener(
            TrackingOnPaidEventListener(
                delegate = onPaidEventListener,
                adFormat = AdFormat.NATIVE,
                placement = placement,
                adUnitId = adUnitId,
                responseInfoProvider = responseInfoProvider,
            ),
        )

        onNativeAdLoaded(nativeAd)
    }

    this.withAdListener(
        TrackingAdListener(
            delegate = adListener,
            adFormat = AdFormat.NATIVE,
            placement = placement,
            adUnitId = adUnitId,
            responseInfoProvider = responseInfoProvider,
            trackAdLoaded = false,
        ),
    )

    return this
}
