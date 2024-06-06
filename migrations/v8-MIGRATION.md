# V8 API Migration Guide

This latest release updates the SDK to use BillingClient 7. This version of BillingClient brings little change compared
with BillingClient 5 which brought an entire new subscription model which resulted in large changes across the entire SDK. It did, however, increase the minSdk to 21 (Android 5.0) and removed the `ProrationMode` enum.

Aside from that, we've also updated Kotlin to 1.8.22.

The only modification at the API level involves removing `UpgradeInfo` and `ProrationMode`. Related functions that were deprecated previously in V7 of our SDK have now been removed completely since they depended on classes not available anymore in billing client 7.0.0.

If your app supports upgrading/downgrading, you need to migrate to use the `purchase(PurchaseParams)` method. The `PurchaseParams` parameter has accessors to set the `oldSku` and `replacementMode` which will allow you to handle upgrades and downgrades.

## Updated Code References

This migration guide has detailed class, property, and method changes.

See [Android Native - 5.x to 6.x Migration](https://www.revenuecat.com/docs/android-native-5x-to-6x-migration) for a
more thorough explanation of the new Google subscription model announced with BillingClient 5 and how to take advantage of it.

### Class/interface changes

| New                                                              |
|------------------------------------------------------------------|
| `InstallmentsInfo`                                               |
| `GoogleInstallmentsInfo`                                         |
| `PurchaseConfiguration.pendingTransactionsForPrepaidPlansEnabled` |
| `SubscriptionOption.installmentsInfo`                            |

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
