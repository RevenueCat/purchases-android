## 6.3.0
### New Features
* `Offering` metadata (#1013) via Josh Holtz (@joshdholtz)
* Support DEFERRED mode (#985) via swehner (@swehner)
* Add completion callback to syncPurchases API (#1002) via Toni Rico (@tonidero)
* Enable offline entitlements (#987) via Toni Rico (@tonidero)
### Bugfixes
* Workaround bug in android 4 for JSON objects with List<String> (#942) via Andy Boedo (@aboedo)
* Bump dokka to 1.8.10 to support Gradle 8 (#1009) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `fe45299` to `13773d2` (#1015) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Disable offline entitlements temporarily (#1023) via Toni Rico (@tonidero)
* Fix integration tests in CI (#1019) via Toni Rico (@tonidero)
* Add offline entitlements integration tests (#1006) via Toni Rico (@tonidero)
* Disable offline entitlements in observer mode (#1014) via Toni Rico (@tonidero)
* Extracts setup and teardown to BasePurchasesTest (#1011) via Cesar de la Vega (@vegaro)
* Support forcing server errors for tests (#1008) via Toni Rico (@tonidero)

## 6.2.0
### New Features
* CAT-859 Expose whether or not a SubscriptionOption is Prepaid in the SDK (#1005) via Deema AlShamaa (@dalshamaa)
### Bugfixes
* [CF-1324] Fix personalizedPrice defaulting to false (#952) via beylmk (@beylmk)
### Performance Improvements
* Store and return ETag last refresh time header (#978) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `3b03efa` to `fe45299` (#991) via dependabot[bot] (@dependabot[bot])
* Bump danger from 9.2.0 to 9.3.0 (#981) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `8482a43` to `3b03efa` (#974) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.212.1 to 2.212.2 (#973) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `9255366` to `8482a43` (#961) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Add proration modes to post to backend (#977) via swehner (@swehner)
* Added ENTITLEMENTS_COMPUTED_ON_DEVICE (#939) via Cesar de la Vega (@vegaro)
* Fix flaky test in OfflineCustomerInfoCalculatorTest (#997) via Cesar de la Vega (@vegaro)
* Fix `OfflineCustomerInfoCalculatorTest` `Unresolved reference: ProducType` (#995) via Cesar de la Vega (@vegaro)
* Add support for product_plan_identifier for offline customer info (#959) via Cesar de la Vega (@vegaro)
* Add non-subscriptions support to offline customer info (#958) via Cesar de la Vega (@vegaro)
* Query only active purchases when generating offline entitlements customer info (#1003) via Toni Rico (@tonidero)
* Fix `PurchasesIntegrationTest` building issue (#996 into main) (#998) via Cesar de la Vega (@vegaro)
* Fail offline entitlements computation if product entitlement mapping not available (#999) via Toni Rico (@tonidero)
* Fix  build_magic_weather lane (#993) via Cesar de la Vega (@vegaro)
* Add backend integration tests and test product entitlement mapping endpoint (#988) via Toni Rico (@tonidero)
* Fix purchases integration tests (#980) via Toni Rico (@tonidero)
* Disable offline entitlements if active inapp purchases exist (#983) via Toni Rico (@tonidero)
* Clear cached customer info upon entering offline entitlements mode (#989) via Toni Rico (@tonidero)
* Update product entitlement mapping request to new format (#976) via Toni Rico (@tonidero)
* Support enabling/disabling offline entitlements (#964) via Toni Rico (@tonidero)
* Add back integration tests automation (#972) via Toni Rico (@tonidero)
* Upgrade to AGP 8.0 (#975) via Toni Rico (@tonidero)
* Extract post receipt logic to PostReceiptHelper (#967) via Toni Rico (@tonidero)
* Add isServerDown to error callback for postReceipt and getCustomerInfo requests (#963) via Toni Rico (@tonidero)
* Add back integration test flavors (#962) via Toni Rico (@tonidero)
* Fix storing test results (#966) via Cesar de la Vega (@vegaro)
* Extract detekt job from test job (#965) via Cesar de la Vega (@vegaro)

## 6.1.1
### Bugfixes
* Remove flavors temporarily to fix deployment (#953) via Toni Rico (@tonidero)
### Other Changes
* Add diagnostics tracking for BC5 google methods (#876) via Toni Rico (@tonidero)
### Changes from 6.1.0
#### New Features
* Add `OfferPaymentMode` on to `PricingPhase` to describe behavior for offers (#943) via Josh Holtz (@joshdholtz)
* Add `Purchases.sharedInstance.store` to get currently configured `Store` (#937) via Josh Holtz (@joshdholtz)
#### Bugfixes
* Add missing presentedOfferingId to `StoreProduct` and `SubscriptionOption` (#930) via beylmk (@beylmk)
* Moved `toPurchasesError` back to internal (#944) via Cesar de la Vega (@vegaro)
#### Other Changes
* Compute CustomerInfo from a list of purchases (#885) via Cesar de la Vega (@vegaro)
* Extract all customer info json strings (#945) via Cesar de la Vega (@vegaro)
* Add verification for entitlement granting to integration tests (#946) via Toni Rico (@tonidero)
* Add note about observer mode to v6-migration (#929) via beylmk (@beylmk)
* Report load shedder integration tests results in slack for V6 (#936) via Toni Rico (@tonidero)
* Increase heap size (#935) via Cesar de la Vega (@vegaro)
* Re-add new integration tests after V6 changes (#928) via Toni Rico (@tonidero)

## 6.1.0
### New Features
* Add `OfferPaymentMode` on to `PricingPhase` to describe behavior for offers (#943) via Josh Holtz (@joshdholtz)
* Add `Purchases.sharedInstance.store` to get currently configured `Store` (#937) via Josh Holtz (@joshdholtz)
### Bugfixes
* Add missing presentedOfferingId to `StoreProduct` and `SubscriptionOption` (#930) via beylmk (@beylmk)
* Moved `toPurchasesError` back to internal (#944) via Cesar de la Vega (@vegaro)
### Other Changes
* Compute CustomerInfo from a list of purchases (#885) via Cesar de la Vega (@vegaro)
* Extract all customer info json strings (#945) via Cesar de la Vega (@vegaro)
* Add verification for entitlement granting to integration tests (#946) via Toni Rico (@tonidero)
* Add note about observer mode to v6-migration (#929) via beylmk (@beylmk)
* Report load shedder integration tests results in slack for V6 (#936) via Toni Rico (@tonidero)
* Increase heap size (#935) via Cesar de la Vega (@vegaro)
* Re-add new integration tests after V6 changes (#928) via Toni Rico (@tonidero)

## 6.0.1
### Bugfixes
* Fix infinite loop when accesing deprecated property (#926) via Toni Rico (@tonidero)

## 6.0.0
**RevenueCat Android SDK v6** is here!! 😻

This latest release updates the SDK to use BillingClient 5. This version of BillingClient brings an entire new subscription model which has resulted in large changes across the entire SDK.

## Migration Guides
- See the full list of API changes in [our v6 migration doc](https://github.com/RevenueCat/purchases-android/blob/v6/migrations/v6-MIGRATION.md)

- See [Android Native - 5.x to 6.x Migration](https://www.revenuecat.com/docs/android-native-5x-to-6x-migration) for a
  more thorough explanation of the new Google subscription model announced with BillingClient 5 and how to take
  advantage of it in V6. This guide includes tips on product setup with the new model.

**Note:** This release is based off of `6.0.0-rc.1`. Developers migrating from that version shouldn't see any changes.

## API changes:
There have been a lot of changes since v5!

Here are the highlights:

### Purchasing API changes

Purchases are now configured using `PurchaseParams.Builder()`. The builder is constructed with either a `Package`,
`Product`, or `SubscriptionOption` and an `Activity`. The builder then has methods for setting the product change
parameters (`oldProductId` and `googleProrationMode`).

To initiate a purchase, simply pass the built `PurchaseParams` and your `PurchaseCallback` to the `purchase()` method.

#### Applying offers on a purchase

In V5, a `Package` or `StoreProduct` represented a single purchaseable entity, and free trials or intro offers would
automatically be applied if the user was eligible.

### New `SubscriptionOption` concept

Now, in v6, a `Package` or `StoreProduct` represents a duration of a subscription and contains all the ways to
purchase that duration -- any offers and its base plan. Each of these purchase options are `SubscriptionOption`s.
When passing a `Package` or `StoreProduct` to `purchase()`, the SDK will use the following logic to choose which
`SubscriptionOption` to purchase:
*   - Filters out offers with "rc-ignore-offer" tag
*   - Uses `SubscriptionOption` with the longest free trial or cheapest first phase
*   - Falls back to base plan

For more control, create your `PurchaseParams.Builder` with the desired `SubscriptionOption`.

## 5.8.2

⚠️ ⚠️ ⚠️ ⚠️ 

Our Android SDK versions 5.8.0 and 5.8.1 introduced a bug that prevented new purchase tokens from being sent to RevenueCat for validation between the above dates.

Users who made new purchases were charged by Google but did not receive entitlements during that time. We've already shipped a backend fix, so affected users can recover their purchases simply by opening the app again. If the purchases aren’t recovered by the users within 72 hours from their purchase date, Google will automatically refund their purchase. No further action is required from you at this time.

Users with pre-existing purchases are not affected.

⚠️ ⚠️ ⚠️ ⚠️

### Other changes in 5.8.0 and 5.8.1
### New Features
* Diagnostics (#811) via Toni Rico (@tonidero)
### Bugfixes
* Fix issue with missing subscriber attributes if set after login but before login callback (#809) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane from 2.212.0 to 2.212.1 (#821) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.211.0 to 2.212.0 (#808) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-versioning_android from 0.1.0 to 0.1.1 (#798) via dependabot[bot] (@dependabot[bot])
* Bump danger from 8.6.1 to 9.2.0 (#778) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Fix docs deployment (#836) via Toni Rico (@tonidero)
* Fix SDKMAN issues (#822) via Toni Rico (@tonidero)
* Fix bundle install on CircleCI (#827) via Cesar de la Vega (@vegaro)
* Update README.md to include minimum Kotlin version (#786) via Cesar de la Vega (@vegaro)
* Remove `tag_release_with_latest_if_needed` fastlane lane (#781) via Cesar de la Vega (@vegaro)
* Adds docs for timeouts when closing and releasing (#759) via Cesar de la Vega (@vegaro)
* Add Amazon App tester package to purchase tester queries (#789) via Stefan Wehner (@tonidero)

## 5.7.1
### Bugfixes
* Revert "Update gems" (#774) via Josh Holtz (@joshdholtz)
### Dependency Updates
* Bump git from 1.12.0 to 1.13.2 (#777) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.210.1 to 2.211.0 (#779) via dependabot[bot] (@dependabot[bot])
* Update gems (#765) via Cesar de la Vega (@vegaro)
### Other Changes
* Adds dependabot (#769) via Cesar de la Vega (@vegaro)
* Updating comment for syncPurchases (#763) via HaleyRevcat (@HaleyRevcat)

## 5.7.0
### New Features
* Add `Purchases.logLevel` and deprecate `Purchases.debugLogsEnabled` (#753) via NachoSoto (@NachoSoto)
### Bugfixes
* Avoid syncing attributes for users with blank user ids (#755) via Toni Rico (@tonidero)
### Other Changes
* Fixed Readme.MD (#727) via AristiDevs (@ArisGuimera)
* Add codecov (#750) via Cesar de la Vega (@vegaro)
* Update AGP to 7.4.0 (#747) via Cesar de la Vega (@vegaro)
* Add test coverage using Kover (#748) via Cesar de la Vega (@vegaro)

## 5.6.7
### Bugfixes
* Fix crash when cleaning old attribution data if SharedPreferences has a null key (#742) via Toni Rico (@tonidero)
### Other Changes
* Update Gemfile.lock (#734) via Cesar de la Vega (@vegaro)
* remove stalebot in favor of SLAs in Zendesk (#725) via Andy Boedo (@aboedo)
* Update fastlane-plugin-revenuecat_internal to latest version (#724) via Cesar de la Vega (@vegaro)
* Bumps AGP to 7.3.1 (#718) via Cesar de la Vega (@vegaro)

## 5.6.6
### Bugfixes
* Added androidx.lifecycle.DefaultLifecycleObserver to consumer-rules.pro (#714) via Cesar de la Vega (@vegaro)

## 5.6.5
### Bugfixes
* Fix for AbstractMethodError DefaultLifecycleObserver.onCreate (#695) via Cesar de la Vega (@vegaro)
### Other Changes
* Remove unnecessary appcompat dependency (#691) via Toni Rico (@tonidero)

## 5.6.4
### Bugfixes
* Fix for calling getUnsyncedSubscriberAttributes callback twice (#687) via Toni Rico (@tonidero)
### Other Changes
* Update Fastlane plugin (#681) via Cesar de la Vega (@vegaro)
* Update fastlane-plugin-revenuecat_internal (#668) via Cesar de la Vega (@vegaro)
* Moves hold job after integration tests (#644) via Cesar de la Vega (@vegaro)
* Fix GitHub releases not being created  (#650) via Cesar de la Vega (@vegaro)
* Store debug builds of purchase tester on each commit (#656) via Toni Rico (@tonidero)
* Remove bc5DevSupport flavor from main (#654) via Cesar de la Vega (@vegaro)
* Disable unsupported stores in Purchase Tester UI (#652) via Toni Rico (@tonidero)

## 5.6.3
### Other Changes
* Add navigation to logs fragment from different screens (#647) via Toni Rico (@tonidero)
* Update Maven publish plugin (#646) via Cesar de la Vega (@vegaro)
* Add cache amazon user data (#640) via Toni Rico (@tonidero)

## 5.6.2
### Bugfixes
* Add timeout to Amazon's getProducts and getUserData requests (#630) via Toni Rico (@tonidero)
### Other Changes
* Automate purchase tester publish on releases (#639) via Toni Rico (@tonidero)
* Enable HTTP traffic in purchase tester (#636) via Toni Rico (@tonidero)
* Add button to navigate from login fragment to configure fragment (#635) via Toni Rico (@tonidero)
* Store purchase tester sdk configuration in DataStore (#633) via Toni Rico (@tonidero)
* Add configuration fragment to purchase tester (#632) via Toni Rico (@tonidero)
* Update fastlane plugin (#627) via Cesar de la Vega (@vegaro)
* Offerings cleanup (#626) via beylmk (@beylmk)
* Initial Support for BillingClient5 (#616) via beylmk (@beylmk)
* Adds action parameter to CircleCI config to be able to trigger the automatic bump manually (#621) via Cesar de la Vega (@vegaro)
* Not create release if next version should be skipped (#624) via Toni Rico (@tonidero)
* Add missing packageName to helper methods in AppConfig (#620) via Toni Rico (@tonidero)
* Add package name to all requests headers (#618) via Toni Rico (@tonidero)
* Update releasing instructions (#602) via Cesar de la Vega (@vegaro)
* Move to SNAPSHOT version on every commit to main if needed (#611) via Toni Rico (@tonidero)

## 5.6.1
### Bugfixes
* Wait to obtain unsubscribed attributes until finishing getting device identifiers (#604) via Toni Rico (@tonidero)
### Other Changes
* Release train (#599) via Cesar de la Vega (@vegaro)
* Adds Danger (#597) via Cesar de la Vega (@vegaro)
* Update releasing documentation (#600) via Toni Rico (@tonidero)

## 5.6.0
### New Features
* CleverTapID integration (#593) via Joshua Liebowitz (@taquitos)

### Other Changes
* Rename package name of purchase tester app to com.revenuecat.purchases_sample (#592) via Toni Rico (@tonidero)

## 5.5.0
### API Updates
* `Transaction` class properties `revenuecatId` and `productId` have been deprecated. Use `transactionIdentifier` and `productIdentifier` respectively instead (#587)

### Other Changes
* Remove billing client 3 support (#567) via beylmk (@beylmk)

## 5.4.1
* Update build.gradle (#579) via Joshua Liebowitz (@taquitos)
* Support for fastlane-plugin-revenuecat_internal (#557) via Toni Rico (@tonidero)
* Revert handler nullability change (#578) via Toni Rico (@tonidero)
* Sync subscriber attributes before login and logout (#572) via Toni Rico (@tonidero)
* Prepare next version: 5.5.0-SNAPSHOT (#574) via RevenueCat Releases (@revenuecat-ops)

## 5.4.0

* Upgrade compile sdk and target sdk versions and Android Gradle Plugin, Kotlin and a few other versions
  * https://github.com/RevenueCat/purchases-android/pull/474

## 5.3.0

* Updates the `unityIAP` build flavor so we don't include the billing client directly
  * https://github.com/RevenueCat/purchases-android/pull/566
* Fixes issue where SDK could not be initialized on a thread other than the main thread
  * https://github.com/RevenueCat/purchases-android/pull/568

## 5.2.1

* Updates the `unityIAP` build flavor for compatibility with Unity IAP >= v3.1.0 for observer mode.

## 5.2.0

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


[Full Changelog](https://github.com/revenuecat/purchases-android/compare/5.1.1...5.2.0)


## 5.1.1

- Fix for Amazon prices that contain non breaking spaces. Transactions were being saved with the wrong price.
  - https://github.com/RevenueCat/purchases-android/pull/538

[Full Changelog](https://github.com/revenuecat/purchases-android/compare/5.1.0...5.1.1)

## 5.1.0

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

## 5.0.0

### Migration Guide
- A number of renames, removal of some deprecated functions, and a change to configuration. Amazon Store support is also included Checkout the full list of changes in [migrations/v5-MIGRATION.md]

### API changes:
- See our [RevenueCat V5 API update doc]([migrations/v5-MIGRATION.md]) for API updates.

### Amazon Store Support
We have introduced support for using the Amazon Store. We have extensively tested this, and there are some apps using our pre-release Amazon versions in production.

However, we have found some inconsistencies in the way Amazon Store prices are reported. We are actively working on patching these inconsistencies.

Please help us help you by reporting any issues you find. [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).

You can enable Amazon Store support by configuring the SDK using the new configure function:

```
Purchases.configure(AmazonConfiguration.Builder(this, "public_amazon_sdk_key").build())
```

For more information around configuration please take a look at the [Amazon Store section in our docs](https://docs.revenuecat.com/docs/amazon-platform-resources). The official [Amazon In-App Purchasing docs](https://developer.amazon.com/docs/in-app-purchasing/iap-overview.html) also contain very valuable information, specially around testing and best practices.

If you're updating from an alpha version with Amazon Store support. Remove the in-app-purchasing-2.0.76.jar dependency from your `app/build.gradle` and from the libs folder. Manually adding the jar is no longer required 😄

```
dependencies {
    implementation files('libs/in-app-purchasing-2.0.76.jar') -> Remove this
```

**⚠️ ⚠️  Important ⚠️ ⚠️** Observer mode for Amazon is not supported yet.
 
### Changelog

**Note:** This release is based off of 5.0.0-rc3. These are the changes compared to 5.0.0-rc3:

- Removed deprecated `addAttributionData` functions and `AttributionNetwork` enum
    https://github.com/RevenueCat/purchases-android/pull/506
- Added retries with exponential backoff when reconnecting to Google's billing service
    https://github.com/RevenueCat/purchases-android/pull/489
- Updated to BillingClient 4.1
    https://github.com/RevenueCat/purchases-android/pull/500
- Prevent Amazon from swallowing exceptions
    https://github.com/RevenueCat/purchases-android/pull/499
- Improvements on Amazon implementation preventing a lot of issues related to price and currency parsing
    https://github.com/RevenueCat/purchases-android/pull/503

[Full Changelog](https://github.com/revenuecat/purchases-android/compare/5.0.0-rc3...5.0.0)

## 5.0.0-rc3

- Added Purchases.logHandler
  https://github.com/RevenueCat/purchases-android/pull/492
- Improved logic for caching getCustomerInfo responses
  https://github.com/RevenueCat/purchases-android/pull/486
- Fix queryPurchases for Amazon
  https://github.com/RevenueCat/purchases-android/pull/494
- Fix for price parsing for Amazon - use regex instead of Locale
  https://github.com/RevenueCat/purchases-android/pull/495
- Add DangerousSetting.AUTO_SYNC_OFF - please don't use this setting without talking to a member of the RevenueCat team
  https://github.com/RevenueCat/purchases-android/pull/462
- Multiple updates on purchase-tester
  https://github.com/RevenueCat/purchases-android/pull/481
  https://github.com/RevenueCat/purchases-android/pull/493
  
## 5.0.0-rc2

- Disabled Amazon observer mode since it's not supported yet
    https://github.com/RevenueCat/purchases-android/pull/469
- Updated `canMakePayments` to always return `true` if called on Amazon
    https://github.com/RevenueCat/purchases-android/pull/431
- Updated doc links to use rev.cat style links
    https://github.com/RevenueCat/purchases-android/pull/460
- Updated MagicWeather to reflect configuration changes and Amazon configuration
    https://github.com/RevenueCat/purchases-android/pull/456
    https://github.com/RevenueCat/purchases-android/pull/458
    https://github.com/RevenueCat/purchases-android/pull/465
    https://github.com/RevenueCat/purchases-android/pull/467
- Multiple updates on purchase-tester
    https://github.com/RevenueCat/purchases-android/pull/448
    https://github.com/RevenueCat/purchases-android/pull/452
    https://github.com/RevenueCat/purchases-android/pull/464
    https://github.com/RevenueCat/purchases-android/pull/472
- Remove purchase-tester-java since it didn't represent a real usage of our APIs
    https://github.com/RevenueCat/purchases-android/pull/463
- Created a publish variant to distribute a version of the SDK that uses BillingClient 3 instead of 4
    https://github.com/RevenueCat/purchases-android/pull/444

## 5.0.0-rc1

- Added Amazon support
- A number of type renames, removal of some deprecated functions, and a change to configuration. Checkout
  the full list of changes in [migrations/v5-MIGRATION.md]

## 4.6.1

- Added `exported` attribute to ResponseReceiver, for builds targeting SDK 31
  https://github.com/RevenueCat/purchases-android/pull/404
  
## 4.6.0

- Added `EntitlementInfo.ownershipType`, which can be used to determine whether an entitlement was granted by a direct purchase or shared through a family member.
    https://github.com/RevenueCat/purchases-android/pull/382
- Added a log warning when `configure` is called with an empty string as `appUserID`, to make it clear that the user will be considered an anonymous user.
    https://github.com/RevenueCat/purchases-android/pull/384

## 4.5.0

- Added `isConfigured` to be able to check if there is an instance of Purchases already configured
    https://github.com/RevenueCat/purchases-android/pull/378

## 4.4.0

- Added support for Airship integration via `setAirshipChannelID`
    https://github.com/RevenueCat/purchases-android/pull/375

## 4.3.3

- Patch around BillingClient bug resulting in multiple responses from querySkuDetailsAsync and queryPurchaseHistoryAsync
    https://github.com/RevenueCat/purchases-android/pull/369
- Update test dependencies, Gradle plugin and buildToolsVersion
    https://github.com/RevenueCat/purchases-android/pull/363
    https://github.com/RevenueCat/purchases-android/pull/365

## 4.3.2

- Makes improvements to prevent multiple HTTP requests with the same parameters to `/identify` and `/alias`. 
    https://github.com/RevenueCat/purchases-android/pull/358
    https://github.com/RevenueCat/purchases-android/pull/359

## 4.3.1

- Moves the code of `onBillingSetupFinished` to the main thread. Changes the callback of `canMakePayments` to the main thread. 
    https://github.com/RevenueCat/purchases-android/pull/349/
    https://github.com/RevenueCat/purchases-android/issues/348

## 4.3.0

### Identity V3:

In this version, we’ve redesigned the way that user identification works. 
Detailed docs about the new system are available [here](https://docs.revenuecat.com/v3.2/docs/user-ids).

#### New methods
- Introduces `logIn`, a new way of identifying users, which also returns whether a new user has been registered in the system. 
`logIn` uses a new backend endpoint. 
- Introduces `logOut`, a replacement for `reset`. 

#### Deprecations / removals
- deprecates `createAlias` in favor of `logIn`
- deprecates `identify` in favor of `logIn`
- deprecates `reset` in favor of `logOut`
- deprecates `allowSharingPlayStoreAccount` in favor of dashboard-side configuration

    https://github.com/RevenueCat/purchases-android/pull/250
    https://github.com/RevenueCat/purchases-android/pull/260
    https://github.com/RevenueCat/purchases-android/pull/252


### Other changes: 
- Fixed CI issues with creating pull requests
    https://github.com/RevenueCat/purchases-android/pull/324
- Re-enable eTags support to minimize unnecessary network traffic
    https://github.com/RevenueCat/purchases-android/pull/337
- When making a multi-line subscription purchase, all product ids are now sent to the backend.
    https://github.com/RevenueCat/purchases-android/pull/335
- Added `@Throws` annotation to `getPackage`, which could throw `NoSuchElementException`, but it wasn't documented.
    https://github.com/RevenueCat/purchases-android/pull/333
- Updated BillingClient to version [4.0.0](https://developer.android.com/google/play/billing/release-notes#4-0).
    https://github.com/RevenueCat/purchases-android/commit/f6554bbf7376c3fd492f0bc67183a9f35889ae78

## 4.2.1

- Temporarily disable ETags
     https://github.com/RevenueCat/purchases-android/pull/322
- Converts attribution data to use subscriber attributes
     https://github.com/RevenueCat/purchases-android/pull/315
- Clarify usage of sharedInstance
     https://github.com/RevenueCat/purchases-android/pull/320

## 4.2.0

- Update incorrect ReplaceWith annotation
     https://github.com/RevenueCat/purchases-android/pull/303
- Fix crash on empty SKUs in list passed to querySkuDetailsAsync
     https://github.com/RevenueCat/purchases-android/pull/302
- Add eTags support to avoid unnecessary backend responses
     https://github.com/RevenueCat/purchases-android/pull/292
     https://github.com/RevenueCat/purchases-android/pull/305
- Add canMakePayments method to check for billing and feature support
     https://github.com/RevenueCat/purchases-android/pull/304

## 4.1.0

 - Updated purchase-tester sample app and modified structure of the examples folder.
     https://github.com/RevenueCat/purchases-android/pull/296
     https://github.com/RevenueCat/purchases-android/pull/297
 - Added back the call setObfuscatedAccountID when purchasing for non-upgrades/downgrades.
     https://github.com/RevenueCat/purchases-android/pull/294
 - Updated error mapping and created a new ConfigurationError for cases when the package name is wrongly configured in the dashboard.
     https://github.com/RevenueCat/purchases-android/pull/298

## 4.0.5

- Catches NullPointerException on DeviceCache.findKeysThatStartWith
    https://github.com/RevenueCat/purchases-android/pull/284
- Catch exceptions when getting stringSet from sharedPreferences
    https://github.com/RevenueCat/purchases-android/pull/280
    https://github.com/RevenueCat/purchases-android/pull/282
- Identity v3: update appUserID location in /identify
    https://github.com/RevenueCat/purchases-android/pull/287
- Added MagicWeather app example in the Examples folder
    https://github.com/RevenueCat/purchases-android/pull/264

## 4.0.4

- Updated willRenew property in the PurchaserInfo to be false also for Consumabled and Promotionals.
    https://github.com/RevenueCat/purchases-android/pull/259
- Added a numeric code to PurchasesErrorCode so that the code numbers are consistent between platforms.
    https://github.com/RevenueCat/purchases-android/pull/261

## 4.0.3

- Fixes JSONObjectParceler, SkuDetailsParceler and adds unit tests for other Parcelable classes.
    https://github.com/RevenueCat/purchases-android/pull/249
    https://github.com/RevenueCat/purchases-android/pull/253
    https://github.com/RevenueCat/purchases-android/pull/254
- Changes cache refresh period on background to 25 hours.
    https://github.com/RevenueCat/purchases-android/pull/255

## 4.0.2

- Update lifecycle version to 2.3.0-rc01 and made sure addObserver is called from the main thread. Should fix #240.
    https://github.com/RevenueCat/purchases-android/pull/241
- Updates BillingClient to version 3.0.2
    https://github.com/RevenueCat/purchases-android/pull/235
- Fixes some exceptions being swallowed by ExecutorService
    https://github.com/RevenueCat/purchases-android/pull/234
- Revamped logging strings, makes log messages from Purchases easier to spot and understand.
    https://github.com/RevenueCat/purchases-android/pull/238
    https://github.com/RevenueCat/purchases-android/pull/237
    https://github.com/RevenueCat/purchases-android/pull/236
    https://github.com/RevenueCat/purchases-android/pull/244
- Made `body` in the HttpResult not null
    https://github.com/RevenueCat/purchases-android/pull/242


## 4.0.1

- Adds mapping for ITEM_ALREADY_OWNED
    https://github.com/RevenueCat/purchases-android/pull/220
- Fixes incompatibilities with Mockito 1.x.x
    https://github.com/RevenueCat/purchases-android/pull/228
   
## 4.0.0

- Removes Billing permission from AndroidManifest since it's added by the BillingClient.
    https://github.com/RevenueCat/purchases-android/pull/211
- Fixes Deferred downgrades. The Purchase object in the completion block of `purchaseProduct` and `purchasePackage` is now nullable when changing products.
    https://github.com/RevenueCat/purchases-android/pull/200
- Deprecated makePurchase and getEntitlements have been removed. Use purchaseProduct/purchasePackage and getOfferings instead.
   
## 3.5.3

- More aggressive caches and jittering for apps in background 
    https://github.com/RevenueCat/purchases-android/pull/201
   
## 3.5.2

- Catch IOException when getAdvertisingIdInfo 
    https://github.com/RevenueCat/purchases-android/pull/197
- Updates BillingClient to 3.0.1
    https://github.com/RevenueCat/purchases-android/pull/199
- Changes the way we deserialize the JSON "management_url" to prevent weird behavior 
    https://github.com/RevenueCat/purchases-android/pull/203
- Moved strings to their own module 
    https://github.com/RevenueCat/purchases-android/pull/202
- Fixes dokka
    https://github.com/RevenueCat/purchases-android/pull/205
   
## 3.5.1

- Fixes an issue where after calling invalidatePurchaserInfoCache and then purchaserInfoWithCompletion, the invalidated 
  cached version of purchaserInfo would be returned first, and only the delegate would get the updated version.
    https://github.com/RevenueCat/purchases-android/pull/189
- Catch TimeoutException when calling getAdvertisingIdInfo
    https://github.com/RevenueCat/purchases-android/pull/194
   
## 3.5.0
- Attribution V2:
    - Deprecated `addAttribution` in favor of `setAdjustId`, `setAppsflyerId`, `setFbAnonymousId`, `setMparticleId`.
    - Added support for OneSignal via `setOnesignalId`
    - Added `setMediaSource`, `setCampaign`, `setAdGroup`, `setAd`, `setKeyword`, `setCreative`, and `collectDeviceIdentifiers`
         https://github.com/RevenueCat/purchases-android/pull/184
- Fixed a RejectedExecutionException due to un-synchronized accesses to the ExecutorService 
    https://github.com/RevenueCat/purchases-android/pull/179
- Fixed downgrades/upgrades https://github.com/RevenueCat/purchases-flutter/issues/93
    https://github.com/RevenueCat/purchases-android/pull/179

## 3.4.1

- Addresses an issue where subscriber attributes might not sync correctly if subscriber info for the user hadn't been synced before the subscriber attributes sync was performed.
     https://github.com/RevenueCat/purchases-android/pull/184

## 3.4.0

- New properties added to the PurchaserInfo to better manage non-subscriptions.
https://github.com/RevenueCat/purchases-android/pull/172

## 3.3.0

- Added mParticle as one of the attribution options  
https://github.com/RevenueCat/purchases-android/pull/163
- Added original_purchase_date to JSON response
https://github.com/RevenueCat/purchases-android/pull/164
- Updated BillingClient to 3.0.0
https://github.com/RevenueCat/purchases-android/pull/166
- Moved the SKUDetails inside the ProductInfo that's passed to the Backend class when posting tokens
https://github.com/RevenueCat/purchases-android/pull/167

## 3.2.0

- Added `proxyKey`, useful for kids category apps, so that they can set up a proxy to send requests through. **Do not use this** unless you've talked to RevenueCat support about it. 
https://github.com/RevenueCat/purchases-android/pull/152
https://github.com/RevenueCat/purchases-android/pull/157
- Added `managementURL` to purchaserInfo. This provides an easy way for apps to create Manage Subscription buttons that will correctly redirect users to the corresponding subscription management page on all platforms. 
https://github.com/RevenueCat/purchases-android/pull/151
- Extra fields sent to the post receipt endpoint: `normal_duration`, `intro_duration` and `trial_duration`. These will feed into the LTV model for more accurate LTV values. 
https://github.com/RevenueCat/purchases-android/pull/148
https://github.com/RevenueCat/purchases-android/pull/156
- Fixed a bug where if the `context` passed to the SDK on setup is not an `Application` context, there is be a memory leak and potential issues getting the Advertising Info. 
https://github.com/RevenueCat/purchases-android/pull/147
- Migrated more classes to use Parcelize 
https://github.com/RevenueCat/purchases-android/pull/150

## 3.1.1

- Fix a subscriber attributes bug where the attributes are deleted when an alias is created. https://github.com/RevenueCat/purchases-android/pull/135
- New headers for observer mode and platform version https://github.com/RevenueCat/purchases-android/pull/136
- Fixed purchase buttons in Sample App https://github.com/RevenueCat/purchases-android/pull/141
- Fixed enablePendingPurchases not being called when calling isFeatureSupported https://github.com/RevenueCat/purchases-android/pull/138
- Adds a Java sample https://github.com/RevenueCat/purchases-android/pull/129
- Updates invalidatePurchaserInfoCache https://github.com/RevenueCat/purchases-android/pull/131
- Fixed Subscriber Attributes JSON in Android < 19 https://github.com/RevenueCat/purchases-android/pull/144

## 3.1.0

- Another fix for NoSuchElementException when retrieving Advertising ID #124
- Added Subscriber Attributes, which allow developers to store additional, structured information 
for a user in RevenueCat. More info: https://docs.revenuecat.com/docs/user-attributes

## 3.0.7

- Fixes NoSuchElementException #115

## 3.0.6

- Added new method to invalidate the purchaser info cache, useful when promotional purchases are granted from outside the app. #109

## 3.0.5

- Fixes compatibility with AppsFlyer SDK https://github.com/RevenueCat/purchases-android/pull/97
- Adds Fastlane to improve releases https://github.com/RevenueCat/purchases-android/pull/99
- Posts price and currency code for managed products https://github.com/RevenueCat/purchases-android/pull/96
- Adds platform flavor static variable https://github.com/RevenueCat/purchases-android/pull/91
- Fixes https://github.com/RevenueCat/purchases-android/issues/98   

## 3.0.4

- Defers fetching updated PurchaserInfo and Offerings to whenever the app is foregrounded.
- Adds Lifecycle components to better detect the lifecycle of the app.

## 3.0.3

- Fixes `syncPurchases` and `restorePurchases`.
- Adds `observer_mode` to the backend post receipt call.

## 3.0.2

- Exposes `all`, property of `Offerings` to access all the Offerings.

## 3.0.1

- Fixes some documentation

## 3.0.0

- Support for new Offerings system.
- Deprecates `makePurchase` methods. Replaces with `purchasePackage`
- Deprecates `entitlements` method. Replaces with `offerings`
- See our migration guide for more info: https://docs.revenuecat.com/v3.0/docs/offerings-migration
- Updates to BillingClient 2.0.3 (#88). If `finishTransactions` is set to `false` (or `observerMode` is true when configuring the SDK), this SDK won't acknowledge any purchase.
- Adds proration mode support on upgrades/downgrades (#86)
- New identity changes (#87):
    - The `.createAlias()` method is no longer required, use `.identify()` instead
    - `.identify()` will create an alias if being called from an anonymous ID generated by RevenueCat
    - Added an `isAnonymous` property to `Purchases.sharedInstance`
    - Improved offline use

## 2.4.1

- Adds missing close call on inputStream.

## 2.4.0

- Fixes crash in `updatePendingPurchaseQueue` ([#81] (https://github.com/RevenueCat/purchases-android/pull/81))
- Deprecates activeEntitlements in RCPurchaserInfo and adds entitlements object to RCPurchaserInfo. For more info check out https://docs.revenuecat.com/docs/purchaserinfo 

## 2.3.1

- Fix NullPointerException in BillingWrapper ([#79](https://github.com/RevenueCat/purchases-android/pull/79))
- Handle missing INTERNET permissions when making network requests ([#78](https://github.com/RevenueCat/purchases-android/pull/78))

## 2.3.0

- **BREAKING CHANGE** Removed deprecated makePurchase methods
- Improved logic on purchase tokens management
- **NEW FEATURE** Added Facebook as supported attribution network https://docs.revenuecat.com/docs/facebook-ads.

## 2.2.5

- Removes duplicated makePurchase call in deprecated makePurchase method.

## 2.2.4

- Fixes Proguard issue

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
