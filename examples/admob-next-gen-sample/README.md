# AdMob Next-Gen sample

This app demonstrates RevenueCat ad-event tracking with the Google Mobile Ads Next-Gen SDK.

## Run locally

1. Add `REVENUECAT_API_KEY=...` to the root `local.properties` file. That file is ignored by Git.
2. Sync Gradle in Android Studio.
3. Select the `admob-next-gen-sample` run configuration and wait until the home screen reports that Google Mobile Ads is ready.

The app uses Google's test app and ad-unit IDs by default. You can override any of these in the root `local.properties` file without changing tracked source:

```properties
ADMOB_APP_ID=...
ADMOB_BANNER_AD_UNIT_ID=...
ADMOB_INTERSTITIAL_AD_UNIT_ID=...
ADMOB_APP_OPEN_AD_UNIT_ID=...
ADMOB_NATIVE_AD_UNIT_ID=...
ADMOB_NATIVE_VIDEO_AD_UNIT_ID=...
ADMOB_REWARDED_AD_UNIT_ID=...
ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID=...
```

## Coverage

Each ad-format screen owns its direct and preloaded examples:

- Banner: callback-based direct loading, preload/poll/register, and banner lifecycle and refresh callbacks.
- Interstitial and app open: suspending direct loads, preload buffers, and show-time placement overrides.
- Rewarded and rewarded interstitial: direct/preloaded loading, ordinary reward callbacks, and RevenueCat reward verification.
- Native: standard/video ad-unit selection, suspending single load, multi-ad `Flow`, preloading, and result-specific handling.
- Diagnostics: direct and preloaded failure paths.

Preload start placements intentionally differ from poll placements. Full-screen show placements differ again, making the ownership of each tracking stage visible in the sample.

Every preloaded mode reports whether its preloader is running, its configured buffer size, and the live number of
ads available. The Start control becomes Stop while running. Buffer size can be adjusted from 1 to 5 before starting
and is locked until the preloader is stopped.

Server-to-server response overloads are not executable in a standalone sample because they require an opaque response obtained from a server. They are documented in the adapter README. The sample also avoids duplicating equivalent callback and suspending overloads when one does not demonstrate a distinct behavior.
