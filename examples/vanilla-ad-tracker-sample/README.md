# AdMob Manual Integration Sample

This sample app demonstrates how to manually integrate Google AdMob with RevenueCat's ad event tracking **without** the `purchases-android-admob` adapter library. You call the AdMob SDK directly and call RevenueCat's `AdTracker` methods yourself in the right callbacks.

This is the right approach if you are not using the adapter (e.g. you are using a different ad network, or you prefer explicit control over when events fire).

If you are using AdMob and want less boilerplate, see the `admob-sample` which uses the adapter library instead.

## Overview

### Ad Events Tracked

For each ad format you are responsible for calling the right `AdTracker` method at the right time:

1. **Ad Loaded** (`trackAdLoaded`) - in the AdMob load callback (`onAdLoaded`)
2. **Ad Displayed** (`trackAdDisplayed`) - in `FullScreenContentCallback.onAdShowedFullScreenContent`, or immediately after attaching a banner/native ad to the view hierarchy
3. **Ad Opened** (`trackAdOpened`) - in `FullScreenContentCallback.onAdClicked`
4. **Ad Revenue** (`trackAdRevenue`) - in `OnPaidEventListener.onPaidEvent`
5. **Ad Failed to Load** (`trackAdFailedToLoad`) - in the AdMob load callback (`onAdFailedToLoad`)

### The Impression ID

RevenueCat uses an `impressionId` to correlate all events for a single ad impression. The AdMob SDK provides this via `responseInfo.responseId`. Capture it when the ad loads and reuse the same value for all subsequent events (displayed, opened, revenue) for that impression.

### Example: Interstitial Ad

```kotlin
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(),
    object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            val responseInfo = ad.responseInfo
            val adTracker = Purchases.sharedInstance.adTracker

            adTracker.trackAdLoaded(AdLoadedData(
                networkName = responseInfo.mediationAdapterClassName,
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "home_interstitial",
                adUnitId = adUnitId,
                impressionId = responseInfo.responseId.orEmpty(),
            ))

            ad.onPaidEventListener = { adValue ->
                adTracker.trackAdRevenue(AdRevenueData(
                    networkName = responseInfo.mediationAdapterClassName,
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = AdFormat.INTERSTITIAL,
                    placement = "home_interstitial",
                    adUnitId = adUnitId,
                    impressionId = responseInfo.responseId.orEmpty(),
                    revenueMicros = adValue.valueMicros,
                    currency = adValue.currencyCode,
                    precision = adValue.precisionType.toAdRevenuePrecision(),
                ))
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    adTracker.trackAdDisplayed(AdDisplayedData(
                        networkName = responseInfo.mediationAdapterClassName,
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = AdFormat.INTERSTITIAL,
                        placement = "home_interstitial",
                        adUnitId = adUnitId,
                        impressionId = responseInfo.responseId.orEmpty(),
                    ))
                }
                override fun onAdClicked() {
                    adTracker.trackAdOpened(AdOpenedData(
                        networkName = responseInfo.mediationAdapterClassName,
                        mediatorName = AdMediatorName.AD_MOB,
                        adFormat = AdFormat.INTERSTITIAL,
                        placement = "home_interstitial",
                        adUnitId = adUnitId,
                        impressionId = responseInfo.responseId.orEmpty(),
                    ))
                }
            }
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            Purchases.sharedInstance.adTracker.trackAdFailedToLoad(AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "home_interstitial",
                adUnitId = adUnitId,
                mediatorErrorCode = error.code,
            ))
        }
    }
)
```

### How It Works

```
┌─────────────┐
│   AdMob SDK │
│  (Load Ads) │
└──────┬──────┘
       │
       │ Ad callbacks (onAdLoaded, FullScreenContentCallback,
       │               OnPaidEventListener, onAdFailedToLoad)
       ▼
┌──────────────────┐
│   Your App Code  │
│                  │
│  Calls manually: │
│  trackAdLoaded   │
│  trackAdDisplayed│
│  trackAdOpened   │
│  trackAdRevenue  │
│  trackAdFailed...|
└──────┬───────────┘
       │
       │ RevenueCat events
       ▼
┌────────────────────────┐
│  RevenueCat Dashboard  │
│  (Analytics)           │
└────────────────────────┘
```

### Ad Formats Demonstrated

- **Banner Ads** - Always visible, auto-loaded
- **Interstitial Ads** - Full-screen ads triggered by user action
- **App Open Ads** - Full-screen ads designed for app launch/resume scenarios
- **Rewarded Ads** - Full-screen ads that reward users after viewing
- **Rewarded Interstitial Ads** - Interstitial ads that reward users
- **Native Ads** - Custom-styled ads with text and images integrated into the app's UI
- **Native Video Ads** - Custom-styled ads with video content integrated into the app's UI

### Key Files

