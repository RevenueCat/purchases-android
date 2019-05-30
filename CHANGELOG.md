## 2.2.3

- Fixes multi threading issues

## 2.2.2

- Fixes KotlinNullPointerException in Backend:248
- Fixes NullPointerException in Purchases:673

## 2.2.1

- Bugfix release: avoids caches being cleared when there is an error fetching purchaser info. Fixes https://github.com/RevenueCat/purchases-android/issues/68

## 2.2.0

- **BREAKING CHANGE** Call `syncTransactions` to send purchases information to RevenueCat after any restore or purchase if you are using the SDK in `observerMode`. See our guide on Migrating Subscriptions for more information on `syncTransactions`: https://docs.revenuecat.com/docs/migrating-existing-subscriptions
- `addAttribution` is now a static method that can be called before the SDK is configured.
- `addAttribution` will automatically add the `rc_gps_adid` parameter.
- A network user identifier can be send to the `addAttribution` function, replacing the previous `rc_appsflyer_id` parameter.
- Adds an optional configuration boolean `observerMode`. This will set the value of `finishTransactions` at configuration time.
- Updated BillingClient to 1.2.2.
- `makePurchase` now requires a SKUDetails product instead of a sku and type.
- Header updates to include client version and locale which will be used for debugging and reporting in the future.

## 2.1.2

- Fixes exception when inputstream is null.
- Fixes an exception when Purchases is instantiated from a background thread.
- Fixes concurrency issue on servicerequests.

## 2.1.1

- Adds setFinishTransactions. Set this to false if you are finishing transactions outside of the Purchases SDK

## 2.1.0

- Adds userCancelled as a parameter to the completion block of the makePurchase function
- Better error codes
- The entitlements call now fails if the service returns FEATURE_NOT_SUPPORTED or BILLING_UNAVAILABLE. Also that DEVELOPER_ERROR that was always displaying in the logs, now shows as "Billing Service Setup is already trying to connect."
- Fixes some exceptions not being reported. We might start seeing more crashes that were silent before.

## 2.0.1

- Fixes ConcurrentModificationException when making a purchase fails.

## 2.0.0

- Refactor to all block based methods
- Optional listener method to receive changes in Purchaser Info
- Ability to turn on detailed logging by setting `debugLogsEnabled`
- Use of AndroidX dependencies
- Upgraded Kotlin, compile and target versions

## 1.4.2

- Sends cached purchaser info after listener is set to fix offline

## 1.4.1

- Prevents calling endConnection if the service is not ready

## 1.4.0

- Adds singleton management inside the SDK
- Adds reset, create alias and identify calls
- Fixes callbacks being called in a background thread
- Renames allowUsingAnonymousId to allowsSharingPlayStoreAccount
- Other bugfixes

## 1.3.8

- Fixes onRestoreTransactions not being called if there are no tokens.

## 1.3.7

- Adds requestDate to the purchaser info to avoid edge cases

## 1.3.6

- Fix bug where closed RCPurchases were still listening for application lifecyle methods

## 1.3.5

- Fix for error edge case

## 1.3.4

- Add close method to Purchases, this should be called before creating a new purchases object

## 1.3.3

- Add close method to Purchases, this should be called before creating a new purchases object

## 1.3.2

- Ensure consumables are consumed
- Add ability to override

## 1.3.1

- Fix visibility for addAttribution methods

## 1.3.0

- Attribution! You can now pass attribution data from AppsFlyer, Adjust and Branch. You can then view the ROI of your campaigns, including revenue coming from referrals.

## 1.2.1

- Fix for entitlements, missing products now return as null, not blocking the whole entitlement

## 1.2.0

- Add error handler for entitlements fetch

## 1.1.0

- Add restore transactions listener methods

## 1.0.0

- Entitlements support
- Caching of purchaser info
- RevenueCat is coming out of beta! Sign up at https://www.revenuecat.com/

## 0.1.0

- Initial release
