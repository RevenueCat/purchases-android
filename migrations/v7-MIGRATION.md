# V7 API Migration Guide

This latest release updates the SDK to use BillingClient 6. This version of BillingClient brings little change compared 
with BillingClient 5 which brought an entire new subscription model which resulted in large changes across the entire SDK.

The only modification at the API level involves replacing "ProrationMode" with "ReplacementMode". The specific replacement 
modes remain unchanged. 

However, there is a behavior change in the "DEFERRED" replacement mode. Previously, a purchase would only occur after the
product change. In BC6, the new purchase occurs place immediately, while the product change becomes effective when the old
item expires. Until we can properly support this new behaviour, we have disabled the "DEFERRED" replacement mode and it 
will be added back in a future release of the SDK. 

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

| Temporarily removed      |
|--------------------------|
| `ProrationMode.DEFERRED` |

### Observer Mode

Observer mode is still supported in v7. Other than updating the SDK version, there are no changes required..

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
