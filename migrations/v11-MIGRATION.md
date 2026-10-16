# V11 API Migration Guide

This release updates the SDK to Google Play Billing Library 9.1.0, Kotlin 2.2.21 and Jetpack Compose BOM 2026.06.01. The RevenueCat API itself is unchanged, but the new dependencies raise the build requirements of your app and can change the versions of libraries it already uses. The minimum Android version stays at Android 6 (API 23).

## Build requirements

### Kotlin 2.1.0 or newer

The minimum Kotlin version is now 2.1.0, up from 1.8.0 in v10. Older compilers fail with:

```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.2.0, expected version is 2.0.0.
```

Update the Kotlin Gradle Plugin in your app to 2.1.0 or newer.

If you use the SDK through a hybrid SDK, see its migration guide for the matching framework versions.

### compileSdk 35 or higher

Your app, and any library module that depends on the SDK, must compile against API 35 or later. Lower values fail with:

```
Execution failed for task ':app:checkDebugAarMetadata'.
> Dependency 'androidx.core:core:1.15.0' requires libraries and applications that
  depend on it to compile against version 35 or later of the
  Android APIs.
```

Set `compileSdk = 35` (or higher) in the affected modules. `minSdk` and `targetSdk` are not affected.

## Play Billing Library 9

The SDK now depends on Play Billing Library 9.1.0. Full release notes: https://developer.android.com/google/play/billing/release-notes#9-0-0

### Blocked Play Store reports `PurchaseNotAllowedError`

When the Play Store app is blocked by the system (for example in OEM kids modes), the SDK now reports `PurchasesErrorCode.PurchaseNotAllowedError`. Previous versions reported `PurchasesErrorCode.StoreProblemError` in this situation. If you branch on the error code to decide what to show the user, update that logic. `Purchases.canMakePayments` keeps returning `false` on these devices.

### Using the SDK with your own IAP code

The SDK exposes Play Billing Library as an `api` dependency, so version 9.1.0 is on your compile classpath even if you never declared it. If you do declare it, it must be 9.0.0 or later.

Play Billing Library 9 removed `SkuDetails`, `SkuDetailsParams`, `SkuDetailsResponseListener`, `BillingClient.SkuType`, `QueryPurchaseHistoryParams` and `BillingFlowParams.Builder.setSkuDetails`. If your code still uses them, migrate to `ProductDetails`, `QueryProductDetailsParams` and `BillingClient.ProductType` before upgrading.

## Jetpack Compose (RevenueCatUI)

RevenueCatUI now depends on Compose BOM 2026.06.01 and exports it as a platform constraint. Your own Compose dependencies resolve to at least that BOM's versions, for example `androidx.compose.material:material` 1.11.4 and `androidx.compose.material3:material3` 1.4.0.

## Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
