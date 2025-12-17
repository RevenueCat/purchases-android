package com.revenuecat.sample.admob.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.sample.admob.data.Constants

/**
 * AdMobManager - Centralized manager for AdMob integration with RevenueCat ad event tracking.
 *
 * This class demonstrates how to:
 * 1. Load different ad formats (Banner, Interstitial, Native)
 * 2. Track all 5 RevenueCat ad events:
 *    - trackAdLoaded: When ad successfully loads
 *    - trackAdDisplayed: When ad is shown to the user
 *    - trackAdOpened: When user clicks on the ad
 *    - trackAdRevenue: When ad generates revenue (via OnPaidEventListener)
 *    - trackAdFailedToLoad: When ad fails to load
 * 3. Map AdMob data to RevenueCat event data structures
 * 4. Handle errors and edge cases
 *
 * IMPORTANT: This uses RevenueCat's Internal API (@InternalRevenueCatAPI) which:
 * - Requires opt-in annotation
 * - May change without notice
 * - Has no compatibility guarantees
 */
@OptIn(InternalRevenueCatAPI::class)
class AdMobManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var currentNativeAd: NativeAd? = null

    /**
     * Load a banner ad with full RevenueCat event tracking.
     *
     * This demonstrates:
     * - AdLoaded event when banner loads successfully
     * - AdDisplayed event when banner is rendered
     * - AdOpened event when user clicks the banner
     * - AdRevenue event when banner generates revenue
     *
     * @param adView The AdView to load the banner into
     * @param adUnitId The AdMob ad unit ID (use test IDs from Constants)
     * @param placement Optional placement identifier (e.g., "home_screen_banner")
     */
    fun loadBannerAd(
        adView: AdView,
        adUnitId: String = Constants.AdMob.BANNER_AD_UNIT_ID,
        placement: String = "banner_home"
    ) {
        Log.d(TAG, "Loading banner ad with unit ID: $adUnitId")

        // Set up the OnPaidEventListener to track ad revenue
        adView.onPaidEventListener = OnPaidEventListener { adValue ->
            val responseInfo = adView.responseInfo
            trackAdRevenue(
                adUnitId = adUnitId,
                placement = placement,
                responseInfo = responseInfo,
                adValue = adValue
            )
        }

        // Set up AdListener to track other ad events
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully")
                val responseInfo = adView.responseInfo
                trackAdLoaded(adUnitId, placement, responseInfo)
            }

            override fun onAdImpression() {
                Log.d(TAG, "Banner ad displayed (impression recorded)")
                val responseInfo = adView.responseInfo
                trackAdDisplayed(adUnitId, placement, responseInfo)
            }

            override fun onAdClicked() {
                Log.d(TAG, "Banner ad clicked (opened)")
                val responseInfo = adView.responseInfo
                trackAdOpened(adUnitId, placement, responseInfo)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "Banner ad failed to load: ${loadAdError.message}")
                trackAdFailedToLoad(adUnitId, placement, loadAdError)
            }
        }

        // Load the ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    /**
     * Load an interstitial ad with full RevenueCat event tracking.
     *
     * This demonstrates:
     * - AdLoaded event when interstitial loads successfully
     * - AdDisplayed event when interstitial is shown
     * - AdOpened event when user interacts with the interstitial
     * - AdRevenue event when interstitial generates revenue
     *
     * @param adUnitId The AdMob ad unit ID (use test IDs from Constants)
     * @param placement Optional placement identifier (e.g., "level_complete_interstitial")
     * @param onAdLoaded Callback when ad is loaded and ready to show
     * @param onAdFailedToLoad Callback when ad fails to load
     */
    fun loadInterstitialAd(
        adUnitId: String = Constants.AdMob.INTERSTITIAL_AD_UNIT_ID,
        placement: String = "interstitial_main",
        onAdLoaded: (() -> Unit)? = null,
        onAdFailedToLoad: ((String) -> Unit)? = null
    ) {
        Log.d(TAG, "Loading interstitial ad with unit ID: $adUnitId")

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad

                    val responseInfo = ad.responseInfo
                    trackAdLoaded(adUnitId, placement, responseInfo)

                    // Set up OnPaidEventListener for revenue tracking
                    ad.onPaidEventListener = OnPaidEventListener { adValue ->
                        trackAdRevenue(
                            adUnitId = adUnitId,
                            placement = placement,
                            responseInfo = ad.responseInfo,
                            adValue = adValue
                        )
                    }

                    // Set up FullScreenContentCallback for display and interaction tracking
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad displayed")
                            trackAdDisplayed(adUnitId, placement, ad.responseInfo)
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "Interstitial ad clicked (opened)")
                            trackAdOpened(adUnitId, placement, ad.responseInfo)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed")
                            interstitialAd = null
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                            interstitialAd = null
                        }
                    }

                    onAdLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                    interstitialAd = null
                    trackAdFailedToLoad(adUnitId, placement, loadAdError)
                    onAdFailedToLoad?.invoke(loadAdError.message)
                }
            }
        )
    }

    /**
     * Show the loaded interstitial ad.
     *
     * @param activity The activity to show the ad on
     * @return true if ad was shown, false if no ad is loaded
     */
    fun showInterstitialAd(activity: Activity): Boolean {
        return if (interstitialAd != null) {
            Log.d(TAG, "Showing interstitial ad")
            interstitialAd?.show(activity)
            true
        } else {
            Log.w(TAG, "Interstitial ad not ready to show")
            false
        }
    }

    /**
     * Load a native ad with full RevenueCat event tracking.
     *
     * This demonstrates:
     * - AdLoaded event when native ad loads successfully
     * - AdDisplayed event when native ad is rendered (call trackNativeAdDisplayed after rendering)
     * - AdOpened event when user clicks the native ad
     * - AdRevenue event when native ad generates revenue
     *
     * @param adUnitId The AdMob ad unit ID (use test IDs from Constants)
     * @param placement Optional placement identifier (e.g., "feed_native_ad")
     * @param onAdLoaded Callback with loaded NativeAd
     * @param onAdFailedToLoad Callback when ad fails to load
     */
    fun loadNativeAd(
        adUnitId: String = Constants.AdMob.NATIVE_AD_UNIT_ID,
        placement: String = "native_main",
        onAdLoaded: ((NativeAd) -> Unit)? = null,
        onAdFailedToLoad: ((String) -> Unit)? = null
    ) {
        Log.d(TAG, "Loading native ad with unit ID: $adUnitId")

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad loaded successfully")

                // Clean up old ad if exists
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd

                val responseInfo = nativeAd.responseInfo
                trackAdLoaded(adUnitId, placement, responseInfo)

                // Set up OnPaidEventListener for revenue tracking
                nativeAd.setOnPaidEventListener { adValue ->
                    trackAdRevenue(
                        adUnitId = adUnitId,
                        placement = placement,
                        responseInfo = nativeAd.responseInfo,
                        adValue = adValue
                    )
                }

                // Note: Native ad click tracking would require implementing
                // a custom NativeAdView to capture click events

                onAdLoaded?.invoke(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: ${loadAdError.message}")
                    trackAdFailedToLoad(adUnitId, placement, loadAdError)
                    onAdFailedToLoad?.invoke(loadAdError.message)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Track native ad impression (call this after you've rendered the ad in your UI).
     *
     * Note: Native ads require manual impression tracking because AdMob doesn't have
     * an automatic impression callback like banner ads.
     *
     * @param adUnitId The ad unit ID used
     * @param placement The placement identifier
     * @param nativeAd The rendered NativeAd
     */
    fun trackNativeAdDisplayed(
        adUnitId: String,
        placement: String,
        nativeAd: NativeAd
    ) {
        Log.d(TAG, "Native ad displayed (manually tracked)")
        trackAdDisplayed(adUnitId, placement, nativeAd.responseInfo)
    }

    /**
     * Load an ad with an invalid ad unit ID to demonstrate error tracking.
     *
     * This intentionally uses an invalid ad unit ID to trigger a load failure,
     * demonstrating how to handle and track ad load errors with RevenueCat.
     */
    fun loadAdWithError(placement: String = "error_test") {
        Log.d(TAG, "Loading ad with invalid ID to test error tracking")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            Constants.AdMob.INVALID_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    // This should not happen with invalid ID
                    Log.w(TAG, "Unexpected: Ad loaded with invalid ID")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "Expected error occurred: ${loadAdError.message}")
                    trackAdFailedToLoad(
                        Constants.AdMob.INVALID_AD_UNIT_ID,
                        placement,
                        loadAdError
                    )
                }
            }
        )
    }

    // ============================================================================
    // RevenueCat Ad Event Tracking Methods
    // ============================================================================

    /**
     * Track when an ad is successfully loaded.
     *
     * Maps to: Purchases.sharedInstance.adTracker.trackAdLoaded()
     */
    private fun trackAdLoaded(
        adUnitId: String,
        placement: String,
        responseInfo: com.google.android.gms.ads.ResponseInfo?
    ) {
        try {
            val data = AdLoadedData(
                networkName = responseInfo?.mediationAdapterClassName ?: "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = responseInfo?.responseId ?: ""
            )
            Purchases.sharedInstance.adTracker.trackAdLoaded(data)
            Log.d(TAG, "✅ Tracked: Ad Loaded - placement=$placement")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track ad loaded event", e)
        }
    }

    /**
     * Track when an ad is displayed to the user (impression).
     *
     * Maps to: Purchases.sharedInstance.adTracker.trackAdDisplayed()
     */
    private fun trackAdDisplayed(
        adUnitId: String,
        placement: String,
        responseInfo: com.google.android.gms.ads.ResponseInfo?
    ) {
        try {
            val data = AdDisplayedData(
                networkName = responseInfo?.mediationAdapterClassName ?: "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = responseInfo?.responseId ?: ""
            )
            Purchases.sharedInstance.adTracker.trackAdDisplayed(data)
            Log.d(TAG, "✅ Tracked: Ad Displayed - placement=$placement")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track ad displayed event", e)
        }
    }

    /**
     * Track when a user clicks/opens an ad.
     *
     * Maps to: Purchases.sharedInstance.adTracker.trackAdOpened()
     */
    private fun trackAdOpened(
        adUnitId: String,
        placement: String,
        responseInfo: com.google.android.gms.ads.ResponseInfo?
    ) {
        try {
            val data = AdOpenedData(
                networkName = responseInfo?.mediationAdapterClassName ?: "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = responseInfo?.responseId ?: ""
            )
            Purchases.sharedInstance.adTracker.trackAdOpened(data)
            Log.d(TAG, "✅ Tracked: Ad Opened - placement=$placement")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track ad opened event", e)
        }
    }

    /**
     * Track ad revenue using AdMob's OnPaidEventListener.
     *
     * This is the most important event for monetization tracking.
     * Maps to: Purchases.sharedInstance.adTracker.trackAdRevenue()
     *
     * IMPORTANT: AdMob provides revenue in micros (divide by 1,000,000 for actual value).
     * RevenueCat expects revenueMicros, so we pass the value directly.
     */
    private fun trackAdRevenue(
        adUnitId: String,
        placement: String,
        responseInfo: com.google.android.gms.ads.ResponseInfo?,
        adValue: com.google.android.gms.ads.AdValue
    ) {
        try {
            val data = AdRevenueData(
                networkName = responseInfo?.mediationAdapterClassName ?: "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                placement = placement,
                adUnitId = adUnitId,
                impressionId = responseInfo?.responseId ?: "",
                revenueMicros = adValue.valueMicros,
                currency = adValue.currencyCode,
                precision = mapAdMobPrecisionToRevenueCat(adValue.precisionType)
            )
            Purchases.sharedInstance.adTracker.trackAdRevenue(data)

            val revenueInDollars = adValue.valueMicros / 1_000_000.0
            Log.d(TAG, "✅ Tracked: Ad Revenue - $${revenueInDollars} ${adValue.currencyCode} " +
                    "(precision: ${data.precision}) - placement=$placement")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track ad revenue event", e)
        }
    }

    /**
     * Track when an ad fails to load.
     *
     * Maps to: Purchases.sharedInstance.adTracker.trackAdFailedToLoad()
     */
    private fun trackAdFailedToLoad(
        adUnitId: String,
        placement: String,
        loadAdError: LoadAdError
    ) {
        try {
            val data = AdFailedToLoadData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                placement = placement,
                adUnitId = adUnitId,
                mediatorErrorCode = loadAdError.code
            )
            Purchases.sharedInstance.adTracker.trackAdFailedToLoad(data)
            Log.d(TAG, "✅ Tracked: Ad Failed to Load - code=${loadAdError.code}, " +
                    "message=${loadAdError.message}, placement=$placement")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track ad failed to load event", e)
        }
    }

    /**
     * Map AdMob's PrecisionType to RevenueCat's AdRevenuePrecision.
     *
     * AdMob precision types:
     * - PRECISE (0): Publisher is paid for this impression
     * - ESTIMATED (1): Estimate, publisher might not be paid
     * - PUBLISHER_PROVIDED (2): Revenue value provided by the publisher
     * - UNKNOWN (3): Precision is unknown
     */
    private fun mapAdMobPrecisionToRevenueCat(precisionType: Int): AdRevenuePrecision {
        return when (precisionType) {
            com.google.android.gms.ads.AdValue.PrecisionType.PRECISE ->
                AdRevenuePrecision.EXACT
            com.google.android.gms.ads.AdValue.PrecisionType.ESTIMATED ->
                AdRevenuePrecision.ESTIMATED
            com.google.android.gms.ads.AdValue.PrecisionType.PUBLISHER_PROVIDED ->
                AdRevenuePrecision.PUBLISHER_DEFINED
            else ->
                AdRevenuePrecision.UNKNOWN
        }
    }

    /**
     * Clean up resources when done.
     */
    fun cleanup() {
        currentNativeAd?.destroy()
        currentNativeAd = null
        interstitialAd = null
    }

    companion object {
        private const val TAG = "AdMobManager"
    }
}
