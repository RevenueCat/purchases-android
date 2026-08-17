# RevenueCat Google Mobile Ads Next-Gen Adapter

The `purchases-admob-next-gen` module integrates RevenueCat with the
[Google Mobile Ads Next-Gen SDK](https://developers.google.com/admob/android/next-gen/quick-start).

> [!IMPORTANT]
> `purchases-admob-next-gen` and the legacy `purchases-admob` adapter are mutually exclusive.
> Remove the legacy adapter and `com.google.android.gms:play-services-ads` before adding this module.

## Requirements

- Android API level 24 or newer
- Compile SDK 35 or newer
- Kotlin 1.9 or newer

## Installation

Make sure Google's Maven repository and Maven Central are available to your build:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add RevenueCat, the Next-Gen adapter, and Google Mobile Ads Next-Gen to your app module:

```kotlin
val revenueCatVersion = "<version>"

dependencies {
    implementation("com.revenuecat.purchases:purchases:$revenueCatVersion")
    implementation("com.revenuecat.purchases:purchases-admob-next-gen:$revenueCatVersion")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.3.0")
}
```

If you use the RevenueCat BOM, it also manages the adapter version:

```kotlin
val revenueCatVersion = "<version>"

dependencies {
    implementation(platform("com.revenuecat.purchases:purchases-bom:$revenueCatVersion"))
    implementation("com.revenuecat.purchases:purchases")
    implementation("com.revenuecat.purchases:purchases-admob-next-gen")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.3.0")
}
```

Mediation adapters can bring the legacy Google Mobile Ads SDK into the dependency graph. Exclude both legacy
artifacts globally to prevent duplicate-symbol build failures:

```kotlin
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}
```

See Google's
[migration guide](https://developers.google.com/admob/android/next-gen/migration)
for additional mediation-specific requirements.

## Initialize Google Mobile Ads Next-Gen

Google Mobile Ads Next-Gen must be initialized before loading ads or calling other `MobileAds` APIs. Initialize it
once, ideally at app launch, on a background thread:

```kotlin
import android.app.Application
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                this@MyApplication,
                InitializationConfig.Builder("ADMOB_APP_ID").build(),
            ) {
                // Adapter initialization is complete.
            }
        }
    }
}
```

If you use mediation, wait for the initialization callback before loading ads so every mediation adapter is ready.
Configure consent and any request-specific privacy flags before initialization because the SDK or a mediation partner
may preload ads during initialization.

## Preloading placement

Preloading has two separate tracking stages. The optional placement passed to `startAndTrack` applies to preload
success and failure events. The optional placement passed to `pollAndTrackAd` is independent and applies to the
ad's later display, click, revenue, and banner-refresh events.

## Preloading

The adapter supports Google's app-open, banner, interstitial, native, rewarded, and rewarded-interstitial
preloaders. Swipeable interstitial ads are not yet supported by this integration.

### App-open ads

```kotlin
val appOpenPreloadConfiguration = PreloadConfiguration(
    request = AppOpenAdRequest.Builder("AD_UNIT_ID").build(),
    bufferSize = 2,
)

AppOpenAdPreloader.startAndTrack(
    preloadId = "app-open-buffer",
    preloadConfiguration = appOpenPreloadConfiguration,
    placement = "app_open_preload",
)

// Later, adopt a buffered app-open ad. This does not emit another loaded event.
appOpenAd = AppOpenAdPreloader.pollAndTrackAd(
    preloadId = "app-open-buffer",
    placement = "app_open",
    adEventCallback = appOpenAdEventCallback,
)

appOpenAd?.show(this)
```

### Banner ads

```kotlin
val bannerPreloadConfiguration = PreloadConfiguration(
    request = BannerAdRequest.Builder(
        "AD_UNIT_ID",
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360),
    ).build(),
    bufferSize = 2,
)

BannerAdPreloader.startAndTrack(
    preloadId = "home-banner-buffer",
    preloadConfiguration = bannerPreloadConfiguration,
    placement = "home_banner_preload",
    preloadCallback = object : PreloadCallback {
        override fun onAdsExhausted(preloadId: String) {
            // The buffer is empty. This callback is forwarded but is not a RevenueCat ad event.
        }
    },
)

// Later, adopt a buffered banner. This does not emit another loaded event.
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

Continue to register the returned banner with Google's normal `AdView.registerBannerAd`; the adapter intentionally
does not combine polling and registration.

### Interstitial ads

```kotlin
val interstitialPreloadConfiguration = PreloadConfiguration(
    request = AdRequest.Builder("AD_UNIT_ID").build(),
    bufferSize = 2,
)

InterstitialAdPreloader.startAndTrack(
    preloadId = "game-interstitial-buffer",
    preloadConfiguration = interstitialPreloadConfiguration,
    placement = "game_interstitial_preload",
    preloadCallback = object : PreloadCallback {},
)

// Later, adopt a buffered interstitial. This does not emit another loaded event.
interstitialAd = InterstitialAdPreloader.pollAndTrackAd(
    preloadId = "game-interstitial-buffer",
    placement = "game_interstitial",
    adEventCallback = interstitialAdEventCallback,
)

interstitialAd?.show(this)
```

### Native ads

```kotlin
val nativePreloadConfiguration = PreloadConfiguration(
    request = NativeAdRequest.Builder(
        "AD_UNIT_ID",
        listOf(NativeAd.NativeAdType.NATIVE),
    ).build(),
    bufferSize = 2,
)

NativeAdPreloader.startAndTrack(
    preloadId = "native-buffer",
    preloadConfiguration = nativePreloadConfiguration,
    placement = "native_preload",
)

// Later, adopt a buffered result. This does not emit another loaded event.
val nativeResult = NativeAdPreloader.pollAndTrackAd(
    preloadId = "native-buffer",
    placement = "native",
    nativeAdEventCallback = nativeAdEventCallback,
    bannerAdEventCallback = bannerAdEventCallback,
    bannerAdRefreshCallback = bannerAdRefreshCallback,
)

when (nativeResult) {
    is NativeAdLoadResult.NativeAdSuccess -> showNativeAd(nativeResult.ad)
    is NativeAdLoadResult.CustomNativeAdSuccess -> showCustomNativeAd(nativeResult.ad)
    is NativeAdLoadResult.BannerAdSuccess -> adView.registerBannerAd(nativeResult.ad, this)
    null -> Unit
}
```

Google's native preloader can return native, custom-native, or banner results depending on the requested ad types.
`pollAndTrackAd` installs the matching lifecycle callbacks before returning any of these result variants.

### Rewarded ads

```kotlin
val rewardedPreloadConfiguration = PreloadConfiguration(
    request = AdRequest.Builder("AD_UNIT_ID").build(),
    bufferSize = 2,
)

RewardedAdPreloader.startAndTrack(
    preloadId = "rewarded-buffer",
    preloadConfiguration = rewardedPreloadConfiguration,
    placement = "rewarded_preload",
)

// Later, adopt a buffered rewarded ad. This does not emit another loaded event.
rewardedAd = RewardedAdPreloader.pollAndTrackAd(
    preloadId = "rewarded-buffer",
    placement = "rewarded",
    adEventCallback = rewardedAdEventCallback,
)

rewardedAd?.show(this) { rewardItem ->
    grantReward(rewardItem)
}
```

### Rewarded-interstitial ads

```kotlin
val rewardedInterstitialPreloadConfiguration = PreloadConfiguration(
    request = AdRequest.Builder("AD_UNIT_ID").build(),
    bufferSize = 2,
)

RewardedInterstitialAdPreloader.startAndTrack(
    preloadId = "rewarded-interstitial-buffer",
    preloadConfiguration = rewardedInterstitialPreloadConfiguration,
    placement = "rewarded_interstitial_preload",
)

// Later, adopt a buffered rewarded-interstitial ad. This does not emit another loaded event.
rewardedInterstitialAd = RewardedInterstitialAdPreloader.pollAndTrackAd(
    preloadId = "rewarded-interstitial-buffer",
    placement = "rewarded_interstitial",
    adEventCallback = rewardedInterstitialAdEventCallback,
)

rewardedInterstitialAd?.show(this) { rewardItem ->
    grantReward(rewardItem)
}
```

If preloading was started through Google's plain `start` API, `pollAndTrackAd` still installs tracking for later
lifecycle events. RevenueCat cannot retroactively observe the original preload completion, so it does not synthesize
a loaded event when the ad is polled.
## Placement

Use the optional `placement` to identify the logical location of an ad in your app, such as
`"level_complete_interstitial"`. Choose stable names and apply them consistently so events from the same slot can be
grouped together. The load-time placement is attached to load success and failure events and is the default for later
events. If the final location is only known when the ad is shown, use `InterstitialAd.show(activity, placement)` to
override the placement for display, click, and revenue events. Calling the Next-Gen SDK's regular `show(activity)`
keeps the load-time placement.

## Usage

### Interstitial ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

InterstitialAd.load(
    adRequest,
    object : AdLoadCallback<InterstitialAd> {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                }
            }
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            interstitialAd = null
        }
    },
)

// Later, to show:
interstitialAd?.show(this)
```

**With RevenueCat tracking**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
    adRequest = adRequest,
    placement = "game_interstitial",
    loadCallback = object : AdLoadCallback<InterstitialAd> {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            interstitialAd = null
        }
    },
    adEventCallback = object : InterstitialAdEventCallback {
        override fun onAdDismissedFullScreenContent() {
            interstitialAd = null
        }

        override fun onAdFailedToShowFullScreenContent(
            fullScreenContentError: FullScreenContentError,
        ) {
            interstitialAd = null
        }
    },
)

