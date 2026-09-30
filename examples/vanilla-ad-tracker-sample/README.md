# LevelPlay Manual Integration Sample

This app demonstrates manual ad-event tracking with Unity LevelPlay and RevenueCat.

The sample covers the LevelPlay formats that have public demo credentials:

- Banner
- Interstitial
- Rewarded
- An explicit load-failure example

Native, app-open, and rewarded-interstitial ads are out of scope because Unity's public LevelPlay demo configuration does not provide ad units for them.

## Event mapping

Each LevelPlay callback is mapped to the equivalent RevenueCat `AdTracker` call:

| LevelPlay callback | RevenueCat call |
| --- | --- |
| `onAdLoaded` | `trackAdLoaded` |
| `onAdDisplayed` | `trackAdDisplayed` and `trackAdRevenue` |
| `onAdClicked` | `trackAdOpened` |
| `onAdLoadFailed` | `trackAdFailedToLoad` |

`LevelPlayAdInfo.auctionId` is reused as the RevenueCat impression ID so events for the same impression can be correlated. LevelPlay reports impression revenue in USD as a decimal value; the sample converts it to micros before calling `trackAdRevenue`.

LevelPlay revenue precision maps as follows:

| LevelPlay | RevenueCat |
| --- | --- |
| `BID` | `EXACT` |
| `RATE` | `PUBLISHER_DEFINED` |
| `CPM` | `ESTIMATED` |
| Anything else | `UNKNOWN` |

## Setup

### RevenueCat

Add a RevenueCat public SDK key to the repository's `local.properties`:

```properties
REVENUECAT_API_KEY=your_api_key_here
```

The RevenueCat Test Store can be used for local testing. Keep the key in `local.properties`, which is
gitignored; do not add it to source files.

### LevelPlay

By default, the app uses the app key and ad unit IDs from Unity's [official LevelPlay demo app](https://github.com/ironsource-mobile/Mediation-Demo-Apps). These public values initialize the SDK but do not guarantee inventory: LevelPlay can return error 509 (`Mediation No fill`) with an empty waterfall.

For reliable testing:

1. Create an app in the LevelPlay dashboard.
2. Create banner, interstitial, and rewarded ad units.
3. Ensure the mediation configuration has an active `ironSource Ads` bidding instance for all three
   formats and that the ad units are included in an active mediation group.
4. In **LevelPlay > Settings > Test devices**, add the device's advertising ID.
5. For that device, select `ironSource Ads` and all three ad units, then select **Test ads**. This is a
   temporary assignment; enable it again after it expires.
6. Override the public demo values in `local.properties`:

```properties
LEVELPLAY_APP_KEY=your_app_key
LEVELPLAY_BANNER_AD_UNIT_ID=your_banner_ad_unit_id
LEVELPLAY_INTERSTITIAL_AD_UNIT_ID=your_interstitial_ad_unit_id
LEVELPLAY_REWARDED_AD_UNIT_ID=your_rewarded_ad_unit_id
```

Then build or run the `vanilla-ad-tracker-sample` app from Android Studio.

LevelPlay's separate Integration Test Suite can verify that a mediated network serves test ads, but it must run without interacting with the regular LevelPlay ad APIs first. It therefore does not exercise the RevenueCat callbacks demonstrated by this sample.

If every format returns `Mediation No fill`, confirm the test-device assignment is still active and the
device is online. On an emulator, also check that it is not configured to use an unreachable HTTP proxy.

## Key files

- `MainApplication.kt` initializes RevenueCat and LevelPlay.
- `data/Constants.kt` contains the public demo configuration.
- `ui/ads/` contains the manual event mappings for each supported format.
- `ui/ads/AdRevenuePrecisionMapping.kt` converts LevelPlay revenue precision.

## Verify events

Load and display an ad, then background the app so RevenueCat can flush pending events. The events should appear shortly afterward in the RevenueCat dashboard. Use the error-testing screen to verify `trackAdFailedToLoad` without waiting for a real inventory failure.
