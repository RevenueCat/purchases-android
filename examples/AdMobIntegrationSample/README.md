# AdMob Integration Sample - RevenueCat Ad Event Tracking

This sample app demonstrates how to integrate Google AdMob with RevenueCat's ad event tracking API to monitor ad performance and revenue.

## Overview

This example shows how to track **all 5 RevenueCat ad events** across three different AdMob ad formats:

### Ad Events Tracked
1. ✅ **Ad Loaded** - When an ad successfully loads
2. ✅ **Ad Displayed** - When an ad is shown to the user (impression)
3. ✅ **Ad Opened** - When a user clicks/interacts with the ad
4. ✅ **Ad Revenue** - When an ad generates revenue (via AdMob's `OnPaidEventListener`)
5. ✅ **Ad Failed to Load** - When an ad fails to load (includes error codes)

### Ad Formats Demonstrated
- **Banner Ads** - Always visible at the top of the screen
- **Interstitial Ads** - Full-screen ads triggered by user action
- **Native Video Ads** - Custom-styled ads with video content integrated into the app's UI

---

## 🚨 Important Notice: Internal API

**This sample uses RevenueCat's `@InternalRevenueCatAPI` for ad tracking.**

⚠️ **What this means:**
- Requires `@OptIn(InternalRevenueCatAPI::class)` annotation
- Designed for internal RevenueCat use and experimental features

---

## Requirements

- **Android Studio** Hedgehog or later
- **Android API 26+** (Android 8.0 Oreo or higher)
- **Kotlin 2.0.21**
- **RevenueCat SDK 9.17.0-SNAPSHOT** or later
- **Google Mobile Ads SDK 24.9.0**

---

## Features & Code Examples

| Feature | Sample Project Location |
|---------|------------------------|
| 🔧 RevenueCat SDK initialization | [MainApplication.kt:29](app/src/main/java/com/revenuecat/sample/admob/MainApplication.kt#L29) |
| 🔧 AdMob SDK initialization | [MainApplication.kt:44](app/src/main/java/com/revenuecat/sample/admob/MainApplication.kt#L44) |
| 📊 Banner ad tracking | [AdMobManager.kt:47](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L47) |
| 📊 Interstitial ad tracking | [AdMobManager.kt:99](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L99) |
| 📊 Native ad tracking | [AdMobManager.kt:179](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L179) |
| 💰 Ad revenue tracking (OnPaidEventListener) | [AdMobManager.kt:360](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L360) |
| ❌ Ad failure tracking | [AdMobManager.kt:391](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L391) |
| 🎨 Compose UI integration | [HomeScreen.kt](app/src/main/java/com/revenuecat/sample/admob/ui/HomeScreen.kt) |

---

## Setup & Run

### 1. Prerequisites

Before running the sample:

1. **Get a RevenueCat API Key**
   - Sign up for a free account at [revenuecat.com](https://www.revenuecat.com)
   - Get your project API key from the [RevenueCat Dashboard](https://app.revenuecat.com/)

2. **AdMob Setup** (Optional for testing)
   - This sample uses **official AdMob test ad unit IDs** (see below)
   - No AdMob account needed to run the sample
   - For production use, create an [AdMob account](https://admob.google.com/) and replace test IDs

### 2. Configure the App

1. **Open the project in Android Studio**
   ```bash
   cd examples/AdMobIntegrationSample
   open -a "Android Studio" .
   ```

2. **Update your RevenueCat API key**

   Edit `app/src/main/java/com/revenuecat/sample/admob/data/Constants.kt`:
   ```kotlin
   const val REVENUECAT_API_KEY = "YOUR_REVENUECAT_API_KEY_HERE"
   ```

3. **Sync Gradle** and wait for dependencies to download

### 3. Run the App

1. Connect an Android device or start an emulator (API 26+)
2. Click **Run** in Android Studio
3. The app will install and launch

### 4. Monitor Ad Events

**View event tracking in Logcat:**
1. Open the **Logcat** panel in Android Studio
2. Filter by `AdMobManager` or `RevenueCat` to see ad events
3. Interact with the ads in the app to trigger events

**Example Logcat output:**
```
D/AdMobManager: Loading banner ad with unit ID: ca-app-pub-3940256099942544/9214589741
D/AdMobManager: Banner ad loaded successfully
D/AdMobManager: ✅ Tracked: Ad Loaded - placement=home_banner
D/AdMobManager: Banner ad displayed (impression recorded)
D/AdMobManager: ✅ Tracked: Ad Displayed - placement=home_banner
D/AdMobManager: ✅ Tracked: Ad Revenue - $0.00015 USD (precision: EXACT) - placement=home_banner
```

---

## AdMob Test Ad Unit IDs

This sample uses **official Google AdMob test ad unit IDs** that are safe for development:

| Ad Format | Test Ad Unit ID | Usage | Status |
|-----------|----------------|-------|--------|
| **Banner** | `ca-app-pub-3940256099942544/9214589741` | Always loads test banner ads | ✅ Working |
| **Interstitial** | `ca-app-pub-3940256099942544/1033173712` | Always loads test interstitial ads | ✅ Working |
| **Native Video** | `ca-app-pub-3940256099942544/1044960115` | Test native ads with video content | ⚠️ See note below |
| **App ID** | `ca-app-pub-3940256099942544~3347511713` | Test application ID (in manifest) | ✅ Working |
| **Error Testing** | `"invalid-ad-unit-id"` | Triggers load failures for error handling demo | ✅ Working |

### About Test Ads

✅ **Safe to use** - No risk of invalid traffic to your AdMob account
✅ **Always return test ads** - Consistent behavior for development
✅ **Labeled as "Test Ad"** - Easy to identify in the UI

### ⚠️ Known Limitation: Native Video Ad Test IDs

**Native video ad test unit IDs provided by Google have reliability issues:**
- Native video ad test ID: `ca-app-pub-3940256099942544/1044960115` - ❌ Fails with internal server error

-**To test native ads with working ads:**
-1. Create a free AdMob account at [admob.google.com](https://admob.google.com)
-2. Create your own native ad unit (takes ~2 minutes)
-3. Replace `NATIVE_AD_UNIT_ID` in `Constants.kt` with your ad unit ID
-4. Configure your test device in AdMob settings to receive test ads without affecting metrics

### Error Testing Note

AdMob does not provide an official "error trigger" test ad unit ID. This sample uses an invalid ID (`"invalid-ad-unit-id"`) to simulate load failures and demonstrate error tracking with RevenueCat.

---

## How It Works

### Integration Architecture

```
┌─────────────┐
│   AdMob SDK │
│  (Load Ads) │
└──────┬──────┘
       │
       │ Ad Events (OnPaidEventListener, AdListener)
       ▼
┌──────────────────┐
│  AdMobManager    │
│  (Event Mapper)  │
└──────┬───────────┘
       │
       │ Mapped Events
       ▼
┌────────────────────────┐
│  RevenueCat AdTracker  │
│  (Track Analytics)     │
└────────────────────────┘
```

### Key Integration Points

#### 1. **Ad Revenue Tracking**

AdMob provides revenue data via `OnPaidEventListener`:

```kotlin
adView.onPaidEventListener = OnPaidEventListener { adValue ->
    Purchases.sharedInstance.adTracker.trackAdRevenue(
        data = AdRevenueData(
            networkName = adView.responseInfo?.mediationAdapterClassName ?: "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            placement = "banner_home",
            adUnitId = "ca-app-pub-xxxxx",
            impressionId = adView.responseInfo?.responseId ?: "",
            revenueMicros = adValue.valueMicros,  // Already in micros
            currency = adValue.currencyCode,
            precision = mapPrecision(adValue.precisionType)
        )
    )
}
```

**Important:** AdMob provides revenue in **micros** (1/1,000,000 of currency unit), which matches RevenueCat's expected format.

#### 2. **Precision Type Mapping**

AdMob precision types are mapped to RevenueCat types:

| AdMob PrecisionType | RevenueCat AdRevenuePrecision | Meaning |
|---------------------|-------------------------------|---------|
| `PRECISE` (0) | `EXACT` | Publisher is paid for this impression |
| `ESTIMATED` (1) | `ESTIMATED` | Estimate; publisher might not be paid |
| `PUBLISHER_PROVIDED` (2) | `PUBLISHER_DEFINED` | Value provided by publisher |
| `UNKNOWN` (3) | `UNKNOWN` | Precision unknown |

See [AdMobManager.kt:425](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L425) for implementation.

#### 3. **Event Timing**

Different ad formats track events at different times:

**Banner Ads:**
- `Loaded`: `onAdLoaded()` callback
- `Displayed`: `onAdImpression()` callback (automatic)
- `Opened`: `onAdClicked()` callback
- `Revenue`: `OnPaidEventListener`

**Interstitial Ads:**
- `Loaded`: `InterstitialAdLoadCallback.onAdLoaded()`
- `Displayed`: `FullScreenContentCallback.onAdShowedFullScreenContent()`
- `Opened`: `FullScreenContentCallback.onAdClicked()`
- `Revenue`: `OnPaidEventListener`

**Native Ads:**
- `Loaded`: `AdLoader.forNativeAd()` callback
- `Displayed`: **Manual** - Call `trackNativeAdDisplayed()` after rendering
- `Opened`: `setOnNativeAdClickedListener()`
- `Revenue`: `setOnPaidEventListener()`

---


## Common Issues & Troubleshooting

### Issue: "Missing RevenueCat API key" or SDK initialization fails

**Solution:** Make sure you've updated `Constants.kt` with your actual RevenueCat API key from the [RevenueCat Dashboard](https://app.revenuecat.com/).

### Issue: Ads not loading

**Possible causes:**
1. **No internet connection** - Ensure device/emulator has internet access
2. **AdMob SDK still initializing** - Wait a few seconds after app launch
3. **Test device not configured** - Emulators are automatically test devices; real devices may take 15 minutes to 24 hours to be recognized

### Issue: "Invalid request" error on real device

**Solution:** Real devices might not be registered as test devices yet. Either:
- Wait up to 24 hours for AdMob to recognize your device as a test device
- Add your device as a test device in AdMob settings

### Issue: Not seeing revenue events

**Cause:** AdMob test ads may not always trigger `OnPaidEventListener` events.

**Note:** Revenue tracking works reliably in production with real ads. Test ads may have inconsistent revenue event behavior.

### Issue: Build errors about `@InternalRevenueCatAPI`

**Solution:** Make sure you have the latest RevenueCat SDK (9.17.0-SNAPSHOT or later) and the `@OptIn` annotation is present:
```kotlin
@OptIn(InternalRevenueCatAPI::class)
```

---

## License

This sample app is part of the RevenueCat SDK and follows the same license terms.