- `ui/HomeScreen.kt` - Navigation state machine and `AdFormat` enum
- `ui/AdFormatListScreen.kt` - Card menu with one item per ad format
- `ui/AdFormatDetailScreen.kt` - Detail screen with Load/Show buttons per format
- `ui/ads/` - Per-format composables with manual `AdTracker` calls
- `ui/ads/AdRevenuePrecisionMapping.kt` - Maps AdMob `PrecisionType` to `AdRevenuePrecision`
- `MainApplication.kt` - RevenueCat and AdMob SDK initialization
- `data/Constants.kt` - Ad unit IDs and configuration

---

## Experimental API Notice

This sample uses RevenueCat's `@ExperimentalPreviewRevenueCatPurchasesAPI` for ad tracking. This API may change without warning and no compatibility guarantees are provided.

To use it, opt-in at the call site with `@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)` or at the module level in `build.gradle.kts`:

```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI"
    )
}
```

---

## Setup & Run

### 1. Prerequisites

1. **Get a RevenueCat API Key**
   - Sign up for a free account at [revenuecat.com](https://www.revenuecat.com)
   - Get your project API key from the [RevenueCat Dashboard](https://app.revenuecat.com/)

2. **AdMob Setup** (Optional for testing)
   - This sample uses **Google's official test ad unit IDs** (see below)
   - No AdMob account needed to run the sample as-is
   - For production use, create an [AdMob account](https://admob.google.com/) and replace with your own ad unit IDs

### 2. Configure the App

1. **Open the project in Android Studio**

2. **Update your RevenueCat API key**

   Add your key to `local.properties` (this file is gitignored):
   ```properties
   REVENUECAT_API_KEY=your_api_key_here
   ```

3. **Sync Gradle** and wait for dependencies to download

### 3. Run the App

1. Connect an Android device or start an emulator (API 26+)
2. Click **Run** in Android Studio
3. The app will install and launch

### 4. Verify Ad Events

Interact with the ads in the app, then background the app. The RevenueCat SDK flushes events when the app goes to the background, and they will be visible shortly after in the [RevenueCat Dashboard](https://app.revenuecat.com/).

---

## AdMob Test Ad Unit IDs

This sample uses **Google's official test ad unit IDs**:

| Ad Format | Ad Unit ID | Status |
|-----------|------------|--------|
| **Banner** | `ca-app-pub-3940256099942544/9214589741` | Working |
| **Interstitial** | `ca-app-pub-3940256099942544/1033173712` | Working |
| **App Open** | `ca-app-pub-3940256099942544/9257395921` | Working |
| **Rewarded** | `ca-app-pub-3940256099942544/5224354917` | Working |
| **Rewarded Interstitial** | `ca-app-pub-3940256099942544/5354046379` | Working |
| **Native** | `ca-app-pub-3940256099942544/2247696110` | Unreliable |
| **Native Video** | `ca-app-pub-3940256099942544/1044960115` | Unreliable |
| **Error Testing** | `"invalid-ad-unit-id"` | Working |

These are official Google test IDs — they always serve test ads with no risk of affecting production metrics.

### Native Ads and Test Ad Unit IDs

Google's test ad unit IDs for native ads often fail to load or behave inconsistently. To test native ads reliably:

1. Create ad units in your [AdMob account](https://admob.google.com)
2. Update the AdMob app ID in `AndroidManifest.xml` with your production app ID
3. Replace the ad unit IDs in `Constants.kt`
4. Configure your test device in AdMob settings (emulators are automatically test devices)

### Error Testing

AdMob does not provide an official "error trigger" test ad unit ID. This sample uses an invalid ID (`"invalid-ad-unit-id"`) to simulate load failures and demonstrate error tracking.

---

## Troubleshooting

### "Missing RevenueCat API key" or SDK initialization fails

Make sure you've added your RevenueCat API key to `local.properties`. Get your key from the [RevenueCat Dashboard](https://app.revenuecat.com/).

### Ads not loading

1. **No internet connection** - Ensure device/emulator has internet access
2. **AdMob SDK still initializing** - Wait a few seconds after app launch
3. **Test device not configured** - Emulators are automatically test devices; real devices may take 15 minutes to 24 hours to be recognized

### "Invalid request" error on real device

Real devices might not be registered as test devices yet. Either wait up to 24 hours for AdMob to recognize your device, or add it as a test device in AdMob settings.

### Not seeing revenue events

AdMob test ads may not always trigger `OnPaidEventListener` events. Revenue tracking works reliably in production with real ads.

### Build errors about `@ExperimentalPreviewRevenueCatPurchasesAPI`

Make sure you have the latest RevenueCat SDK (9.19.4 or later) and add `@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)` at each call site or configure the module-level opt-in in `build.gradle.kts`.

---

## License

This sample app is part of the RevenueCat SDK and follows the same license terms.
