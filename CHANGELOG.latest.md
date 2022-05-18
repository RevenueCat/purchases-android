#### API updates:

- Added new method `setMixpanelDistinctID` as a convenience method for setting the required attribute for the Mixpanel integration
  - https://github.com/RevenueCat/purchases-android/pull/518
- Added `setFirebaseAppInstanceID` to allow associating RevenueCat users with Firebase.
  - https://github.com/RevenueCat/purchases-android/pull/523
- Adds `syncObserverModeAmazonPurchase` to sync Amazon purchases with RevenueCat. To be used in observer mode integrations.
  - https://github.com/RevenueCat/purchases-android/pull/483

#### Other:

- Updated error log message for cases when there are no products configured in the dashboard
  - https://github.com/RevenueCat/purchases-android/pull/517
- Adds missing docs for `purchaseProduct` and `purchasePackage`
  - https://github.com/RevenueCat/purchases-android/pull/520
- Better error handling when Amazon prices are missing
  - https://github.com/RevenueCat/purchases-android/pull/527
- Better error handling when Amazon user data cannot be loaded
  - https://github.com/RevenueCat/purchases-android/pull/528
- Fix detecting RevenueCat errors when loading offerings
  - https://github.com/RevenueCat/purchases-android/pull/529
- Adds price and marketplace to the headers of the `/receipts` API call
  - https://github.com/RevenueCat/purchases-android/pull/529

[Full Changelog](https://github.com/revenuecat/purchases-android/compare/5.0.0...5.1.0)
