# AdMob Next-Gen sample

This app demonstrates RevenueCat ad-event tracking with the Google Mobile Ads Next-Gen SDK. See the
[adapter README](../../feature/admob-next-gen/README.md) for installation instructions and complete API examples.

## Run locally

1. Add `REVENUECAT_API_KEY=...` to the root `local.properties` file. That file is ignored by Git.
2. Connect a device or emulator running Android 7.0 (API level 24) or newer.
3. Sync Gradle in Android Studio.
4. Select the `admob-next-gen-sample` run configuration and wait until the home screen reports that Google Mobile Ads is ready.

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

## Exercise the sample

Each format has **Direct** and **Preloaded** modes. Start with a direct load. Then select **Preloaded**, choose a
buffer size, select **Start**, wait until **Ads available** is greater than zero, and select **Poll + Show**. Stop a
preloader before changing options that form part of its configuration.

Use the format-specific controls to exercise behavior that is not shared by every ad type:

- **Banner:** confirm the returned ad is registered with the existing `AdView` and continues reporting refreshes.
- **Interstitial and app open:** compare the load, preload, poll, and show placement names in the resulting events.
- **Rewarded and rewarded interstitial:** try both ordinary Google rewards and RevenueCat reward verification. The
  verification choice is captured when an ad is loaded or polled, so change it before that step.
- **Native:** try standard and video test ad units, then enable **Batch Flow** to collect more than one result.
- **Diagnostics:** trigger both direct and preloaded failures with the intentionally invalid ad unit.

Google's native and native-video test inventory can be inconsistent. A failed test request does not necessarily mean
the renderer or tracking integration is broken; use your own test-enabled ad unit when you need reliable native fills.

## Verify RevenueCat events

After exercising a flow, background the app so the RevenueCat SDK flushes queued events. Check the RevenueCat
dashboard for loaded, displayed, opened, revenue, and failed-to-load events with the placement shown by the sample.
Google test ads do not always emit paid events, so absence of an ad-revenue event alone is not a tracking failure.

Google Mobile Ads Next-Gen can invoke callbacks on a background thread. The sample re-enters its Compose-owned
coroutine scope before changing UI or reward state. Preserve that main-thread boundary when adapting callback-based
examples to UI code.

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
