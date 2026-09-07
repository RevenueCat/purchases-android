# RevenueCat Google Mobile Ads Next-Gen Adapter

The `purchases-admob-next-gen` module integrates RevenueCat with the
[Google Mobile Ads Next-Gen SDK](https://developers.google.com/admob/android/next-gen/quick-start).

**Kotlin only.** The load-and-track helpers are not available from Java. Java apps can still use the core
[`AdTracker`](https://revenuecat.github.io/purchases-android/docs/core/com.revenuecat.purchases.ads.events/-ad-tracker/index.html)
APIs directly.

See the [AdMob Next-Gen sample](../../examples/admob-next-gen-sample/README.md) for a runnable Compose app covering
direct loading, preloading, placement overrides, native batch loading, and reward verification.

> [!IMPORTANT]
> `purchases-admob-next-gen` and the legacy `purchases-admob` adapter are mutually exclusive. Remove the legacy
> adapter and `com.google.android.gms:play-services-ads` before adding this module.

## Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Initialize Google Mobile Ads Next-Gen](#initialize-google-mobile-ads-next-gen)
- [Events tracked](#events-tracked)
- [Usage](#usage)
- [Placement](#placement)
- [Callback handling](#callback-handling)
- [Further guides](#further-guides)

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

Initialize Google Mobile Ads once before loading ads or calling other `MobileAds` APIs. Initialization should run on
a background thread:

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

## Events tracked

All formats automatically report these RevenueCat ad events:

- **Ad Loaded** — the ad loaded successfully. Banner auto-refresh reports each refreshed creative as a new load.
- **Ad Failed to Load** — the load failed, tagged with the SDK's numeric error code. Banner refresh failures report
  here too.
- **Ad Displayed** — the ad was shown.
- **Ad Opened** — the user clicked the ad.
- **Ad Revenue** — the ad generated revenue, with the amount, currency, and precision reported by Google.

Every event carries the ad format, ad unit, and optional placement. Ad Loaded, Ad Displayed, Ad Opened, and Ad
Revenue also carry the mediation network and impression ID. Ad Failed to Load carries the error code instead because
no creative was served.

Network and impression ID are read when each callback fires. This ensures that an auto-refreshing banner attributes
events to the creative currently on screen rather than the first one loaded.

## Usage

Each format below shows the Google Mobile Ads Next-Gen call followed by its RevenueCat-tracked equivalent. These
side-by-side examples make the migration explicit while preserving Google's request, result, and callback types.

### Banner ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adView = AdView(this)
binding.adViewContainer.addView(adView)

val adRequest = BannerAdRequest.Builder(
    "AD_UNIT_ID",
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360),
).build()

adView.loadAd(
    adRequest,
    object : AdLoadCallback<BannerAd> {
        override fun onAdLoaded(ad: BannerAd) {
            ad.adEventCallback = bannerAdEventCallback
            ad.bannerAdRefreshCallback = bannerAdRefreshCallback
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            // Handle the error.
        }
    },
)
```

**With RevenueCat tracking**

```kotlin
val adView = AdView(this)
binding.adViewContainer.addView(adView)

val adRequest = BannerAdRequest.Builder(
    "AD_UNIT_ID",
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360),
).build()

adView.loadAndTrackAd(
    adRequest = adRequest,
    placement = "home_banner",
    loadCallback = object : AdLoadCallback<BannerAd> {
        override fun onAdLoaded(ad: BannerAd) {
            // The AdView already displays this ad.
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            // Handle the error.
        }
    },
    adEventCallback = bannerAdEventCallback,
    bannerAdRefreshCallback = bannerAdRefreshCallback,
)
```

The equivalent `AdTracker` extension accepts the same view, request, and callbacks:

```kotlin
Purchases.sharedInstance.adTracker.loadAndTrackBannerAd(
    adView = adView,
    adRequest = adRequest,
    placement = "home_banner",
    loadCallback = loadCallback,
    adEventCallback = bannerAdEventCallback,
    bannerAdRefreshCallback = bannerAdRefreshCallback,
)
```

An `AdView` owns and displays its loaded banner, so the load callback is optional. Google does not offer a suspending
`AdView.loadAd` API. Although `BannerAd.load` has a suspending overload, Google deprecates it in favor of
`AdView.loadAd` or preloading, so the adapter does not wrap it.

### Interstitial ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

InterstitialAd.load(
    adRequest,
    object : AdLoadCallback<InterstitialAd> {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
            ad.adEventCallback = interstitialAdEventCallback
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
    adEventCallback = interstitialAdEventCallback,
)

// Later, optionally override the load-time placement:
interstitialAd?.show(this, placement = "level_complete_interstitial")
```

### Rewarded ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

RewardedAd.load(
    adRequest,
    object : AdLoadCallback<RewardedAd> {
        override fun onAdLoaded(ad: RewardedAd) {
            rewardedAd = ad
            ad.adEventCallback = rewardedAdEventCallback
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedAd = null
        }
    },
)

// Later, to show and grant the reward reported by Google:
rewardedAd?.show(this) { rewardItem ->
    grantReward(rewardItem)
}
```

**With RevenueCat tracking**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

Purchases.sharedInstance.adTracker.loadAndTrackRewardedAd(
    adRequest = adRequest,
    placement = "game_reward",
    loadCallback = object : AdLoadCallback<RewardedAd> {
        override fun onAdLoaded(ad: RewardedAd) {
            rewardedAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedAd = null
        }
    },
    adEventCallback = rewardedAdEventCallback,
)

// Later, optionally override the load-time placement while preserving Google's reward callback:
rewardedAd?.show(
    activity = this,
    placement = "level_complete_reward",
    onUserEarnedRewardListener = OnUserEarnedRewardListener { reward ->
        grantReward(reward)
    },
)
```

Reward delivery is separate from `RewardedAdEventCallback`. Always pass Google's required
`OnUserEarnedRewardListener` when showing an ordinary rewarded ad.

#### Reward verification

To use RevenueCat reward verification, enable it on the loaded or polled ad before showing it. Then use the
verification-aware `show` overload instead of Google's ordinary reward listener:

```kotlin
override fun onAdLoaded(ad: RewardedAd) {
    rewardedAd = ad.apply {
        enableRewardVerification()
    }
}

// Later, to show:
rewardedAd?.show(
    activity = this,
    placement = "level_complete_reward",
    rewardVerificationStarted = {
        // Verification polling has started.
    },
    rewardVerificationCompleted = { result ->
        rewardedAd = null
        if (result.verifiedReward != null) {
            // Handle the verified reward.
        } else if (result.failed) {
            // Verification failed.
        }
    },
)
```

The verification choice belongs to the loaded ad. Call `enableRewardVerification` after a direct load or poll and
before showing that ad.

### Rewarded interstitial ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

RewardedInterstitialAd.load(
    adRequest,
    object : AdLoadCallback<RewardedInterstitialAd> {
        override fun onAdLoaded(ad: RewardedInterstitialAd) {
            rewardedInterstitialAd = ad
            ad.adEventCallback = rewardedInterstitialAdEventCallback
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedInterstitialAd = null
        }
    },
)

// Later, to show and grant the reward reported by Google:
rewardedInterstitialAd?.show(this) { rewardItem ->
    grantReward(rewardItem)
}
```

**With RevenueCat tracking**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

Purchases.sharedInstance.adTracker.loadAndTrackRewardedInterstitialAd(
    adRequest = adRequest,
    placement = "level_complete_reward",
    loadCallback = object : AdLoadCallback<RewardedInterstitialAd> {
        override fun onAdLoaded(ad: RewardedInterstitialAd) {
            rewardedInterstitialAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedInterstitialAd = null
        }
    },
    adEventCallback = rewardedInterstitialAdEventCallback,
)

// Later, optionally override the load-time placement while preserving Google's reward callback:
rewardedInterstitialAd?.show(this, placement = "bonus_chest") { rewardItem ->
    grantReward(rewardItem)
}
```

Reward verification works the same way as for rewarded ads: call `enableRewardVerification()` on the loaded or
polled `RewardedInterstitialAd`, then use its verification-aware `show` overload:

```kotlin
rewardedInterstitialAd?.show(
    activity = this,
    placement = "bonus_chest",
    rewardVerificationStarted = { /* Verification polling has started. */ },
    rewardVerificationCompleted = { result -> /* Handle the verification result. */ },
)
```

### App open ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

AppOpenAd.load(
    adRequest,
    object : AdLoadCallback<AppOpenAd> {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
            ad.adEventCallback = appOpenAdEventCallback
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            appOpenAd = null
        }
    },
)

// Later, to show:
appOpenAd?.show(this)
```

**With RevenueCat tracking**

```kotlin
val adRequest = AdRequest.Builder("AD_UNIT_ID").build()

Purchases.sharedInstance.adTracker.loadAndTrackAppOpenAd(
    adRequest = adRequest,
    placement = "app_start",
    loadCallback = object : AdLoadCallback<AppOpenAd> {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            appOpenAd = null
        }
    },
    adEventCallback = appOpenAdEventCallback,
)

// Later, optionally override the load-time placement:
appOpenAd?.show(this, placement = "resume_from_background")
```

### Native ads

**Google Mobile Ads Next-Gen only**

```kotlin
val adRequest = NativeAdRequest.Builder(
    "AD_UNIT_ID",
    listOf(NativeAd.NativeAdType.NATIVE),
).build()

NativeAdLoader.load(
    adRequest,
    object : NativeAdLoaderCallback {
        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            loadedNativeAd = nativeAd
            nativeAd.adEventCallback = nativeAdEventCallback
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadedNativeAd = null
        }
    },
)
```

**With RevenueCat tracking**

```kotlin
Purchases.sharedInstance.adTracker.loadAndTrackNativeAd(
    adRequest = adRequest,
    placement = "home_feed",
    loadCallback = object : NativeAdLoaderCallback {
        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            loadedNativeAd = nativeAd
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadedNativeAd = null
        }
    },
    nativeAdEventCallback = nativeAdEventCallback,
)
```

A `NativeAdRequest` can combine standard native, custom-native, and banner inventory. RevenueCat tracks each result
using its actual format and installs the corresponding event wrapper before forwarding it. Use
`nativeAdEventCallback` for native and custom-native results and `bannerAdEventCallback` for banner results.

## Placement

Every tracking helper accepts an optional `placement` that identifies the logical location of an ad in your app, such
as `"home_banner"`, `"app_start"`, or `"level_complete_reward"`. Use stable names so events from the same slot can
be grouped together in RevenueCat.

Placement ownership depends on the loading stage:

| Stage | Placement applies to |
| --- | --- |
| Direct load | Load success or failure and, by default, the ad's later lifecycle events |
| `startAndTrack` | Preload success or failure only |
| `pollAndTrackAd` | The polled ad's display, click, revenue, and banner-refresh events |
| Tracked `show(activity, placement, ...)` | Overrides the polled or load-time placement for full-screen lifecycle events |

The placements passed to `startAndTrack` and `pollAndTrackAd` are independent. Polling does not emit another loaded
event. Calling Google's regular `show` retains the load-time or poll-time placement; passing a placement to the
adapter's tracked `show` overload replaces it. Passing `null` explicitly clears it.

Banner and native ads do not have a placement-aware `show` call. They keep the placement supplied when loaded or
polled for their complete lifecycle, including banner refreshes.

## Callback handling

The adapter installs tracking wrappers before returning a loaded or polled ad. Pass your event callbacks to the
RevenueCat load or poll helper instead of assigning the ad's callback property afterward. Direct assignment replaces
the tracking wrapper.

Every supported ad type exposes the same tracking-safe event callback setter:

```kotlin
ad.setTrackingAdEventCallback(adEventCallback)
```

Banners also expose `setTrackingBannerAdRefreshCallback` for their refresh callback.

All Google callbacks are forwarded after RevenueCat performs its own tracking. Forwarding still occurs if RevenueCat
is not configured or tracking fails.

Google Mobile Ads Next-Gen can invoke load and event callbacks on a background thread. Dispatch to the main thread
before updating views or other UI-confined state. RevenueCat's `rewardVerificationStarted` and
`rewardVerificationCompleted` callbacks are delivered on the main thread.

## Further guides

- [Advanced loading](docs/advanced-loading.md): suspending loads, server-to-server responses, native batch loading,
  and preloading.
- [Migrate from the legacy adapter](docs/migration.md): dependency, initialization, request, and callback changes.
