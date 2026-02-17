@file:JvmName("RCAdMobBannerAd")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:SuppressLint("MissingPermission")

package com.revenuecat.purchases.admob

import android.annotation.SuppressLint
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.OnPaidEventListener
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Sets up RevenueCat ad-event tracking for this [AdView] and loads the ad.
 *
 * This works for both programmatically-created and **XML-declared** `AdView`s.
 * When the `AdView` is declared in XML with `app:adUnitId` and `app:adSize`,
 * those attributes are read automatically — just call this method after inflation.
 *
 * Wraps the [adListener] (or the one already set on this [AdView]) with a
 * [TrackingAdListener] that automatically tracks loaded, displayed, opened,
 * and failed-to-load events. Revenue is tracked via [OnPaidEventListener].
 *
 * If an [AdListener] or [OnPaidEventListener] is already set on this [AdView]
 * before calling this method, it will be preserved and called after the
 * RevenueCat tracking calls. An explicit [adListener] parameter takes
 * precedence over a previously set listener.
 *
 * **Important:** Do not reassign [AdView.adListener] or
 * [AdView.onPaidEventListener] after calling this method, as doing so will
 * replace the tracking wrappers and break RevenueCat event tracking.
 *
 * Alternatively use [AdTracker.loadAndTrackBannerAd] for a call that takes the tracker explicitly.
 *
 * @param adRequest The [AdRequest] to use.
 * @param placement A placement identifier (e.g., "home_screen_banner").
 * @param adListener Optional [AdListener] to receive ad lifecycle events.
 *   If `null` and an [AdListener] is already set on this [AdView], the
 *   existing listener will be used as the delegate. RevenueCat tracking is
 *   injected transparently before each delegate call.
 * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
 *   RevenueCat revenue tracking is called first, then forwarded to this listener.
 *   If `null` and an [OnPaidEventListener] is already set on this [AdView], the
 *   existing listener will be used as the delegate.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdView.loadAndTrackAd(
    adRequest: AdRequest,
    placement: String? = null,
    adListener: AdListener? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    loadAndTrackBannerAdInternal(
        adView = this,
        placement = placement,
        adRequest = adRequest,
        adListener = adListener,
        onPaidEventListener = onPaidEventListener,
    )
}

internal fun loadAndTrackBannerAdInternal(
    adView: AdView,
    adRequest: AdRequest,
    placement: String? = null,
    adListener: AdListener? = null,
    onPaidEventListener: OnPaidEventListener? = null,
) {
    val adUnitId = adView.adUnitId.orEmpty()

    // Capture any pre-existing listeners so we can chain them.
    // If the listeners are already our tracking wrappers (from a previous call),
    // unwrap them to get the original user listener, preventing double-tracking.
    val existingAdListener: AdListener? = adView.adListener
    val unwrappedAdListener = if (existingAdListener is TrackingAdListener) {
        existingAdListener.delegate
    } else {
        existingAdListener
    }
    val effectiveAdListener = adListener ?: unwrappedAdListener

    val existingPaidEventListener = adView.onPaidEventListener
    val unwrappedPaidListener = if (existingPaidEventListener is TrackingOnPaidEventListener) {
        existingPaidEventListener.delegate
    } else {
        existingPaidEventListener
    }
    // Explicit parameter takes precedence over pre-existing listener
    val effectivePaidListener = onPaidEventListener ?: unwrappedPaidListener

    adView.onPaidEventListener = TrackingOnPaidEventListener(
        delegate = effectivePaidListener,
        adFormat = AdFormat.BANNER,
        placement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = { adView.responseInfo },
    )

    adView.adListener = TrackingAdListener(
        delegate = effectiveAdListener,
        adFormat = AdFormat.BANNER,
        placement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = { adView.responseInfo },
    )

    adView.loadAd(adRequest)
}
