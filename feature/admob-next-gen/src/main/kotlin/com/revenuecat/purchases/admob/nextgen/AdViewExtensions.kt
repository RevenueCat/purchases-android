@file:JvmName("RCAdMobNextGenAdView")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob.nextgen

import android.annotation.SuppressLint
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAdLoadCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Loads a banner ad into this [AdView] and automatically tracks RevenueCat ad events.
 *
 * The loaded [BannerAd] has tracking callbacks installed before it is forwarded to [loadCallback]. Pass app
 * callbacks here instead of assigning them directly to the loaded ad, as direct assignment would replace
 * RevenueCat's tracking callbacks. To change them later, use [BannerAd.setTrackingAdEventCallback] and
 * [BannerAd.setTrackingBannerAdRefreshCallback].
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
@Suppress("LongParameterList")
public fun AdView.loadAndTrackAd(
    adRequest: BannerAdRequest,
    placement: String? = null,
    loadCallback: AdLoadCallback<BannerAd>? = null,
    adEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
) {
    loadAndTrackBannerAdInternal(
        adRequest = adRequest,
        placement = placement,
        loadCallback = loadCallback,
        adEventCallback = adEventCallback,
        bannerAdRefreshCallback = bannerAdRefreshCallback,
    )
}

internal fun AdView.loadAndTrackBannerAdInternal(
    adRequest: BannerAdRequest,
    placement: String?,
    loadCallback: AdLoadCallback<BannerAd>?,
    adEventCallback: BannerAdEventCallback?,
    bannerAdRefreshCallback: BannerAdRefreshCallback?,
) {
    val adUnitId = adRequest.adUnitId
    loadAd(
        adRequest,
        trackingLoadCallback(
            adUnitId = adUnitId,
            placement = placement,
            loadCallback = loadCallback,
            adEventCallback = adEventCallback,
            bannerAdRefreshCallback = bannerAdRefreshCallback,
        ),
    )
}

private fun trackingLoadCallback(
    adUnitId: String,
    placement: String?,
    loadCallback: AdLoadCallback<BannerAd>?,
    adEventCallback: BannerAdEventCallback?,
    bannerAdRefreshCallback: BannerAdRefreshCallback?,
): TrackingAdLoadCallback<BannerAd> = TrackingAdLoadCallback(
    delegate = loadCallback,
    adFormat = AdFormat.BANNER,
    placement = placement,
    adUnitId = adUnitId,
    configureAd = { ad ->
        ad.installTrackingCallbacks(
            adEventCallback = adEventCallback,
            bannerAdRefreshCallback = bannerAdRefreshCallback,
            placement = placement,
            adUnitId = adUnitId,
        )
    },
)
