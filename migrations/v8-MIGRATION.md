# V8 API Migration Guide

This latest release updates the SDK to use BillingClient 7. This version increased the minSdk to 21 (Android 5.0) and removed the `ProrationMode` enum. Additionally, it added support for [installment plans](https://developer.android.com/google/play/billing/subscriptions#installments) and [pending prepaid plans](https://developer.android.com/google/play/billing/subscriptions#pending).

Additionally, we've also updated Kotlin which means we now require Kotlin 1.7.0+. If you were using an older version of Kotlin, you will need to update it.

The only modification at the API level involves removing `UpgradeInfo` and `ProrationMode`. Related functions that were deprecated previously in V7 of our SDK have now been removed completely since they depended on classes not available anymore in billing client 7.0.0.

If your app supports upgrading/downgrading, you need to migrate to use the `purchase(PurchaseParams)` method. The `PurchaseParams` parameter has accessors to set the `oldSku` and `replacementMode` which will allow you to handle upgrades and downgrades.

If you want to use Google's installment plans, you don't need to do anything! You can access the installment plan details from the SDK by using the `SubscriptionOption.installmentsInfo` property, like this:
```kotlin
val offerings = purchases.awaitOfferings()
// This provides the number of installments the customer commits to, and the number of installments they commit to upon a renewal.
val installmentsInfo = offerings.current?.monthly?.product?.defaultOption?.installmentsInfo
```

If you want to use Google's pending prepaid plans, you can enable it when configuring the SDK by using the `PurchaseConfiguration.Builder.pendingTransactionsForPrepaidPlansEnabled` function.
```kotlin
val purchaseConfig = PurchaseConfiguration.Builder(applicationContext, apiKey)
    .pendingTransactionsForPrepaidPlansEnabled(true)
    .build()
Purchases.configure(purchaseConfig)
```

## Updated Code References

This migration guide has detailed class, property, and method changes.

### Class/interface changes

| New                                                               |
|-------------------------------------------------------------------|
| `InstallmentsInfo`                                                |
| `GoogleInstallmentsInfo`                                          |
| `PurchaseConfiguration.pendingTransactionsForPrepaidPlansEnabled` |
| `SubscriptionOption.installmentsInfo`                             |

| Removed                                                               |
|-----------------------------------------------------------------------|
| `UpgradeInfo`                                                         |
| `ProrationMode`                                                       |
| `GoogleProrationMode`                                                 |
| `StoreTransaction.prorationMode`                                      |
| `PurchaseParams.googleProrationMode`                                  |
| `purchasePackage(activity, packageToPurchase, upgradeInfo, callback)` |
| `purchaseProduct(activity, productToPurchase, upgradeInfo, callback)` |

### Bumped minimum Android SDK version

RevenueCat SDK v8 bumps minimum Android SDK version from Android 4.4 (API level 19) to Android 5.0 (API level 21) since it's required by Google's Billing client.

### Observer Mode

Observer mode is still supported in v8. Other than updating the SDK version, there are no changes required. Just make sure the version of the billing client is also version 7.0.0.

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
