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
