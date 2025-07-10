# V9 API Migration Guide

This latest release updates the SDK to use BillingClient 8. This version removed APIs to query for expired subscriptions and consumed one-time products, aside from other improvements. You can check the full list of changes here: https://developer.android.com/google/play/billing/release-notes#8-0-0

Additionally, we've also updated Kotlin to 2.0.21 but kept language compatibility which means we now require Kotlin 1.8.0+. If you were using an older version of Kotlin, you will need to update it.

Regarding API changes, we've also removed data classes from our public APIs. This means that for classes that were previously data classes, the `copy` constructor and `componentN` functions have been removed (unpacking). Equals/HashCode functions should still work as before.

### BC8: No expired subscriptions or consumed one-time products

With BC8, we can't query for expired subscriptions or consumed one-time products. This means that, for users migrating from a different implementation of the Play Billing Library, the SDK will not be able to send purchase information from these purchases. We can still ingest historical data from these purchases through a backend historical import. See [docs](https://www.revenuecat.com/docs/migrating-to-revenuecat/migrating-existing-subscriptions). This doesn't affect developers that have started using RevenueCat since day one.

### Bumped minimum Kotlin version

RevenueCat SDK v9 bumps Kotlin to 2.0.21, with a minimum Kotlin version of 1.8.0.

### Using the SDK with your own IAP code (previously Observer Mode)

Using the SDK with your own IAP code is still supported in v9. Other than updating the SDK version, there are no changes required. Just make sure the version of the billing client is also version 8.0.0+.

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
