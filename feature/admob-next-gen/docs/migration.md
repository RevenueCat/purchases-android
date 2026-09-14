# Migrate from the legacy AdMob adapter

`purchases-admob-next-gen` is not a drop-in binary replacement for `purchases-admob`. Migrate the Google Mobile Ads
SDK and RevenueCat adapter together, and review Google's
[Next-Gen migration guide](https://developers.google.com/admob/android/next-gen/migration) if you use mediation.

## Contents

- [Replace the dependencies](#replace-the-dependencies)
- [Update imports and initialization](#update-imports-and-initialization)
- [Update RevenueCat tracking calls](#update-revenuecat-tracking-calls)
- [Review callback handling](#review-callback-handling)

## Replace the dependencies

Remove both legacy artifacts and add their Next-Gen replacements. Do not keep both SDK generations in the same app:

```kotlin
dependencies {
    // Remove:
    // implementation("com.revenuecat.purchases:purchases-admob:$revenueCatVersion")
    // implementation("com.google.android.gms:play-services-ads:<version>")

    implementation("com.revenuecat.purchases:purchases-admob-next-gen:$revenueCatVersion")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.3.0")
}
```

If mediation adapters pull either legacy Google artifact back into the dependency graph, add the global exclusions
shown in the main guide's [installation section](../README.md#installation).

## Update imports and initialization

Update RevenueCat imports from `com.revenuecat.purchases.admob` to `com.revenuecat.purchases.admob.nextgen`. Google
Mobile Ads types move from `com.google.android.gms.ads` to `com.google.android.libraries.ads.mobile.sdk`. Initialize
the Next-Gen SDK using `InitializationConfig` before loading ads.

## Update RevenueCat tracking calls

Most RevenueCat helper names remain the same, but their Google SDK parameters change:

| Format | Legacy adapter | Next-Gen adapter |
| --- | --- | --- |
| Banner | `AdView.loadAndTrackAd(AdRequest, ...)` | `AdView.loadAndTrackAd(BannerAdRequest, ...)` |
| Interstitial | `AdTracker.loadAndTrackInterstitialAd(...)` | `AdTracker.loadAndTrackInterstitialAd(...)` |
| Rewarded | `AdTracker.loadAndTrackRewardedAd(...)` | `AdTracker.loadAndTrackRewardedAd(...)` |
| Rewarded interstitial | `AdTracker.loadAndTrackRewardedInterstitialAd(...)` | `AdTracker.loadAndTrackRewardedInterstitialAd(...)` |
| App open | `AdTracker.loadAndTrackAppOpenAd(...)` | `AdTracker.loadAndTrackAppOpenAd(...)` |
| Native | `AdLoader.Builder.forNativeAdWithTracking(...)` | `AdTracker.loadAndTrackNativeAd(...)` or `loadAndTrackNativeAds(...)` |

`BannerAdRequest` now owns the ad unit ID and size that the legacy SDK stored on `AdView`. Full-screen requests also
own their ad unit ID instead of receiving `context` and `adUnitId` as separate load parameters. Format-specific load
callbacks become `AdLoadCallback<AdType>`, while lifecycle and paid events move to each format's event callback.

The reward-verification helper names are unchanged. Keep existing placement values when they represent the same ad
slots so reporting remains consistent across the migration.

## Review callback handling

Next-Gen callbacks may arrive on a background thread, and assigning an event callback directly after a tracked load
replaces RevenueCat's wrapper. See the main guide's [callback handling](../README.md#callback-handling) section for
the tracking-safe setters and threading contract.
