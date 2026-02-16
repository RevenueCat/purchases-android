# RevenueCat AdMob Adapter

Wraps AdMob ad lifecycle callbacks to automatically track ad events in RevenueCat. Drop-in replacement for standard AdMob loading calls — add one method call and RevenueCat tracks loaded, displayed, opened, revenue, and failed-to-load events for you.

## Usage

### Banner ads

**AdMob only** ([docs](https://developers.google.com/admob/android/banner)):

```kotlin
val adView = AdView(this)
adView.adUnitId = "AD_UNIT_ID"
adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360))
binding.adViewContainer.addView(adView)

adView.loadAd(AdRequest.Builder().build())
```

**With RevenueCat tracking:**

```kotlin
val adView = AdView(this)
adView.adUnitId = "AD_UNIT_ID"
adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360))
binding.adViewContainer.addView(adView)

adView.loadAndTrackAd(
    adRequest = AdRequest.Builder().build(),
    placement = "home_banner",
)
```

Or using the static method (also works from Java):

```kotlin
RCAdMob.loadAndTrackBannerAd(
    adView = adView,
    adRequest = AdRequest.Builder().build(),
    placement = "home_banner",
)
```

### Interstitial ads

**AdMob only** ([docs](https://developers.google.com/admob/android/interstitial)):

```kotlin
InterstitialAd.load(
    this,
    "AD_UNIT_ID",
    AdRequest.Builder().build(),
    object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
        }
        override fun onAdFailedToLoad(adError: LoadAdError) {
            interstitialAd = null
        }
    },
)

// Later, to show:
interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
    override fun onAdDismissedFullScreenContent() { interstitialAd = null }
}
interstitialAd?.show(this)
```

**With RevenueCat tracking:**

```kotlin
RCAdMob.loadAndTrackInterstitialAd(
    context = this,
    adUnitId = "AD_UNIT_ID",
    adRequest = AdRequest.Builder().build(),
    placement = "level_complete",
    loadCallback = object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialAd = ad
        }
        override fun onAdFailedToLoad(adError: LoadAdError) {
            interstitialAd = null
        }
    },
    fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() { interstitialAd = null }
    },
)

// Later, to show:
interstitialAd?.show(this)
```

### Rewarded ads

**AdMob only** ([docs](https://developers.google.com/admob/android/rewarded)):

```kotlin
RewardedAd.load(
    this,
    "AD_UNIT_ID",
    AdRequest.Builder().build(),
    object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) {
            rewardedAd = ad
        }
        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedAd = null
        }
    },
)

// Later, to show:
rewardedAd?.show(this) { rewardItem ->
    val rewardAmount = rewardItem.amount
    val rewardType = rewardItem.type
}
```

**With RevenueCat tracking:**

```kotlin
RCAdMob.loadAndTrackRewardedAd(
    context = this,
    adUnitId = "AD_UNIT_ID",
    adRequest = AdRequest.Builder().build(),
    placement = "bonus_coins",
    loadCallback = object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) {
            rewardedAd = ad
        }
        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedAd = null
        }
    },
)

// Later, to show (unchanged):
rewardedAd?.show(this) { rewardItem ->
    val rewardAmount = rewardItem.amount
    val rewardType = rewardItem.type
}
```

### Rewarded interstitial ads

**AdMob only** ([docs](https://developers.google.com/admob/android/rewarded-interstitial)):

```kotlin
RewardedInterstitialAd.load(
    this,
    "AD_UNIT_ID",
    AdRequest.Builder().build(),
    object : RewardedInterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedInterstitialAd) {
            rewardedInterstitialAd = ad
        }
        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedInterstitialAd = null
        }
    },
)

// Later, to show:
rewardedInterstitialAd?.show(this) { rewardItem ->
    val rewardAmount = rewardItem.amount
    val rewardType = rewardItem.type
}
```

**With RevenueCat tracking:**

```kotlin
RCAdMob.loadAndTrackRewardedInterstitialAd(
    context = this,
    adUnitId = "AD_UNIT_ID",
    adRequest = AdRequest.Builder().build(),
    placement = "between_levels",
    loadCallback = object : RewardedInterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedInterstitialAd) {
            rewardedInterstitialAd = ad
        }
        override fun onAdFailedToLoad(adError: LoadAdError) {
            rewardedInterstitialAd = null
        }
    },
)

// Later, to show (unchanged):
rewardedInterstitialAd?.show(this) { rewardItem ->
    val rewardAmount = rewardItem.amount
    val rewardType = rewardItem.type
}
```

### App open ads

**AdMob only** ([docs](https://developers.google.com/admob/android/app-open)):

```kotlin
AppOpenAd.load(
    context,
    "AD_UNIT_ID",
    AdRequest.Builder().build(),
    object : AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
        }
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            // Handle error.
        }
    },
)

// Later, to show:
appOpenAd?.show(activity)
```

**With RevenueCat tracking:**

```kotlin
RCAdMob.loadAndTrackAppOpenAd(
    context = context,
    adUnitId = "AD_UNIT_ID",
    adRequest = AdRequest.Builder().build(),
    placement = "app_launch",
    loadCallback = object : AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
        }
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            // Handle error.
        }
    },
)

// Later, to show (unchanged):
appOpenAd?.show(activity)
```

### Native ads

**AdMob only** ([docs](https://developers.google.com/admob/android/native/start)):

```kotlin
val adLoader = AdLoader.Builder(context, "AD_UNIT_ID")
    .forNativeAd { nativeAd ->
        // Show the ad.
    }
    .withAdListener(object : AdListener() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            // Handle error.
        }
    })
    .withNativeAdOptions(NativeAdOptions.Builder().build())
    .build()

adLoader.loadAd(AdRequest.Builder().build())
```

**With RevenueCat tracking** — swap `forNativeAd` for `forNativeAdWithTracking`:

```kotlin
val adLoader = AdLoader.Builder(context, "AD_UNIT_ID")
    .forNativeAdWithTracking(
        adUnitId = "AD_UNIT_ID",
        placement = "feed",
        adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Handle error.
            }
        },
    ) { nativeAd ->
        // Show the ad.
    }
    .withNativeAdOptions(NativeAdOptions.Builder().build())
    .build()

adLoader.loadAd(AdRequest.Builder().build())
```

Or using the static method (also works from Java):

```kotlin
RCAdMob.loadAndTrackNativeAd(
    context = context,
    adUnitId = "AD_UNIT_ID",
    adRequest = AdRequest.Builder().build(),
    placement = "feed",
    nativeAdOptions = NativeAdOptions.Builder().build(),
    adListener = object : AdListener() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            // Handle error.
        }
    },
    onAdLoaded = { nativeAd ->
        // Show the ad.
    },
)
```

## Java

All methods are `@JvmStatic` with `@JvmOverloads`, so they work the same way from Java:

```java
RCAdMob.loadAndTrackInterstitialAd(
    context,
    "AD_UNIT_ID",
    new AdRequest.Builder().build(),
    "level_complete",
    new InterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(@NonNull InterstitialAd ad) {
            interstitialAd = ad;
        }
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            interstitialAd = null;
        }
    }
);
```

Optional parameters can be omitted — overloads are generated automatically.

## Supported ad formats

| Format | Method |
|--------|--------|
| Banner | `AdView.loadAndTrackAd()` / `RCAdMob.loadAndTrackBannerAd()` |
| Interstitial | `RCAdMob.loadAndTrackInterstitialAd()` |
| Rewarded | `RCAdMob.loadAndTrackRewardedAd()` |
| Rewarded Interstitial | `RCAdMob.loadAndTrackRewardedInterstitialAd()` |
| App Open | `RCAdMob.loadAndTrackAppOpenAd()` |
| Native | `AdLoader.Builder.forNativeAdWithTracking()` / `RCAdMob.loadAndTrackNativeAd()` |

## Events tracked

All formats automatically report these RevenueCat ad events:

- **Ad Loaded** — ad successfully loaded
- **Ad Displayed** — impression recorded
- **Ad Opened** — user clicked/interacted
- **Ad Revenue** — revenue reported via AdMob's `OnPaidEventListener`
- **Ad Failed to Load** — load error

## Experimental API

This library uses `@ExperimentalPreviewRevenueCatPurchasesAPI`. Opt in at the module level:

```kotlin
// build.gradle.kts
kotlinOptions {
    freeCompilerArgs += "-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI"
}
```
