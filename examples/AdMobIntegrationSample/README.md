# AdMob Integration Sample - RevenueCat Ad Event Tracking

This sample app demonstrates how to integrate Google AdMob with RevenueCat's ad event tracking API to monitor ad performance and revenue.

## Overview

This example shows how to track **all 5 RevenueCat ad events** across five different AdMob ad formats:

### Ad Events Tracked
1. ✅ **Ad Loaded** - When an ad successfully loads
2. ✅ **Ad Displayed** - When an ad is shown to the user (impression)
3. ✅ **Ad Opened** - When a user clicks/interacts with the ad
4. ✅ **Ad Revenue** - When an ad generates revenue (via AdMob's `OnPaidEventListener`)
5. ✅ **Ad Failed to Load** - When an ad fails to load (includes error codes)

### Ad Formats Demonstrated
- **Banner Ads** - Always visible at the top of the screen
- **Interstitial Ads** - Full-screen ads triggered by user action
- **App Open Ads** - Full-screen ads designed for app launch/resume scenarios
- **Native Ads** - Custom-styled ads with text and images integrated into the app's UI
- **Native Video Ads** - Custom-styled ads with video content integrated into the app's UI

---

## 🚨 Important Notice: Experimental API

**This sample uses RevenueCat's `@ExperimentalPreviewRevenueCatPurchasesAPI` for ad tracking.**

⚠️ **What this means:**
- This is an experimental API that may change without warning
- No compatibility guarantees are provided
- Designed for internal RevenueCat use and preview features

### Required Setup

To use this API, you need to opt-in both at the code and build level:

**1. Code-level opt-in** (already done in `AdMobManager.kt`):
```kotlin
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class AdMobManager(private val context: Context) {
    // ...
}
```

**2. Module-level opt-in** (in `app/build.gradle.kts`):
```kotlin
kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs += listOf(
        "-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI"
    )
}
```

This suppresses the "internal RevenueCat API" warnings during compilation.

---

## Requirements

- **Android Studio** Hedgehog or later
- **Android API 26+** (Android 8.0 Oreo or higher)
- **Kotlin 2.0.21**
- **RevenueCat SDK 9.19.4** or later
- **Google Mobile Ads SDK 24.9.0**

---

## Features & Code Examples

| Feature | Sample Project Location |
|---------|------------------------|
| 🔧 RevenueCat SDK initialization | [MainApplication.kt:36](app/src/main/java/com/revenuecat/sample/admob/MainApplication.kt#L36) |
| 🔧 AdMob SDK initialization | [MainApplication.kt:60](app/src/main/java/com/revenuecat/sample/admob/MainApplication.kt#L60) |
| 📊 Banner ad tracking | [AdMobManager.kt:72](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L72) |
| 📊 Interstitial ad tracking | [AdMobManager.kt:135](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L135) |
| 📊 App Open ad tracking | [AdMobManager.kt:237](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L237) |
| 📊 Native ad tracking | [AdMobManager.kt:336](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L336) |
| 💰 Ad revenue tracking (OnPaidEventListener) | [AdMobManager.kt:525](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L525) |
| ❌ Ad failure tracking | [AdMobManager.kt:557](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L557) |
| 🎨 Compose UI integration | [HomeScreen.kt](app/src/main/java/com/revenuecat/sample/admob/ui/HomeScreen.kt) |

---

## Setup & Run

### 1. Prerequisites

Before running the sample:

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

## AdMob Ad Unit IDs

This sample uses **Google's official test ad unit IDs**:

| Ad Format | Ad Unit ID | Usage | Status |
|-----------|----------------|-------|--------|
| **Banner** | `ca-app-pub-3940256099942544/9214589741` | Google's test banner ad | ✅ Working |
| **Interstitial** | `ca-app-pub-3940256099942544/1033173712` | Google's test interstitial ad | ✅ Working |
| **App Open** | `ca-app-pub-3940256099942544/9257395921` | Google's test app open ad | ✅ Working |
| **Native** | `ca-app-pub-3940256099942544/2247696110` | Google's test native ad (text + images) | ⚠️ Unreliable |
| **Native Video** | `ca-app-pub-3940256099942544/1044960115` | Google's test native video ad | ⚠️ Unreliable |
| **Error Testing** | `"invalid-ad-unit-id"` | Triggers load failures for error handling demo | ✅ Working |

### About These Ad Units

✅ **Official test IDs** - Provided by Google for development and testing
✅ **Always serve test ads** - No risk of affecting production metrics
⚠️ **Native ad limitation** - Test IDs for native ads don't work reliably (see below)

### ⚠️ Important: Native Ads and Test Ad Unit IDs

**Google's official test ad unit IDs do not work reliably with native ads.**

While Google provides test ad unit IDs for banner and interstitial ads (like `ca-app-pub-3940256099942544/9214589741`), the official test IDs for native ads often fail to load or behave inconsistently.

**Recommended approach for testing native ads:**
1. Create production ad units in your AdMob account
2. Update the AdMob app ID in `AndroidManifest.xml` (line 18-19) with your production app ID
3. Update the ad unit IDs in `Constants.kt` with your production ad unit IDs
4. Configure your test device in AdMob settings (or use an emulator which is automatically treated as a test device)

This ensures you receive test ads (no real impressions) while having reliable ad loading behavior during development.

### Setting Up Your Own Ad Units

To use your own AdMob ad units:
1. Create a free AdMob account at [admob.google.com](https://admob.google.com)
2. Create ad units for each format you want to test
3. Replace the ad unit IDs in `Constants.kt`
4. Configure your test device in AdMob settings to receive test ads without affecting metrics

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
            adFormat = AdFormat.BANNER,
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

See [AdMobManager.kt:587](app/src/main/java/com/revenuecat/sample/admob/manager/AdMobManager.kt#L587) for implementation.

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

**App Open Ads:**
- `Loaded`: `AppOpenAd.AppOpenAdLoadCallback.onAdLoaded()`
- `Displayed`: `FullScreenContentCallback.onAdShowedFullScreenContent()`
- `Opened`: `FullScreenContentCallback.onAdClicked()`
- `Revenue`: `OnPaidEventListener`

**Native Ads (both regular and video):**
- `Loaded`: `AdLoader.forNativeAd()` callback
- `Displayed`: **Manual** - Call `trackNativeAdDisplayed()` after rendering
- `Opened`: `AdListener.onAdClicked()` callback
- `Revenue`: `setOnPaidEventListener()`

**Note:** Native and native video ads use the same tracking mechanisms. The only difference is the ad unit ID used and the content returned (with or without video). App Open ads follow the same pattern as Interstitial ads but are designed for app launch/resume scenarios.

---


## Common Issues & Troubleshooting

### Issue: "Missing RevenueCat API key" or SDK initialization fails

**Solution:** Make sure you've updated `Constants.kt` with your actual RevenueCat API key from the [RevenueCat Dashboard](https://app.revenuecat.com/).

### Issue: Ads not loading

**Possible causes:**
1. **No internet connection** - Ensure device/emulator has internet access
2. **AdMob SDK still initializing** - Wait a few seconds after app launch
3. **Test device not configured** - Emulators are automatically test devices; real devices may take 15 minutes to 24 hours to be recognized

### Issue: Native ads not loading with test ad unit IDs

**Cause:** Google's official test ad unit IDs for native ads (e.g., `ca-app-pub-3940256099942544/2247696110`) do not work reliably. They often fail to load or behave inconsistently.

**Solution:** Use production ad unit IDs configured with test devices:
1. Create native and native video ad units in your [AdMob account](https://admob.google.com)
2. Update the AdMob app ID in `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
   ```
3. Replace the test IDs in `Constants.kt` with your production ad unit IDs
4. Configure your device as a test device in AdMob settings (emulators are automatically test devices)
5. You'll receive test ads without affecting your production metrics

This limitation only affects native and native video ad formats. Banner and interstitial test IDs work as expected.

### Issue: "Invalid request" error on real device

**Solution:** Real devices might not be registered as test devices yet. Either:
- Wait up to 24 hours for AdMob to recognize your device as a test device
- Add your device as a test device in AdMob settings

### Issue: Not seeing revenue events

**Cause:** AdMob test ads may not always trigger `OnPaidEventListener` events.

**Note:** Revenue tracking works reliably in production with real ads. Test ads may have inconsistent revenue event behavior.

### Issue: Build errors or warnings about `@ExperimentalPreviewRevenueCatPurchasesAPI`

**Solution:** Make sure you have:
1. The latest RevenueCat SDK (9.19.4 or later)
2. Both code-level and module-level opt-in configured

See the [Required Setup](#required-setup) section above for detailed instructions on configuring the opt-in.

---

## License

This sample app is part of the RevenueCat SDK and follows the same license terms.
