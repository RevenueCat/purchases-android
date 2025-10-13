> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Add support for the Test Store (#2710) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Add Customer Center per-view listener for hybrids (#2713) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* Add `TestStoreSimulatedPurchaseError` error code (#2716) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `3f7fffc` to `25c7fb8` (#2721) via dependabot[bot] (@dependabot[bot])
* Disable offline entitlements in test store (#2718) via Toni Rico (@tonidero)
