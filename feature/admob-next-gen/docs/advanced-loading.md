# Advanced loading

This guide covers alternative loading workflows for the
[RevenueCat Google Mobile Ads Next-Gen Adapter](../README.md). The adapter preserves Google's original callback and
result types while installing RevenueCat tracking.

## Contents

- [Suspending loads](#suspending-loads)
- [Server-to-server responses](#server-to-server-responses)
- [Native batch loading](#native-batch-loading)
- [Preloading](#preloading)

## Suspending loads

Interstitial, rewarded, rewarded-interstitial, app-open, and native helpers have suspending overloads. They return
Google's original result unchanged, with event tracking already installed on successful ads:

```kotlin
lifecycleScope.launch {
    when (
        val result = Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
            adRequest = AdRequest.Builder("AD_UNIT_ID").build(),
            placement = "game_interstitial",
        )
    ) {
        is AdLoadResult.Success -> interstitialAd = result.ad
        is AdLoadResult.Failure -> interstitialAd = null
    }
}
```

Direct banner loading remains callback-based because Google does not provide a supported suspending `AdView.loadAd`
API.

## Server-to-server responses

Every format can track an opaque server-to-server ad response. Provide the ad unit ID explicitly because neither a
failed load nor the opaque response reliably supplies it:

```kotlin
Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAdFromResponse(
    adResponse = serverAdResponse,
    adUnitId = "AD_UNIT_ID",
    placement = "game_interstitial",
    loadCallback = object : AdLoadCallback<InterstitialAd> {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            interstitialAd = null
        }
    },
    adEventCallback = interstitialAdEventCallback,
)
```

Use the corresponding `loadAndTrack*FromResponse` helper for other formats. Banner responses can be loaded directly
through `AdView.loadAndTrackAdFromResponse` or through `AdTracker.loadAndTrackBannerAdFromResponse`. Google exposes a
suspending response-loading API only for banners, so the other response helpers are callback-based.

## Native batch loading

Use `loadAndTrackNativeAds` to load several native results. Its suspending overload returns Google's original `Flow`;
results are tracked and configured as they are collected:

```kotlin
lifecycleScope.launch {
    Purchases.sharedInstance.adTracker.loadAndTrackNativeAds(
        adRequest = adRequest,
        maxNumberOfAds = 3,
        placement = "home_feed",
    ).collect { result ->
        when (result) {
            is NativeAdLoadResult.NativeAdSuccess -> showNativeAd(result.ad)
            is NativeAdLoadResult.CustomNativeAdSuccess -> showCustomNativeAd(result.ad)
            is NativeAdLoadResult.BannerAdSuccess -> showBannerAd(result.ad)
            is NativeAdLoadResult.Failure -> handleError(result.error)
        }
    }
}
```

A callback overload is also available and forwards `onAdLoadingCompleted` after Google finishes the batch.

## Preloading

All six formats support Google's preloaders through the same two-stage pattern:

```kotlin
val preloadConfiguration = PreloadConfiguration(
    request = AdRequest.Builder("AD_UNIT_ID").build(),
    bufferSize = 2,
)

InterstitialAdPreloader.startAndTrack(
    preloadId = "game-interstitial-buffer",
    preloadConfiguration = preloadConfiguration,
    placement = "game_interstitial_preload",
    preloadCallback = object : PreloadCallback {},
)

// Later, adopt a buffered ad. Polling does not emit another loaded event.
interstitialAd = InterstitialAdPreloader.pollAndTrackAd(
    preloadId = "game-interstitial-buffer",
    placement = "game_interstitial",
    adEventCallback = interstitialAdEventCallback,
)

interstitialAd?.show(this)
```

The placements passed to `startAndTrack` and `pollAndTrackAd` are independent. See the main guide's
[placement explanation](../README.md#placement) for the complete lifecycle.

Use the corresponding `AppOpenAdPreloader`, `BannerAdPreloader`, `NativeAdPreloader`, `RewardedAdPreloader`, or
`RewardedInterstitialAdPreloader` extensions for other formats. Their differences are limited to the Google request,
result, and callback types:

| Format | Request or result-specific behavior after polling |
| --- | --- |
| App open | Build an `AppOpenAdRequest`; the returned `AppOpenAd` can be shown normally |
| Banner | Register the returned `BannerAd` with `AdView.registerBannerAd` |
| Interstitial | Show the returned `InterstitialAd` normally or with a placement override |
| Native | Handle the returned native, custom-native, or banner success result |
| Rewarded | Supply Google's reward listener or enable RevenueCat reward verification before showing |
| Rewarded interstitial | Supply Google's reward listener or enable RevenueCat reward verification before showing |

For example, polling a banner does not register it automatically:

```kotlin
val bannerAd = BannerAdPreloader.pollAndTrackAd(
    preloadId = "home-banner-buffer",
    placement = "home_banner",
    adEventCallback = bannerAdEventCallback,
    bannerAdRefreshCallback = bannerAdRefreshCallback,
)

if (bannerAd != null) {
    adView.registerBannerAd(bannerAd, this)
}
```

If preloading was started through Google's plain `start` API, `pollAndTrackAd` still installs tracking for later
lifecycle events. RevenueCat cannot observe the earlier preload completion, so it does not synthesize a loaded event.
Swipeable interstitial ads are not currently supported by this integration.
