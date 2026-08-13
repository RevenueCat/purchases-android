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
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Loads a banner ad into this [AdView] and automatically tracks RevenueCat ad events.
 *
 * The loaded [BannerAd] has tracking callbacks installed before it is forwarded to
 * [loadCallback]. Pass app callbacks here instead of assigning them directly to the loaded ad,
 * as direct assignment would replace RevenueCat's tracking callbacks. To change them later, use
 * [BannerAd.setTrackingAdEventCallback] and [BannerAd.setTrackingBannerAdRefreshCallback].
 *
 * @param adRequest The [BannerAdRequest] to load. Its ad unit ID is used for tracking.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param loadCallback Optional callback to receive load success and failure events.
 * @param adEventCallback Optional callback for banner lifecycle and paid events.
 * @param bannerAdRefreshCallback Optional callback for automatic banner refresh events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
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
    placement: String? = null,
    loadCallback: AdLoadCallback<BannerAd>? = null,
    adEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
) {
    val adUnitId = adRequest.adUnitId
    loadAd(
        adRequest,
        TrackingAdLoadCallback(
            delegate = loadCallback,
            adFormat = AdFormat.BANNER,
            placement = placement,
            adUnitId = adUnitId,
            configureAd = { ad ->
                ad.adEventCallback = TrackingBannerAdEventCallback(
                    delegate = adEventCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = ad::getResponseInfo,
                )
                ad.bannerAdRefreshCallback = TrackingBannerAdRefreshCallback(
                    delegate = bannerAdRefreshCallback,
                    placement = placement,
                    adUnitId = adUnitId,
                    responseInfoProvider = ad::getResponseInfo,
                )
            },
        ),
    )
}
