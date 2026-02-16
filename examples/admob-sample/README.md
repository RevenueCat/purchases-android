# AdMob Integration Sample

This sample app demonstrates how to integrate Google AdMob with RevenueCat's ad event tracking using the `purchases-android-admob` adapter library. The adapter handles all event mapping automatically — you load ads through it and RevenueCat events are tracked for you.

## Overview

### Ad Events Tracked

The adapter library automatically tracks these RevenueCat ad events for all ad formats:

1. **Ad Loaded** - When an ad successfully loads
2. **Ad Displayed** - When an ad is shown to the user (impression)
3. **Ad Opened** - When a user clicks/interacts with the ad
4. **Ad Revenue** - When an ad generates revenue (via AdMob's `OnPaidEventListener`)
5. **Ad Failed to Load** - When an ad fails to load

### Ad Formats Demonstrated

- **Banner Ads** - Always visible, auto-loaded
- **Interstitial Ads** - Full-screen ads triggered by user action
- **App Open Ads** - Full-screen ads designed for app launch/resume scenarios
- **Rewarded Ads** - Full-screen ads that reward users after viewing
- **Rewarded Interstitial Ads** - Interstitial ads that reward users
- **Native Ads** - Custom-styled ads with text and images integrated into the app's UI
- **Native Video Ads** - Custom-styled ads with video content integrated into the app's UI

### Key Files

- `HomeScreen.kt` - Compose UI with list + detail navigation per ad format; calls `RCAdMob` directly
- `BannerAdView.kt` - Composable wrapper for banner ads using `adView.loadAndTrackAd(...)`
- `MainApplication.kt` - RevenueCat and AdMob SDK initialization
- `Constants.kt` - Ad unit IDs and configuration

---

## Experimental API Notice

This sample uses RevenueCat's `@ExperimentalPreviewRevenueCatPurchasesAPI` for ad tracking. This API may change without warning and no compatibility guarantees are provided.

To use it, opt-in at the module level in `build.gradle.kts`:

```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI"
    )
}
```

---

## Requirements

- **Android Studio** Hedgehog or later
- **Android API 26+** (Android 8.0 Oreo or higher)
- **Kotlin 2.0.21**
- **RevenueCat SDK 9.19.4** or later
- **Google Mobile Ads SDK 24.9.0**

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
   ```bash
   cd example
   open -a "Android Studio" .
   ```

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

## How It Works

The `purchases-android-admob` adapter library sits between AdMob and RevenueCat, automatically mapping AdMob callbacks to RevenueCat ad events:

```
┌─────────────┐
│   AdMob SDK │
│  (Load Ads) │
└──────┬──────┘
       │
       │ Ad callbacks
       ▼
┌───────────────────────────┐
│  purchases-android-admob  │
│  (Adapter Library)        │
│                           │
│  Automatically tracks:    │
│  Loaded, Displayed,       │
│  Opened, Revenue, Failed  │
└──────┬────────────────────┘
       │
       │ RevenueCat events
       ▼
┌────────────────────────┐
│  RevenueCat Dashboard  │
│  (Analytics)           │
└────────────────────────┘
```

The sample calls `RCAdMob.loadAndTrack*()` methods directly from Compose — no wrapper needed. The adapter handles all event mapping internally.

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

This ensures you receive test ads (no real impressions) while having reliable loading behavior.

### Error Testing

AdMob does not provide an official "error trigger" test ad unit ID. This sample uses an invalid ID (`"invalid-ad-unit-id"`) to simulate load failures and demonstrate error tracking.

---

## Troubleshooting

### "Missing RevenueCat API key" or SDK initialization fails

Make sure you've added your RevenueCat API key to `local.properties` (see [Running the Sample](#running-the-sample) step 2). Get your key from the [RevenueCat Dashboard](https://app.revenuecat.com/).

### Ads not loading

1. **No internet connection** - Ensure device/emulator has internet access
2. **AdMob SDK still initializing** - Wait a few seconds after app launch
3. **Test device not configured** - Emulators are automatically test devices; real devices may take 15 minutes to 24 hours to be recognized

### "Invalid request" error on real device

Real devices might not be registered as test devices yet. Either wait up to 24 hours for AdMob to recognize your device, or add it as a test device in AdMob settings.

### Not seeing revenue events

AdMob test ads may not always trigger `OnPaidEventListener` events. Revenue tracking works reliably in production with real ads.

### Build errors about `@ExperimentalPreviewRevenueCatPurchasesAPI`

Make sure you have the latest RevenueCat SDK (9.19.4 or later) and the module-level opt-in configured in `build.gradle.kts`. See the [Experimental API Notice](#experimental-api-notice) section.

---

## License

This sample app is part of the RevenueCat SDK and follows the same license terms.
