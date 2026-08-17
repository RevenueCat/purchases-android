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

## Events tracked

All formats — banner, interstitial, rewarded, rewarded interstitial, app open, and native — report these
RevenueCat ad events:

- **Ad Loaded** — the ad loaded successfully. Banner auto-refresh reports each refreshed creative as a new load.
- **Ad Failed to Load** — the load failed, tagged with the SDK's numeric error code. Banner refresh failures
  report here too.
- **Ad Displayed** — the ad was shown. Which callback triggers this depends on the format, see below.
- **Ad Opened** — the user clicked the ad.
- **Ad Revenue** — revenue from the SDK's paid callback, with currency, micros, and the reported precision
  mapped to the RevenueCat equivalent.

Every event carries the ad format, the ad unit, and the optional `placement`. Ad Loaded, Ad Displayed, Ad Opened
and Ad Revenue also carry the mediation network and the impression id; Ad Failed to Load carries the error code
instead, since no creative was served.

Network and impression id are read when the callback fires rather than when the ad loads, so an auto-refreshing
banner attributes events to the creative currently on screen instead of the first one loaded. `placement` is read
at the same moment, so if it changes before the ad is shown, the newer value is the one reported.

### Display triggers

| Format                                                  | Trigger                          |
| ------------------------------------------------------- | -------------------------------- |
| Banner, native                                          | `onAdImpression()`               |
| Interstitial, rewarded, rewarded interstitial, app open | `onAdShowedFullScreenContent()`  |

Full-screen formats emit both callbacks, so tracking the display from the show callback is what keeps a single
display from being counted twice. Banner and native never emit the show callback. The adapter keeps no
deduplication state.

### Callback forwarding

The adapter wraps your callbacks instead of replacing them. Every callback the SDK exposes is forwarded to your own
callback after the RevenueCat event is tracked — including the event callbacks the adapter does not track:
`onAdDismissedFullScreenContent`, `onAdFailedToShowFullScreenContent`, `onAppEvent` (banner, interstitial),
`onAdMetadataChanged` (rewarded, rewarded interstitial), and `onAdSwipeGestureClicked` /
`onCustomMuteThisAdReported` (native).

Forwarding is unconditional. If RevenueCat is not configured the adapter logs a warning and skips tracking, and if
tracking itself fails the adapter logs the error and swallows it. Either way your callback still runs.
