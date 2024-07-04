# V7 API Migration Guide

This latest release updates the SDK to use BillingClient 6. This version of BillingClient brings little change compared
with BillingClient 5 which brought an entire new subscription model which resulted in large changes across the entire SDK.

The only modification at the API level involves replacing "ProrationMode" with "ReplacementMode". The specific replacement
modes remain unchanged.

~~If your app doesn't currently use DEFERRED replacement modes, then you should be safe to upgrade to this version without
changes in behavior.~~

~~If your app supports product changes using [DEFERRED replacement mode](https://www.revenuecat.com/docs/managing-subscriptions#google-play),
then you can either stick with the previous major version until support for DEFERRED is re-introduced in this major version,
or you can remove DEFERRED replacement options from your app.~~

UPDATE: DEFERRED replacement mode has been added back in version 7.12.0 and 8.2.0 of the Android SDK.

## Updated Code References

This migration guide has detailed class, property, and method changes.

See [Android Native - 5.x to 6.x Migration](https://www.revenuecat.com/docs/android-native-5x-to-6x-migration) for a
more thorough explanation of the new Google subscription model announced with BillingClient 5 and how to take advantage of it.

### Class/interface changes

| New                     |
|-------------------------|
| `ReplacementMode`       |
| `GoogleReplacementMode` |


| Deprecated            | Replace with            |
|-----------------------|-------------------------|
| `ProrationMode`       | `ReplacementMode`       |
| `GoogleProrationMode` | `GoogleReplacementMode` |

| Temporarily removed                             |
|-------------------------------------------------|
| `ProrationMode.DEFERRED` (added back in 7.12.0) |

### Bumped minimum Android SDK version

RevenueCat SDK v7 bumps minimum Android SDK version from Android 4.0 (API level 14) to Android 4.4 (API level 19).

### Support for Google Play InApp Messages

We've added support for [Google Play InApp Messages](https://rev.cat/googleplayinappmessaging) which will show users a snackbar message during grace period and account hold once per day and provide them an opportunity to fix their payment without leaving the app.

InApp Messages are shown by default. If you want to disable this behaviour during configuration of the RevenueCat SDK:

```kotlin
Purchases.configure(
    PurchasesConfiguration.Builder(applicationContext, apiKey)
        .showInAppMessagesAutomatically(false)
        .build()
)
```

### Observer Mode

Observer mode is still supported in v7. Other than updating the SDK version, there are no changes required..

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
