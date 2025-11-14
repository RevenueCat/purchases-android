> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add `countFrom` support for `CountdownComponent` (#2844) via Cesar de la Vega (@vegaro)
* Countdown Paywalls component (#2837) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Bump fastlane-plugin-revenuecat_internal from `1e3e3fd` to `9f78bb9` (#2840) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `9362b21` to `1e3e3fd` (#2836) via dependabot[bot] (@dependabot[bot])
* [AUTOMATIC] Update golden test files for backend integration tests (#2830) via RevenueCat Git Bot (@RCGitBot)
* Adds slack alert and heartbeat monitor to backend integration tests (#2826) via JayShortway (@JayShortway)
* Bump fastlane-plugin-revenuecat_internal from `525d48c` to `9362b21` (#2825) via dependabot[bot] (@dependabot[bot])
* Add internal `Offerings` and `ProductEntitlementMapping` source information (#2802) via Toni Rico (@tonidero)
* Add job to update golden test files for backend integration tests manually (#2824) via Toni Rico (@tonidero)