// Later, override the load-time placement for events emitted while showing:
interstitialAd?.show(this, placement = "level_complete_interstitial")
```

The coroutine overload returns Google's original load result:

```kotlin
lifecycleScope.launch {
    when (
        val result = Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
            adRequest = adRequest,
            placement = "game_interstitial",
        )
    ) {
        is AdLoadResult.Success -> interstitialAd = result.ad
        is AdLoadResult.Failure -> interstitialAd = null
    }
}
```

> [!IMPORTANT]
> Do not assign `interstitialAd.adEventCallback` directly after a tracked load. Direct assignment replaces
> RevenueCat's tracking wrapper and prevents `show(activity, placement)` from applying its placement override. Pass
> the callback to `loadAndTrackInterstitialAd`, or replace only the forwarded callback safely:

```kotlin
interstitialAd?.setTrackingAdEventCallback(newAdEventCallback)
```

Google Mobile Ads Next-Gen invokes load and event callbacks on a background thread. Dispatch explicitly to the main
thread before updating views or other UI-confined state from a callback.

## Supported formats

| Format       | RevenueCat tracking entry point                                 |
| ------------ | --------------------------------------------------------------- |
| Interstitial | `AdTracker.loadAndTrackInterstitialAd()`                        |

## Events tracked

All formats — banner, interstitial, rewarded, rewarded interstitial, app open, and native — report these
RevenueCat ad events:

- **Ad Loaded** — the ad loaded successfully. Banner auto-refresh reports each refreshed creative as a new load.
- **Ad Failed to Load** — the load failed, tagged with the SDK's numeric error code. Banner refresh failures
  report here too.
- **Ad Displayed** — the ad was shown. Which callback triggers this depends on the format, see below.
- **Ad Opened** — the user clicked the ad.
- **Ad Revenue** — the ad generated revenue, with the amount, currency, and precision reported by the SDK.

Every event carries the ad format, the ad unit, and the optional `placement`. Ad Loaded, Ad Displayed, Ad Opened
and Ad Revenue also carry the mediation network and the impression id; Ad Failed to Load carries the error code
instead, since no creative was served.

Network and impression id are read when the callback fires rather than when the ad loads, so an auto-refreshing
banner attributes events to the creative currently on screen instead of the first one loaded.

### Display triggers

| Format                                                  | Trigger                          |
| ------------------------------------------------------- | -------------------------------- |
| Banner, native                                          | `onAdImpression()`               |
| Interstitial, rewarded, rewarded interstitial, app open | `onAdShowedFullScreenContent()`  |

Full-screen formats emit both callbacks, so tracking the display from only one of them is what keeps a single
display from being counted twice. Banner and native emit `onAdShowedFullScreenContent()` too — the SDK reports any
screen-covering overlay there, such as an expanding banner or the AdChoices overlay — and the adapter forwards it
without tracking a display. The adapter keeps no deduplication state.

### Callback forwarding

The adapter wraps your callbacks instead of replacing them. Every callback the SDK exposes is forwarded to your own
callback after the RevenueCat event is tracked — including the event callbacks the adapter does not track:
`onAdDismissedFullScreenContent`, `onAdFailedToShowFullScreenContent`, `onAppEvent` (banner, interstitial),
`onAdMetadataChanged` (rewarded, rewarded interstitial), and `onAdSwipeGestureClicked` /
`onCustomMuteThisAdReported` (native).

Forwarding is unconditional. If RevenueCat is not configured the adapter logs a warning and skips tracking, and if
tracking itself fails the adapter logs the error and swallows it. Either way your callback still runs.

## Experimental API

The tracking helpers use `@ExperimentalPreviewRevenueCatPurchasesAPI`. Opt in at the narrowest scope that calls them:

```kotlin
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
fun loadInterstitial() {
    val adRequest = AdRequest.Builder("AD_UNIT_ID").build()
    Purchases.sharedInstance.adTracker.loadAndTrackInterstitialAd(
        adRequest = adRequest,
        placement = "game_interstitial",
    )
}
```
