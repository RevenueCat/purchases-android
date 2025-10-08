> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Fix deadlock in `allowSharingPlayStoreAccount` (#2707) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add E2E Test app for integration tests (#2715) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2714) via RevenueCat Git Bot (@RCGitBot)
* Bump fastlane-plugin-revenuecat_internal from `a8770fd` to `3f7fffc` (#2712) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `e555afb` to `a8770fd` (#2708) via dependabot[bot] (@dependabot[bot])
* Correct Opt In for FileRepository (#2709) via Jacob Rakidzich (@JZDesign)
* Paywalls -> Video Component (#2680) via Jacob Rakidzich (@JZDesign)
* Update fastlane-plugin-revenuecat_internal to e555afb (#2705) via Cesar de la Vega (@vegaro)
* Revert kotlinCompilerExtensionVersion for MagicWeatherCompose and CustomEntitlementComputationSample (#2701) via Jaewoong Eum (@skydoves)
* [EXTERNAL] Fix incorrect exception use in test (#2674) via @nikit19 (#2703) via Toni Rico (@tonidero)
