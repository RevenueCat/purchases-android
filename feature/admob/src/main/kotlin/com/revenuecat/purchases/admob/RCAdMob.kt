@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Main entry point for loading AdMob ads with automatic RevenueCat ad-event tracking.
 *
 * All methods are static and annotated with [@JvmStatic] and [@JvmOverloads] so they can be
 * called naturally from both Kotlin and Java:
 *
 * ```kotlin
 * // Kotlin
 * RCAdMob.loadAndTrackInterstitialAd(context, adUnitId, adRequest, placement = "home")
 * ```
 *
 * ```java
 * // Java
 * RCAdMob.loadAndTrackInterstitialAd(context, adUnitId, adRequest);
 * RCAdMob.loadAndTrackInterstitialAd(context, adUnitId, adRequest, "home");
 * ```
 *
 * For Kotlin banner ads, an extension function [AdView.loadAndTrackAd] is also available
 * for more idiomatic syntax.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
object RCAdMob {

    /**
     * Loads an [InterstitialAd] and automatically tracks RevenueCat ad events.
     *
     * On success the ad is delivered via [loadCallback] with full-screen content
     * tracking already wired. Revenue tracking is set up via `OnPaidEventListener`.
     *
     * @param context The context.
     * @param adUnitId The AdMob ad unit ID.
     * @param adRequest The [AdRequest] to use.
     * @param placement A placement identifier for RevenueCat tracking.
     * @param loadCallback Optional [InterstitialAdLoadCallback] to receive load results.
     * @param fullScreenContentCallback Optional [FullScreenContentCallback] to receive
     *   full-screen content events. RevenueCat tracking is injected transparently.
     * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
     *   RevenueCat revenue tracking is called first, then forwarded to this listener.
     */
    @JvmStatic
    @JvmOverloads
    fun loadAndTrackInterstitialAd(
        context: Context,
        adUnitId: String,
        adRequest: AdRequest,
        placement: String? = null,
        loadCallback: InterstitialAdLoadCallback? = null,
        fullScreenContentCallback: FullScreenContentCallback? = null,
        onPaidEventListener: OnPaidEventListener? = null,
    ) {
        loadAndTrackInterstitialAdInternal(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = placement,
            loadCallback = loadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
    }

    /**
     * Loads an [AppOpenAd] and automatically tracks RevenueCat ad events.
     *
     * @param context The context.
     * @param adUnitId The AdMob ad unit ID.
     * @param adRequest The [AdRequest] to use.
     * @param placement A placement identifier for RevenueCat tracking.
     * @param loadCallback Optional [AppOpenAd.AppOpenAdLoadCallback] to receive load results.
     * @param fullScreenContentCallback Optional [FullScreenContentCallback] to receive
     *   full-screen content events. RevenueCat tracking is injected transparently.
     * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
     *   RevenueCat revenue tracking is called first, then forwarded to this listener.
     */
    @JvmStatic
    @JvmOverloads
    fun loadAndTrackAppOpenAd(
        context: Context,
        adUnitId: String,
        adRequest: AdRequest,
        placement: String? = null,
        loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null,
        fullScreenContentCallback: FullScreenContentCallback? = null,
        onPaidEventListener: OnPaidEventListener? = null,
    ) {
        loadAndTrackAppOpenAdInternal(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = placement,
            loadCallback = loadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
    }

    /**
     * Loads a [RewardedAd] and automatically tracks RevenueCat ad events.
     *
     * @param context The context.
     * @param adUnitId The AdMob ad unit ID.
     * @param adRequest The [AdRequest] to use.
     * @param placement A placement identifier for RevenueCat tracking.
     * @param loadCallback Optional [RewardedAdLoadCallback] to receive load results.
     * @param fullScreenContentCallback Optional [FullScreenContentCallback] to receive
     *   full-screen content events. RevenueCat tracking is injected transparently.
     * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
     *   RevenueCat revenue tracking is called first, then forwarded to this listener.
     */
    @JvmStatic
    @JvmOverloads
    fun loadAndTrackRewardedAd(
        context: Context,
        adUnitId: String,
        adRequest: AdRequest,
        placement: String? = null,
        loadCallback: RewardedAdLoadCallback? = null,
        fullScreenContentCallback: FullScreenContentCallback? = null,
        onPaidEventListener: OnPaidEventListener? = null,
    ) {
        loadAndTrackRewardedAdInternal(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = placement,
            loadCallback = loadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
    }

    /**
     * Loads a [RewardedInterstitialAd] and automatically tracks RevenueCat ad events.
     *
     * @param context The context.
     * @param adUnitId The AdMob ad unit ID.
     * @param adRequest The [AdRequest] to use.
     * @param placement A placement identifier for RevenueCat tracking.
     * @param loadCallback Optional [RewardedInterstitialAdLoadCallback] to receive load results.
     * @param fullScreenContentCallback Optional [FullScreenContentCallback] to receive
     *   full-screen content events. RevenueCat tracking is injected transparently.
     * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
     *   RevenueCat revenue tracking is called first, then forwarded to this listener.
     */
    @JvmStatic
    @JvmOverloads
    fun loadAndTrackRewardedInterstitialAd(
        context: Context,
        adUnitId: String,
        adRequest: AdRequest,
        placement: String? = null,
        loadCallback: RewardedInterstitialAdLoadCallback? = null,
        fullScreenContentCallback: FullScreenContentCallback? = null,
        onPaidEventListener: OnPaidEventListener? = null,
    ) {
        loadAndTrackRewardedInterstitialAdInternal(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = placement,
            loadCallback = loadCallback,
            fullScreenContentCallback = fullScreenContentCallback,
            onPaidEventListener = onPaidEventListener,
        )
    }

    /**
     * Loads a native ad with automatic RevenueCat ad event tracking.
     *
     * Creates an [AdLoader], wires up tracking for loaded, failed-to-load, impression, click,
     * and revenue events, then starts loading.
     *
     * The [onAdLoaded] lambda delivers the [NativeAd] instance — this is native-ad-specific
     * and not part of [AdListener], so it is kept as a lambda. All other lifecycle events
     * are received through the optional [adListener].
     *
     * @param context The context.
     * @param adUnitId The AdMob ad unit ID.
     * @param adRequest The [AdRequest] to use.
     * @param placement A placement identifier for RevenueCat tracking.
     * @param nativeAdOptions Optional [NativeAdOptions] to configure the native ad request.
     * @param adListener Optional [AdListener] to receive ad lifecycle events.
     *   RevenueCat tracking for impression, click, and failed-to-load is injected
     *   transparently before each delegate call.
     * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
     *   RevenueCat revenue tracking is called first, then forwarded to this listener.
     * @param onAdLoaded Called with the loaded [NativeAd] (already tracked).
     * @return The [AdLoader] instance. Retain this if you need to load more ads.
     */
    @JvmStatic
    @JvmOverloads
    fun loadAndTrackNativeAd(
        context: Context,
        adUnitId: String,
        adRequest: AdRequest,
        placement: String? = null,
        nativeAdOptions: NativeAdOptions? = null,
        adListener: AdListener? = null,
        onPaidEventListener: OnPaidEventListener? = null,
        onAdLoaded: (NativeAd) -> Unit = {},
    ): AdLoader {
        return loadAndTrackNativeAdInternal(
            context = context,
            adUnitId = adUnitId,
            adRequest = adRequest,
            placement = placement,
            nativeAdOptions = nativeAdOptions,
            adListener = adListener,
            onPaidEventListener = onPaidEventListener,
            onAdLoaded = onAdLoaded,
        )
    }

    /**
     * Sets up RevenueCat ad-event tracking for the given [AdView] and loads the ad.
     *
     * This works for both programmatically-created and **XML-declared** `AdView`s.
     * When the `AdView` is declared in XML with `app:adUnitId` and `app:adSize`,
     * those attributes are read automatically — just call this method after inflation.
     *
     * Wraps the [adListener] (or the one already set on the [AdView]) with a
     * tracking listener that automatically tracks loaded, displayed, opened,
     * and failed-to-load events. Revenue is tracked via `OnPaidEventListener`.
     *
     * If an `AdListener` or `OnPaidEventListener` is already set on the [AdView]
     * before calling this method, it will be preserved and called after the
     * RevenueCat tracking calls. An explicit [adListener] parameter takes
     * precedence over a previously set listener.
     *
     * **Important:** Do not reassign [AdView.adListener] or
     * [AdView.onPaidEventListener] after calling this method, as doing so will
     * replace the tracking wrappers and break RevenueCat event tracking.
     *
     * @param adView The [AdView] to track and load.
     * @param adRequest The [AdRequest] to use.
     * @param placement A placement identifier (e.g., "home_screen_banner").
     * @param adListener Optional [AdListener] to receive ad lifecycle events.
     *   If `null` and an [AdListener] is already set on this [AdView], the
     *   existing listener will be used as the delegate.
     * @param onPaidEventListener Optional [OnPaidEventListener] to receive paid events.
     *   RevenueCat revenue tracking is called first, then forwarded to this listener.
     *   If `null` and an [OnPaidEventListener] is already set on this [AdView], the
     *   existing listener will be used as the delegate.
     */
    @JvmStatic
    @JvmOverloads
    fun loadAndTrackBannerAd(
        adView: AdView,
        adRequest: AdRequest,
        placement: String? = null,
        adListener: AdListener? = null,
        onPaidEventListener: OnPaidEventListener? = null,
    ) {
        loadAndTrackBannerAdInternal(
            adView = adView,
            adRequest = adRequest,
            placement = placement,
            adListener = adListener,
            onPaidEventListener = onPaidEventListener,
        )
    }
}
