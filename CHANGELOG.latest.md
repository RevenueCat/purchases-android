## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrect as consumables, RevenueCat will consume these purchases. This means that customers won't be able to restore them from version 10.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions. Please see more information in our [docs](https://www.revenuecat.com/docs/known-store-issues/play-billing-library/restore-consumable-purchases-bc8)

### 💥 Breaking Changes
This release updates to Billing Library 8.3.0 with min SDK supported of Android 6 (API 23), previously min was 21. It also removes a previous workaround used to be able to restore consumed one time products which is not available anymore.

* Update to Billing Library 8.3.0 (#3256) via Toni Rico (@tonidero)
* Revert AIDL workaround for consumed consumables (#3255) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Bump fastlane-plugin-revenuecat_internal from `6289be1` to `ceecf91` (#3301) via dependabot[bot] (@dependabot[bot])
* Increase targetSdkVersion to 36 (#2446) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3302) via RevenueCat Git Bot (@RCGitBot)
* Bump fastlane-plugin-revenuecat_internal from `5d6e93f` to `6289be1` (#3299) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `f11fe40` to `5d6e93f` (#3294) via dependabot[bot] (@dependabot[bot])
