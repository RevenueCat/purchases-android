# V7 API Migration Guide

This latest release updates the SDK to use BillingClient 6. This version of BillingClient brings an entire new
subscription model which has resulted in large changes across the entire SDK.

## Updated Code References

This migration guide has detailed class, property, and method changes.

See [Android Native - 5.x to 6.x Migration](https://www.revenuecat.com/docs/android-native-5x-to-6x-migration) for a 
more thorough explanation of the new Google subscription model announced with BillingClient 5 and how to take advantage of it in V6.

### Class/interface changes

| New                     |
|-------------------------|
| `ReplacementMode`       |
| `GoogleReplacementMode` |


| Deprecated            | Replace with            |
|-----------------------|-------------------------|
| `ProrationMode`       | `ReplacementMode`       |
| `GoogleProrationMode` | `GoogleReplacementMode` |

### API deprecations

| Deprecated                                                                                         | New                                                                                     |
|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `getSubscriptionSkusWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)`    | `getProductsWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)` |
| `getNonSubscriptionSkusWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)` | `getProductsWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)` |

### Observer Mode

Observer mode is still supported in v7. Other than updating the SDK version, there are no changes required..

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
