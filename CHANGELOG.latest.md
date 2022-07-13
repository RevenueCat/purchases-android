## Changes from 5.2.1

* Updates the `unityIAP` build flavor for compatibility with Unity IAP >= v3.1.0 for observer mode.

## Changes from 5.2.0

### API Updates

* New API to get customer info with a given `CacheFetchPolicy`
  * https://github.com/RevenueCat/purchases-android/pull/546


### Other

* Validate API key
  * https://github.com/RevenueCat/purchases-android/pull/542
* Migrate from kotlin-android-extensions to kotlin-parcelize
  * https://github.com/RevenueCat/purchases-android/pull/545
* Removed private typealiases to fix generated documentation
  * https://github.com/RevenueCat/purchases-android/pull/554
* Fix for Amazon purchase dialog not showing up
  * https://github.com/RevenueCat/purchases-android/pull/552
* Added a log when `autoSyncPurchases` is disabled
  * https://github.com/RevenueCat/purchases-android/pull/555
* Attempt to reconnect to BillingClient when Billing response is error
  * https://github.com/RevenueCat/purchases-android/pull/558


[Full Changelog](https://github.com/revenuecat/purchases-android/compare/5.2.0...5.2.1)

