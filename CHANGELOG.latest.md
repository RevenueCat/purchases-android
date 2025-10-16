## RevenueCat SDK
> [!NOTE]  
> This release brings all non-breaking changes between SDK versions 9.6.0 and 9.11.0 to major 8, so developers that don't/can't use major 9, can still get the latest updates. meant to be bought only once, for example, lifetime subscriptions.

### ✨ New Features
* Adds new experimental `storefrontLocale` APIs. (#2726) via JayShortway (@JayShortway)
* Turn on Video Component (#2725) via Jacob Rakidzich (@JZDesign)
* Add support for the Test Store (#2710) via Toni Rico (@tonidero)
* Add Airbridge device ID subscriber attribute (#2696) via Lim Hoang (@limdauto)
* Enable Test Store (#2685) via Antonio Pallares (@ajpallares)
### 🐞 Bugfixes
* Fix deadlock in `allowSharingPlayStoreAccount` (#2707) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Expose `launch` and `launchIfNeeded` to Java (#2695) via Cesar de la Vega (@vegaro)
#### 🐞 Bugfixes
* Fix bundled font families when using assets folder (#2672) via Toni Rico (@tonidero)
### Customer Center
#### ✨ New Features
* Add Customer Center per-view listener for hybrids (#2713) via Facundo Menzella (@facumenzella)
* Display Virtual Currencies in Customer Center (#2652) via Will Taylor (@fire-at-will)
#### 🐞 Bugfixes
* Fixes more Icons crashing with `NoClassDefFoundError` when using Material 1.4.0 (#2732) via Cesar de la Vega (@vegaro)
* Fix potential `NoClassDefFoundError` crash finding `Icons` caused by material3 1.4.0  (#2727) via Cesar de la Vega (@vegaro)
* Fix Expired badge in Customer Center dark mode (#2693) via Cesar de la Vega (@vegaro)
* Fix promo offer button CTA in Customer Center (#2682) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add test store maestro test (#2711) via Toni Rico (@tonidero)
* Finalize video component to include a checksum, and optimize memory usage for large file downloads (#2717) via Jacob Rakidzich (@JZDesign)
* Add `TestStoreSimulatedPurchaseError` error code (#2716) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `3f7fffc` to `25c7fb8` (#2721) via dependabot[bot] (@dependabot[bot])
* Disable offline entitlements in test store (#2718) via Toni Rico (@tonidero)
* Add E2E Test app for integration tests (#2715) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2714) via RevenueCat Git Bot (@RCGitBot)
* Bump fastlane-plugin-revenuecat_internal from `a8770fd` to `3f7fffc` (#2712) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `e555afb` to `a8770fd` (#2708) via dependabot[bot] (@dependabot[bot])
* Correct Opt In for FileRepository (#2709) via Jacob Rakidzich (@JZDesign)
* Paywalls -> Video Component (#2680) via Jacob Rakidzich (@JZDesign)
* Update fastlane-plugin-revenuecat_internal to e555afb (#2705) via Cesar de la Vega (@vegaro)
* [EXTERNAL] Fix incorrect exception use in test (#2674) via @nikit19 (#2703) via Toni Rico (@tonidero)
* Add CODEOWNERS (#2664) via Facundo Menzella (@facumenzella)
* Adds new tab ID properties to `TabsComponent`. (#2577) via JayShortway (@JayShortway)
* Bump fastlane-plugin-revenuecat_internal from `401d148` to `7508f17` (#2697) via dependabot[bot] (@dependabot[bot])
* Reimplement placeholder with Modifier.Node (#2677) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2667) via RevenueCat Git Bot (@RCGitBot)
* Bump fastlane-plugin-revenuecat_internal from `a6dc551` to `401d148` (#2683) via dependabot[bot] (@dependabot[bot])
* Bump rexml from 3.4.1 to 3.4.2 (#2676) via dependabot[bot] (@dependabot[bot])
* Add 8.24.0 CHANGELOG to main (#2662) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `489faef` to `a6dc551` (#2673) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `7d97553` to `489faef` (#2671) via dependabot[bot] (@dependabot[bot])
* File Repository (#2634) via Jacob Rakidzich (@JZDesign)
