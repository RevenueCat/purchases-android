## 9.19.4
### 🔄 Other Changes
* Add internal API to debug tracked events (#3054) via Toni Rico (@tonidero)
* [ADS-1] feat: add ad_format to AdTracker events (#3050) via Peter Porfy (@peterporfy)

## 9.19.2
## RevenueCat SDK
### 🐞 Bugfixes
* Make sure countdown component works when there's no package context (#3036) via Cesar de la Vega (@vegaro)
* Fix paywall data misattributions (#3032) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Add `sdk_originated` to POST /receipt (#3034) via Toni Rico (@tonidero)
* Finish activities instead of crashing when not launched through SDK (#3040) via Antonio Pallares (@ajpallares)
* Bump fastlane from 2.231.0 to 2.231.1 (#3035) via dependabot[bot] (@dependabot[bot])
* Update CLAUDE code review workflow reference (#3030) via Cesar de la Vega (@vegaro)
* Bump fastlane from 2.230.0 to 2.231.0 (#3028) via dependabot[bot] (@dependabot[bot])
* Ignore new headers in golden backend test snapshots (#3029) via Toni Rico (@tonidero)
* Add Claude Code GitHub Workflow (#3011) via Cesar de la Vega (@vegaro)
* Send `paywall_id` in paywall events (#3018) via Toni Rico (@tonidero)
* Add retry mechanism to purchases integration tests (#3024) via Toni Rico (@tonidero)

## 9.19.1
## RevenueCat SDK
### 🐞 Bugfixes
* Fixes incorrect lifecycle transitions in `CompatComposeView` (#3006) via JayShortway (@JayShortway)
* Fixes `showInAppMessages` NPE when the Activity has no content View (#3004) via JayShortway (@JayShortway)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Synchronize `cacheDirectory` lazy property to fix `NullPointerException` on `UnsafeLazyImpl`  (#3019) via Cesar de la Vega (@vegaro)
### Customer Center
#### 🐞 Bugfixes
* Simplify determining paid price in Customer Center (#2600) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Improve accuracy of transactions origin Part 6: Cleanup, Do not cache user id (#3012) via Toni Rico (@tonidero)
* Properly cleanup new shared preferences files between tests (#3016) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3015) via RevenueCat Git Bot (@RCGitBot)
* Improve accuracy of transactions origin Part 5: Cleanup (#3002) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 4: Post remaining transaction metadata when syncing purchases (#2993) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 3: Merge cached data when posting receipts + Cache amazon data (#2989) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 2: Store transaction metadata (#2988) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 1: Refactor to allow caching transaction metadata (#2987) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3010) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC] Update golden test files for backend integration tests (#3008) via RevenueCat Git Bot (@RCGitBot)
* Track exit offers (#2975) via Cesar de la Vega (@vegaro)

## 9.19.0
## RevenueCat SDK
### ✨ New Features
* Add Solar Engine integration support (#2994) via Lim Hoang (@limdauto)

### 🔄 Other Changes
* Incrase firebase tests timeout (#2997) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#2998) via RevenueCat Git Bot (@RCGitBot)

## 9.18.1
## RevenueCat SDK
### 📦 Dependency Updates
* [RENOVATE] Update dependency gradle to v8.14.3 (#2977) via RevenueCat Git Bot (@RCGitBot)

### 🔄 Other Changes
* Bump uri from 1.0.3 to 1.0.4 (#2984) via dependabot[bot] (@dependabot[bot])
* [AUTOMATIC] Update golden test files for backend integration tests (#2985) via RevenueCat Git Bot (@RCGitBot)
* Add better error message when opening SimulatedStoreErrorDialogActivity from non-supported sources (#2980) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#2978) via RevenueCat Git Bot (@RCGitBot)
* Bump fastlane-plugin-revenuecat_internal from `76a3a08` to `e6454e3` (#2976) via dependabot[bot] (@dependabot[bot])
* Improve error messages when displaying `CustomerCenterActivity` or `PaywallActivity` from other sources (#2974) via Toni Rico (@tonidero)
* Fix SDK size testing app version (#2973) via Toni Rico (@tonidero)
* Add size testing app and related CI tasks (#2956) via Toni Rico (@tonidero)

## 9.18.0
## RevenueCatUI SDK
### 🐞 Bugfixes
* Retry creation of font folder (#2965) via Cesar de la Vega (@vegaro)
### Paywallv2
#### ✨ New Features
* Add exit offers support to PaywallActivity and PaywallDialog (#2938) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Improve event tracking reliability (#2968) via Toni Rico (@tonidero)
* Use @ExperimentalPreviewRevenueCatPurchasesAPI for non subscription tracking API (#2962) via Pol Miro (@polmiro)

## 9.17.1
## RevenueCat SDK
### 🐞 Bugfixes
* Remove deprecated API usage (#2957) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fix crash when context.cacheDir is null in FontLoader (#2961) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Bump fastlane from 2.229.1 to 2.230.0 (#2951) via dependabot[bot] (@dependabot[bot])

## 9.17.0
## RevenueCat SDK
### ✨ New Features
* Adds `setAppsFlyerConversionData` to conveniently track AppsFlyer conversion data (#2931) via JayShortway (@JayShortway)
### 🐞 Bugfixes
* Make close() method also clear the shared instance of the SDK (#2940) via Toni Rico (@tonidero)
* Fix purchase callback not firing for DEFERRED product changes with baePlanId in oldProductId (#2937) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* [AUTOMATIC] Update golden test files for backend integration tests (#2949) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC] Update golden test files for backend integration tests (#2944) via RevenueCat Git Bot (@RCGitBot)
* Adds `.kotlin` to `.gitignore` (#2941) via JayShortway (@JayShortway)
* Dont use reflection to instantiate AmazonOfferingParser (#2934) via Will Taylor (@fire-at-will)

## 9.16.0

### 🔄 Other Changes
* Add AIDL to query purchase history inapp products (#2872) via Toni Rico (@tonidero)

## 9.15.5
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 📦 Dependency Updates
* [RENOVATE] Update build-dependencies to v8.13.2 (#2929) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix `IllegalStateException`: No `ViewModelStoreOwner` was provided via `LocalViewModelStoreOwner` in `CompatComposeView` (#2912) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Change AdFailedToLoad mediatorErrorCode type from Long to Int (#2924) via Pol Miro (@polmiro)
* Change paywall preview recorder name template (#2923) via Toni Rico (@tonidero)
* Don't use reflection to instantiate AmazonDeviceIdentifiersFetcher (#2919) via Will Taylor (@fire-at-will)

## 9.15.4
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fix custom schemes on paywall purchase buttons (#2914) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Bump fastlane-plugin-revenuecat_internal from `efca663` to `76a3a08` (#2915) via dependabot[bot] (@dependabot[bot])

## 9.15.3
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Fix missing events tracking by removing delay to flush when backgrounding app (#2907) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Update Golden File for Galaxy Store (#2902) via Will Taylor (@fire-at-will)
* Improve log message for no products of specific store in any offerings (#2897) via Antonio Pallares (@ajpallares)

## 9.15.2
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fix exception when buttons are hidden (#2889) via Cesar de la Vega (@vegaro)
* Fixes `ConcurrentModificationException` in `FontLoader` (#2885) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Add extra non subscription events (#2892) via Toni Rico (@tonidero)
* Show redacted Test Api key in alert when detected in Release build (#2891) via Antonio Pallares (@ajpallares)
* Remove data binding purchase tester + Update AGP (#2887) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2828) via RevenueCat Git Bot (@RCGitBot)
* Remove unused `BuildConfig. ENABLE_VIDEO_COMPONENT ` (#2884) via Toni Rico (@tonidero)
* Configure build logic and restructure gradle files (#2827) via Jaewoong Eum (@skydoves)

## 9.15.1
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix ANR when initializing FontLoader (#2819) via Cesar de la Vega (@vegaro)
### Paywallv2
#### 🐞 Bugfixes
* Fix `Template7CustomPackagesTestData` (#2875) via Cesar de la Vega (@vegaro)
* Fix predownloading of fonts if first offering doesn't have paywall components (#2873) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Extract parameters for non paid revenue tracking API to use objects (#2871) via Toni Rico (@tonidero)
* Bump fastlane from 2.229.0 to 2.229.1 (#2869) via dependabot[bot] (@dependabot[bot])

## 9.15.0
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Restore Purchases config automatically in CustomerCenter (#2867) via Facundo Menzella (@facumenzella)
* Handle error reading `errorStream` in some devices (#2865) via Toni Rico (@tonidero)
* [MON-1122] Revert variable rounding logic to not round up (#2857) via Pol Piella Abadia (@polpielladev)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Select default package on Sheet dismissal (#2861) via Cesar de la Vega (@vegaro)
### Customer Center
#### ✨ New Features
* CC-581 | Allow for support ticket creation (#2810) via Rosie Watson (@RosieWatson)

### 🔄 Other Changes
* Bump fastlane-plugin-revenuecat_internal from `7328ea7` to `efca663` (#2864) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.228.0 to 2.229.0 (#2863) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `083ced9` to `7328ea7` (#2862) via dependabot[bot] (@dependabot[bot])
* Runs plugin actions from correct directory (#2858) via JayShortway (@JayShortway)
* Flush multiple event batches (#2842) via Toni Rico (@tonidero)
* Add file size limit to events tracking files (#2841) via Toni Rico (@tonidero)
* Make events manager be supported in Android < 24 (#2854) via Toni Rico (@tonidero)
* Add non paid revenue reporting infra (#2728) via Toni Rico (@tonidero)
* Fix backend integration tests (#2860) via Toni Rico (@tonidero)
* Track `connection_error_reason` property in diagnostics (#2855) via Toni Rico (@tonidero)
* Uses some git+GitHub lanes from Fastlane plugin (#2856) via JayShortway (@JayShortway)
* Add client side timeout logic for endpoints that support fallback URLs (#2807) via Toni Rico (@tonidero)
* [EXTERNAL] Fix deprecation warnings in examples module (#2852) contributed by @gojoel (#2853) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `9f78bb9` to `083ced9` (#2848) via dependabot[bot] (@dependabot[bot])

## 9.14.1
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


### 🔄 Other Changes
* Add BC7 version of the SDKs  (#2839) via Toni Rico (@tonidero)

## 9.14.0
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

## 9.13.0
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* [Purchase Tester]: Fix toggling "Base Item" box sometimes programmatically enabling other "Buy Option" boxes (#2812) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add hook to delay payment screens (#2753) via Jacob Rakidzich (@JZDesign)
* Video Background (#2786) via Jacob Rakidzich (@JZDesign)
### Customer Center
#### 🐞 Bugfixes
* Fix dark mode in CustomerCenterView (#2817) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Fix integration tests (#2821) via Toni Rico (@tonidero)
* Add internal CustomerInfo source properties (#2796) via Toni Rico (@tonidero)

## 9.12.2
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Hotfix video component crash onPause (#2804) via Jacob Rakidzich (@JZDesign)
* Normalize product ids (#2797) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Adds `TestStoreErrorDialogActivity` (#2809) via JayShortway (@JayShortway)
* Update golden test files to have tests pass again (#2813) via Toni Rico (@tonidero)
* Wrap error when connecting to BillingClient (#2801) via Toni Rico (@tonidero)
* Use new fortress us-east-1 base url (#2805) via Toni Rico (@tonidero)
* Support fortress us-east-2 integration tests (#2793) via Toni Rico (@tonidero)
* Add test backend environment parameter to integration tests (#2792) via Toni Rico (@tonidero)
* Unify integration tests jobs (#2791) via Toni Rico (@tonidero)
* Update CHANGELOG to reflect where changes actually happened in 9.12.0 and 9.12.1 (#2798) via Toni Rico (@tonidero)
* Add extra request logging (#2794) via Toni Rico (@tonidero)
* Fix golden backend integration tests (#2795) via Toni Rico (@tonidero)

## 9.12.1
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Remove GOOGLE_PRODUCT_MISSING_INFINITELY_RECURRING_BILLING_PHASE log message (#2767) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Invalidate CustomerInfo cache when opening a WPB in paywalls (#2785) via Toni Rico (@tonidero)
* FIX: Video component - felt slow on first appearance (#2784) via Jacob Rakidzich (@JZDesign)
* Add package fallback to tab selection when there's no default selected (#2782) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add golden tests for requests and responses in backend integration tests (#2781) via Toni Rico (@tonidero)
* Unlock entitlements from add-ons in offline entitlements mode (#2776) via Will Taylor (@fire-at-will)
* Add fallback URL integration tests (#2769) via Toni Rico (@tonidero)
* [SDK-4152] Use static fallback urls (#2780) via Antonio Pallares (@ajpallares)
* Skip tests not supported in fortress (#2783) via Toni Rico (@tonidero)
* Keep cache logic similar to iOS + ensure path extension is on file (#2750) via Jacob Rakidzich (@JZDesign)
* Use cached offerings on network errors (#2772) via Toni Rico (@tonidero)
* [EXTERNAL] Update Emerge snapshots to best/latest practices (#2777) by @rbro112 (#2779) via Toni Rico (@tonidero)
* Change ForceServerErrorStrategy to be a `fun interface` to simplify instantiation (#2773) via Toni Rico (@tonidero)
* Stores backend integration test result on CircleCI (#2771) via JayShortway (@JayShortway)
* Change test server down mechanism to use new server down endpoint (#2765) via Toni Rico (@tonidero)
* [CAT-2067 EXPERIMENTAL]: Add-On Subscription Options (#2763) via Will Taylor (@fire-at-will)
* Avoids the `PaywallComponentsTemplate_Preview` from being rendered by Emerge (#2761) via JayShortway (@JayShortway)
* Handle error building `QueryProductDetailsParams` in some devices (#2764) via Toni Rico (@tonidero)
* Add more proper error message when using test store API key in release builds (#2762) via Toni Rico (@tonidero)

## 9.12.0
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Add webCheckoutURL property to Offering and Package (#2747) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Use the fallback endpoint in more failure scenarios (#2754) via JayShortway (@JayShortway)
* Protect against developers excluding blockstore dependency (#2745) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add support for Web Purchase buttons in paywalls (#2751) via Toni Rico (@tonidero)
### Customer Center
#### 🐞 Bugfixes
* Add elevation to surfaces in Customer Center to fix default appearance (#2741) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [Experimental]: Support Multi-Line Subscriptions (#2668) via Will Taylor (@fire-at-will)
* Add PurchaseButtonComponent WPL field deserialization (#2749) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `25c7fb8` to `525d48c` (#2748) via dependabot[bot] (@dependabot[bot])
* Add extra message test store dialog (#2746) via Toni Rico (@tonidero)

## 9.11.0
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Adds new experimental `storefrontLocale` APIs. (#2726) via JayShortway (@JayShortway)

## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Fixes more Icons crashing with `NoClassDefFoundError` when using Material 1.4.0 (#2732) via Cesar de la Vega (@vegaro)

## 9.10.0
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Turn on Video Component (#2725) via Jacob Rakidzich (@JZDesign)

## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Fix potential `NoClassDefFoundError` crash finding `Icons` caused by material3 1.4.0  (#2727) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add test store maestro test (#2711) via Toni Rico (@tonidero)
* Finalize video component to include a checksum, and optimize memory usage for large file downloads (#2717) via Jacob Rakidzich (@JZDesign)

## 9.9.0
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

## 9.8.1
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

## 9.8.0
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Add Airbridge device ID subscriber attribute (#2696) via Lim Hoang (@limdauto)
* Enable Test Store (#2685) via Antonio Pallares (@ajpallares)

## RevenueCatUI SDK
### ✨ New Features
* Expose `launch` and `launchIfNeeded` to Java (#2695) via Cesar de la Vega (@vegaro)
### Customer Center
#### 🐞 Bugfixes
* Fix Expired badge in Customer Center dark mode (#2693) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add CODEOWNERS (#2664) via Facundo Menzella (@facumenzella)
* Adds new tab ID properties to `TabsComponent`. (#2577) via JayShortway (@JayShortway)
* Bump fastlane-plugin-revenuecat_internal from `401d148` to `7508f17` (#2697) via dependabot[bot] (@dependabot[bot])
* Reimplement placeholder with Modifier.Node (#2677) via Jaewoong Eum (@skydoves)
* Remove unused kotlinCompilerExtensionVersion (#2691) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2667) via RevenueCat Git Bot (@RCGitBot)

## 9.7.2
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Fix promo offer button CTA in Customer Center (#2682) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Bump fastlane-plugin-revenuecat_internal from `a6dc551` to `401d148` (#2683) via dependabot[bot] (@dependabot[bot])
* Bump rexml from 3.4.1 to 3.4.2 (#2676) via dependabot[bot] (@dependabot[bot])

## 9.7.1
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fix bundled font families when using assets folder (#2672) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Add 8.24.0 CHANGELOG to main (#2662) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `489faef` to `a6dc551` (#2673) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `7d97553` to `489faef` (#2671) via dependabot[bot] (@dependabot[bot])
* File Repository (#2634) via Jacob Rakidzich (@JZDesign)

## 9.7.0
## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Display Virtual Currencies in Customer Center (#2652) via Will Taylor (@fire-at-will)

## 9.6.0
## RevenueCat SDK
### ✨ New Features
* Add `RevenueCatBackupAgent` (#2625) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Fix CoroutineCreationDuringComposition lint error on AGP 8.13.0 (#2659) via Cesar de la Vega (@vegaro)
* Support setting null offering id on PaywallView (#2658) via Toni Rico (@tonidero)

## 9.5.0
## RevenueCat SDK
### ✨ New Features
* Add preferred UI locale override for RevenueCat UI components (#2620) via Josh Holtz (@joshdholtz)

### 🔄 Other Changes
* Improve thread safety of setting paywalls preferred locale (#2655) via Josh Holtz (@joshdholtz)
* Remove validation for no packages on paywalls (#2653) via Josh Holtz (@joshdholtz)
* Video Component Models (dark code) (#2646) via Jacob Rakidzich (@JZDesign)
* [EXTERNAL] docs: fixed a typo on documentation about `Purchases.awaitPurchase` by @matteinn in #2593 (#2651) via Toni Rico (@tonidero)
* Add warning with 9.x issues to all versions since 9.0.0 in CHANGELOG (#2650) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2647) via RevenueCat Git Bot (@RCGitBot)
* Delete CLAUDE.md (#2648) via Cesar de la Vega (@vegaro)
* MON-1193 flatten Transition JSON structure after chatting more thoroughly with team (#2641) via Jacob Rakidzich (@JZDesign)

## 9.4.0
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### ✨ New Features
* Add option to disable automatic ID collection when setting attribution network IDs at configuration time (#2643) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Handle payment pending errors when restoring properly (#2635) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* MON-1193 Support delayed close button (Component Transitions) (#2623) via Jacob Rakidzich (@JZDesign)
#### 🐞 Bugfixes
* Fix PaywallDialog going over screen size on Android 35+ (#2642) via Toni Rico (@tonidero)
### Customer Center
#### ✨ New Features
* Add button_text to ScreenOffering (#2638) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* Chore: Update detekt yml file (#2637) via Jacob Rakidzich (@JZDesign)
* Update CHANGELOG for version 8.23.0 release (#2636) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2633) via RevenueCat Git Bot (@RCGitBot)

## 9.3.2
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 🐞 Bugfixes
* Fix embedded font family loading (#2624) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Fix integration tests shared preferences file (#2629) via Toni Rico (@tonidero)
* Migrate RC SharedPreferences data to a separate file (#2621) via Toni Rico (@tonidero)
* Add annotation experimental library to debug builds (#2617) via Toni Rico (@tonidero)

## 9.3.1
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 🐞 Bugfixes
* Use `Block store` to backup anonymous user ids across installations (#2595) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fixes price formatting discrepancies on Paywalls for `{{ product.price_per_[day|week|month|year] }}` (#2604) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Revert dokka 2 and gradle 9 update (#2618) via Toni Rico (@tonidero)
* Introduce runtime annotations library and add stability annotations for increasing UI performances (#2608) via Jaewoong Eum (@skydoves)
* Override presented offering context paywalls without offering (#2612) via Toni Rico (@tonidero)
* Add APIs for hybrid SDKs to set presentedOfferingContext (#2610) via Toni Rico (@tonidero)
* Bump Baseline Profiles to 1.4.0 and update profiles (#2611) via Jaewoong Eum (@skydoves)
* Migrate deprecated kotlinOptions to compilerOptions (#2607) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2613) via RevenueCat Git Bot (@RCGitBot)
* Migrate amazon & debugview modules to KTS (#2327) via Jaewoong Eum (@skydoves)
* Update to Dokka 2.0.0 (#2609) via Toni Rico (@tonidero)
* Add log when restoring purchases finds no purchases with some troubleshooting (#2599) via Toni Rico (@tonidero)

## 9.3.0
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 📦 Dependency Updates
* [RENOVATE] Update dependency gradle to v9 (#2589) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Show a subscribe button in customer center when there are no subscriptions (#2596) via Facundo Menzella (@facumenzella)
#### 🐞 Bugfixes
* Fix title and price of non-Google purchases in Customer Center (#2576) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2598) via RevenueCat Git Bot (@RCGitBot)
* Add alias users backend call (#2594) via Toni Rico (@tonidero)
* Rename TestStore to SimulatedStore (#2591) via Toni Rico (@tonidero)
* Fail configure call if using test api key in release builds (#2590) via Toni Rico (@tonidero)
* Support test store (#2554) via Toni Rico (@tonidero)
* Fix insets in Paywall Tester (#2584) via Cesar de la Vega (@vegaro)
* Add 8.22.1 to CHANGELOG (#2582) via Toni Rico (@tonidero)
* Add warning on V9 CHANGELOG (#2586) via Toni Rico (@tonidero)
* Change metalava job to diff dump generated dump files (#2585) via Toni Rico (@tonidero)

## 9.2.0
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 🐞 Bugfixes
* Add lifetime badge to customer center (#2565) via Cesar de la Vega (@vegaro)
### Customer Center
#### ✨ New Features
* Introduce custom actions for customer center (#2572) via Facundo Menzella (@facumenzella)

## RevenueCatUI SDK
### ✨ New Features
* Add support for az-AZ locale (#2575) via Franco Correa (@francocorreasosa)
### Customer Center
#### 🐞 Bugfixes
* Display latest expired subscription if no active subscriptions (#2564) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Expose getStorefront APIs in CustomEntitlementComputation flavor (#2579) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2578) via RevenueCat Git Bot (@RCGitBot)
* Add missing dokka-hide-internal tests to CI and fix reported number of tests  (#2569) via Cesar de la Vega (@vegaro)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2574) via RevenueCat Git Bot (@RCGitBot)
* Convert WebBilling products into TestStoreProducts (#2573) via Toni Rico (@tonidero)
* Add WebBillingGetProducts endpoint (#2571) via Toni Rico (@tonidero)
* Remove TestStoreProduct purchase check (#2570) via Toni Rico (@tonidero)
* Improve 9.0.0 changelog (#2568) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2567) via RevenueCat Git Bot (@RCGitBot)
* Parallel test jobs (#2549) via Cesar de la Vega (@vegaro)

## 9.1.2
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 🐞 Bugfixes
* Resubscribe if cancelled (#2487) via Cesar de la Vega (@vegaro)
### Virtual Currency
#### 🐞 Bugfixes
* Update VC Caching Log Message (#2552) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Update design of No Active subscriptions screen in Customer Center (#2559) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2561) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2560) via RevenueCat Git Bot (@RCGitBot)
* Bump danger from 9.5.1 to 9.5.3 (#2556) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.227.2 to 2.228.0 (#2557) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `9a29f63` to `7d97553` (#2558) via dependabot[bot] (@dependabot[bot])
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2555) via RevenueCat Git Bot (@RCGitBot)
* Bump nokogiri from 1.18.8 to 1.18.9 (#2553) via dependabot[bot] (@dependabot[bot])
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2551) via RevenueCat Git Bot (@RCGitBot)
* Fix paywall tester so customer center doesn't close when changing to dark mode (#2550) via Cesar de la Vega (@vegaro)

## 9.1.1
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 🔄 Other Changes
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2544) via RevenueCat Git Bot (@RCGitBot)
* Expose Virtual Currency Constructors with @InternalRevenueCatAPI (#2543) via Will Taylor (@fire-at-will)
* Expose API key validation result to BillingFactory (#2542) via Toni Rico (@tonidero)

## 9.1.0
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### Virtual Currency
#### ✨ New Features
* Virtual Currency Support (#2519) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* PaywallActivityLauncher: Add `edgeToEdge` parameter to display paywall in full screen (#2530) via Toni Rico (@tonidero)
#### 🐞 Bugfixes
* Remove logic to avoid repurchasing already subscribed products (#2492) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Dont run VC tests on load shedder integration tests (#2538) via Will Taylor (@fire-at-will)
* Introduces `CompatComposeView` to handle scenarios where the view tree is not set up (#2527) via JayShortway (@JayShortway)

## 9.0.1
## RevenueCat SDK
> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

### 🐞 Bugfixes
* Fix upgrades/downgrades after BC8 update (#2532) via Toni Rico (@tonidero)
### 📦 Dependency Updates
* [RENOVATE] Update build-dependencies (#2515) via RevenueCat Git Bot (@RCGitBot)
* [RENOVATE] Update build-dependencies (#2472) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Make PaywallActivity not consume insets in Android 16+ (#2520) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Add CLAUDE.md (#2522) via Cesar de la Vega (@vegaro)
* Update kotlin version in README (#2524) via Cesar de la Vega (@vegaro)
* Increase compile/target SDK version to 35 (#2525) via Toni Rico (@tonidero)
* Fix snapshot deployment (#2526) via Toni Rico (@tonidero)
* Update CircleCI android orb (#2521) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2517) via RevenueCat Git Bot (@RCGitBot)
* Improve v9 migration docs (#2514) via Toni Rico (@tonidero)
* Bump AGP to 8.10.1 and Gradle to 8.14.2 (#2440) via Jaewoong Eum (@skydoves)
* Fixes compilation of the CEC sample app. (#2512) via JayShortway (@JayShortway)

## 9.0.0
## RevenueCat SDK

> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.

This release updates the SDK to use Google Play Billing Library 8. This version of the Billing Library removed APIs to query for expired subscriptions and consumed one-time products, aside from other improvements. You can check the full list of changes here: https://developer.android.com/google/play/billing/release-notes#8-0-0

Additionally, we've also updated Kotlin to 2.0.21 and our new minimum version is Kotlin 1.8.0+. If you were using an older version of Kotlin, you will need to update it.

Regarding API changes, we've also removed data classes from our public APIs. This means that for classes that were previously data classes, the `copy` function and `componentN` functions (destructuring declarations) have been removed. `equals` and `hashCode` functions still work as before.

### Play Billing Library 8: No expired subscriptions or consumed one-time products

Play Billing Library 8 removed the ability to query for expired subscriptions and consumed one-time products. This means that the RevenueCat SDK will no longer be able to send purchase information from these purchases. There are 2 cases where this can have an impact:
- If you have consumed one time purchases in Google Play that need to be restored (for example, in order to grant a lifetime entitlement). In these cases, the SDK will not be able to find these purchases and will not be able to restore them and grant the entitlements. This can especially be a problem if you're using anonymous ids and don't have your own account system. Please make sure your products are correctly configured as non-consumables in the RevenueCat dashboard to avoid consuming them in the first place if you intend to make them behave as lifetime purchases.
- (Only relevant if you recently integrated RevenueCat before upgrading to v9, and do not (yet) have all your transactions imported). The SDK will not be able to send purchase information from these expired subscriptions and consumed one time purchases to our backend, so we might miss this data in our customer profile/targeting. We can still ingest historical data from these purchases through a backend historical import. See [docs](https://www.revenuecat.com/docs/migrating-to-revenuecat/migrating-existing-subscriptions). This case doesn't affect developers that have all transactions in RevenueCat, which is true for the vast majority.

### Bumped minimum Kotlin version

RevenueCat SDK v9 bumps Kotlin to 2.0.21, with a minimum Kotlin version of 1.8.0.

### Using the SDK with your own IAP code (previously Observer Mode)

Using the SDK with your own IAP code is still supported in v9. Other than updating the SDK version, there are no changes required. Just make sure the version of the Play Billing Library is also version 8.0.0+.

### 💥 Breaking Changes
* Removes data classes from public API (#2498) via JayShortway (@JayShortway)
* Marks `PaywallData` and `PaywallColor` as `InternalRevenueCatAPI`. (#2507) via JayShortway (@JayShortway)
* BC8 migration (#2506) via Toni Rico (@tonidero)
* Update to kotlin 2.0.21 while keeping language compatibility (#2493) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Update CustomEntitlementComputation sample app kotlin version (#2510) via Toni Rico (@tonidero)
* Fix `Switch` component previews (#2509) via Toni Rico (@tonidero)
* Add V9 migration guide (#2508) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2499) via RevenueCat Git Bot (@RCGitBot)

## 8.25.0
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

## 8.24.0
## RevenueCat SDK
> [!NOTE]  
> This release brings all non-breaking changes between SDK versions 9.4.0 and 9.6.0 to major 8, so developers that don't/can't use major 9, can still get the latest updates.

### ✨ New Features
* Add `RevenueCatBackupAgent` (#2625) via Toni Rico (@tonidero)
* Add preferred UI locale override for RevenueCat UI components (#2620) via Josh Holtz (@joshdholtz)
* Add option to disable automatic ID collection when setting attribution network IDs at configuration time (#2643) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Handle payment pending errors when restoring properly (#2635) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* MON-1193 Support delayed close button (Component Transitions) (#2623) via Jacob Rakidzich (@JZDesign)
#### 🐞 Bugfixes
* Fix PaywallDialog going over screen size on Android 35+ (#2642) via Toni Rico (@tonidero)
### Customer Center
#### ✨ New Features
* Add button_text to ScreenOffering (#2638) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* Fix CoroutineCreationDuringComposition lint error on AGP 8.13.0 (#2659) via Cesar de la Vega (@vegaro)
* Support setting null offering id on PaywallView (#2658) via Toni Rico (@tonidero)
* Improve thread safety of setting paywalls preferred locale (#2655) via Josh Holtz (@joshdholtz)
* Remove validation for no packages on paywalls (#2653) via Josh Holtz (@joshdholtz)
* MON-1193 flatten Transition JSON structure after chatting more thoroughly with team (#2641) via Jacob Rakidzich (@JZDesign)

## 8.23.0
> [!NOTE]  
> This release brings all non-breaking changes between SDK versions 9.0.0 and 9.3.2 to major 8, so developers that don't/can't use major 9, can still get the latest updates.

## RevenueCat SDK
### ✨ New Features
* Virtual Currency Support (#2519) via Will Taylor (@fire-at-will)
### 🐞 Bugfixes
* Use `Block store` to backup anonymous user ids across installations (#2595) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add support for az-AZ locale (#2575) via Franco Correa (@francocorreasosa)
* PaywallActivityLauncher: Add `edgeToEdge` parameter to display paywall in full screen (#2530) via Toni Rico (@tonidero)
#### 🐞 Bugfixes
* Fixes price formatting discrepancies on Paywalls for `{{ product.price_per_[day|week|month|year] }}` (#2604) via JayShortway (@JayShortway)
* Remove logic to avoid repurchasing already subscribed products (#2492) via Toni Rico (@tonidero)
* Make PaywallActivity not consume insets in Android 16+ (#2520) via Toni Rico (@tonidero)

### Customer Center
#### ✨ New Features
* Show a subscribe button in customer center when there are no subscriptions (#2596) via Facundo Menzella (@facumenzella)
* Introduce custom actions for customer center (#2572) via Facundo Menzella (@facumenzella)
#### 🐞 Bugfixes
* Fix title and price of non-Google purchases in Customer Center (#2576) via Cesar de la Vega (@vegaro)
* Add lifetime badge to customer center (#2565) via Cesar de la Vega (@vegaro)
* Display latest expired subscription if no active subscriptions (#2564) via Cesar de la Vega (@vegaro)
* Resubscribe if cancelled (#2487) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Override presented offering context paywalls without offering (#2612) via Toni Rico (@tonidero)
* Add APIs for hybrid SDKs to set presentedOfferingContext (#2610) via Toni Rico (@tonidero)
* Bump Baseline Profiles to 1.4.0 and update profiles (#2611) via Jaewoong Eum (@skydoves)
* Migrate deprecated kotlinOptions to compilerOptions (#2607) via Jaewoong Eum (@skydoves)
* Migrate amazon & debugview modules to KTS (#2327) via Jaewoong Eum (@skydoves)
* Add log when restoring purchases finds no purchases with some troubleshooting (#2599) via Toni Rico (@tonidero)
* Add alias users backend call (#2594) via Toni Rico (@tonidero)
* Rename TestStore to SimulatedStore (#2591) via Toni Rico (@tonidero)
* Fail configure call if using test api key in release builds (#2590) via Toni Rico (@tonidero)
* Support test store (#2554) via Toni Rico (@tonidero)
* Fix insets in Paywall Tester (#2584) via Cesar de la Vega (@vegaro)
* Change metalava job to diff dump generated dump files (#2585) via Toni Rico (@tonidero)
* Expose getStorefront APIs in CustomEntitlementComputation flavor (#2579) via Toni Rico (@tonidero)
* Add missing dokka-hide-internal tests to CI and fix reported number of tests  (#2569) via Cesar de la Vega (@vegaro)
* Convert WebBilling products into TestStoreProducts (#2573) via Toni Rico (@tonidero)
* Add WebBillingGetProducts endpoint (#2571) via Toni Rico (@tonidero)
* Remove TestStoreProduct purchase check (#2570) via Toni Rico (@tonidero)
* Parallel test jobs (#2549) via Cesar de la Vega (@vegaro)
* Update VC Caching Log Message (#2552) via Will Taylor (@fire-at-will)
* Update design of No Active subscriptions screen in Customer Center (#2559) via Cesar de la Vega (@vegaro)
* Bump danger from 9.5.1 to 9.5.3 (#2556) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.227.2 to 2.228.0 (#2557) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `9a29f63` to `7d97553` (#2558) via dependabot[bot] (@dependabot[bot])
* Bump nokogiri from 1.18.8 to 1.18.9 (#2553) via dependabot[bot] (@dependabot[bot])
* Fix paywall tester so customer center doesn't close when changing to dark mode (#2550) via Cesar de la Vega (@vegaro)
* Expose Virtual Currency Constructors with @InternalRevenueCatAPI (#2543) via Will Taylor (@fire-at-will)
* Expose API key validation result to BillingFactory (#2542) via Toni Rico (@tonidero)
* Dont run VC tests on load shedder integration tests (#2538) via Will Taylor (@fire-at-will)
* Introduces `CompatComposeView` to handle scenarios where the view tree is not set up (#2527) via JayShortway (@JayShortway)
* Increase compile/target SDK version to 35 (#2525) via Toni Rico (@tonidero)
* Fix snapshot deployment (#2526) via Toni Rico (@tonidero)
* Update CircleCI android orb (#2521) via Toni Rico (@tonidero)
* Fixes compilation of the CEC sample app. (#2512) via JayShortway (@JayShortway)
* Update CustomEntitlementComputation sample app kotlin version (#2510) via Toni Rico (@tonidero)
* Fix `Switch` component previews (#2509) via Toni Rico (@tonidero)

## 8.22.1
### 🔄 Other Changes
* Expose getStorefront APIs in CustomEntitlementComputation flavor (#2579)

## 8.22.0
## RevenueCat SDK
### ✨ New Features
* feat(purchases): Add setPostHogUserId() method to Purchases API (#2495) via Hussain Mustafa (@hussain-mustafa990)
### 🐞 Bugfixes
* Improves button progress indicator size calculation. (#2485) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Revert "BC8 migration (#2477)" (#2501) via Toni Rico (@tonidero)
* Add codelab instructions on README file (#2489) via Jaewoong Eum (@skydoves)
* Use collectAsStateWithLifecycle instead of collectAsState in Compose (#2488) via Jaewoong Eum (@skydoves)
* Improve Composable stabilities (#2478) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2486) via RevenueCat Git Bot (@RCGitBot)
* BC8 migration (#2477) via Toni Rico (@tonidero)
* Fixes building sample apps with SNAPSHOT dependencies (#2483) via JayShortway (@JayShortway)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2484) via RevenueCat Git Bot (@RCGitBot)

## 8.21.0
## RevenueCat SDK
### ✨ New Features
* Add `managementURL` to `SubscriptionInfo` (#2468) via Cesar de la Vega (@vegaro)
### 🐞 Bugfixes
* Use subscription's managementURL instead of CustomerInfo's in Customer Center (#2473) via Cesar de la Vega (@vegaro)
* Issue when loading promotional offers when products are missing base plans (#2471) via Cesar de la Vega (@vegaro)
* Purchases the right package when a purchase button is inside a package component (#2469) via JayShortway (@JayShortway)
* fix potential infinite loop in paywalls (#2462) via Antonio Pallares (@ajpallares)

## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Support multi purchases in Customer Center (#2431) via Cesar de la Vega (@vegaro)
#### 🐞 Bugfixes
* Use Material TopAppBar in Customer Center (#2459) via Cesar de la Vega (@vegaro)
* Redo navigation in Customer Center (#2458) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Migrates publishing to Maven Central Portal. (#2476) via JayShortway (@JayShortway)
* Introduce the Compose compiler metrics option for the ui package (#2474) via Jaewoong Eum (@skydoves)
* Update workflows/issue-notifications.yml@v2 (#2475) via Josh Holtz (@joshdholtz)
* Configure Renovate (#2470) via RevenueCat Git Bot (@RCGitBot)
* Add GitHub Issue Action Ack (#2467) via Josh Holtz (@joshdholtz)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2448) via RevenueCat Git Bot (@RCGitBot)
* Improve paywall error logs (#2461) via Toni Rico (@tonidero)
* [EXTERNAL] Improve logger performance with message lambdas (#2456) via @landarskiy (#2460) via JayShortway (@JayShortway)
* [Paywalls] Ignores `sheet.background` property in schema (#2451) via JayShortway (@JayShortway)

## 8.20.0
## RevenueCat SDK
### 🐞 Bugfixes
* Fixes `OutOfMemoryError` caused by printing large `Offerings` objects. (#2452) via JayShortway (@JayShortway)
* [Paywalls] Margin no longer decreases icon size (#2447) via JayShortway (@JayShortway)
* `getOfferings()` returns fresh Offerings and Products when the locale has changed (#2438) via JayShortway (@JayShortway)

## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Redo Customer Center subscription card and action buttons (#2430) via Cesar de la Vega (@vegaro)
#### 🐞 Bugfixes
* Clean up paths logic in Customer Center (#2439) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [Paywalls] Tab toggle supports gradient colors (#2453) via JayShortway (@JayShortway)
* Introduce Baseline Profiles for increasing SDK startup performance (#2437) via Jaewoong Eum (@skydoves)
* Support spaces in template IDs when recording Paywall screenshots (#2449) via JayShortway (@JayShortway)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2444) via RevenueCat Git Bot (@RCGitBot)
* Use script instead of gradle for metalava apiDump and apiCheck (#2445) via Cesar de la Vega (@vegaro)
* Clean up `ExperimentalPreviewRevenueCatPurchasesAPI` (#2434) via Cesar de la Vega (@vegaro)
* Remove assertion on strings in Customer Center test (#2433) via Cesar de la Vega (@vegaro)
* Some Metalava updates (#2442) via JayShortway (@JayShortway)
* [Paywalls] Ensures bottom window insets are applied to the sheet. (#2441) via JayShortway (@JayShortway)
* [Paywalls] Buttons can now open sheets (#2436) via JayShortway (@JayShortway)
* [Paywalls] Correctly deserializes the `Sheet` destination in Button components (#2435) via JayShortway (@JayShortway)
* Include commit hash in commit message for paywall-rendering-validation (#2432) via Antonio Pallares (@ajpallares)
* Update `PaywallTester` colors (#2429) via Cesar de la Vega (@vegaro)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2426) via RevenueCat Git Bot (@RCGitBot)
* Bump fastlane from 2.227.1 to 2.227.2 (#2392) via dependabot[bot] (@dependabot[bot])

## 8.19.2
## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls] Fix Bold text in Markdown on higher Weights Text composables (#2421) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Add coroutines dependency to core SDK (#2423) via Toni Rico (@tonidero)
* [Paywalls] Add `font_weight_int` to `TextComponent` (#2419) via Toni Rico (@tonidero)
* [Paywalls V2] Downloadable fonts (#2414) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2412) via RevenueCat Git Bot (@RCGitBot)
* Use new resource format for cross validation screenshots and fix lint (#2413) via Josh Holtz (@joshdholtz)

## 8.19.1
## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Fix display of wrong product information when not using entitlements (#2415) via Cesar de la Vega (@vegaro)

## 🔄 Other Changes
* Update Java in .sdkmanrc to 21.0.6 (#2395) via Cesar de la Vega (@vegaro)

## 8.19.0
## RevenueCat SDK
### ✨ New Features
* Add Paddle Store Type (#2353) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls V2] Support missing packages in paywalls (#2408) via Toni Rico (@tonidero)

### 🔄 Other Changes
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2410) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2407) via RevenueCat Git Bot (@RCGitBot)

## 8.18.0
## RevenueCat SDK
### 🐞 Bugfixes
* Fix `Transaction` `isSandbox` parsing (#2404) via Toni Rico (@tonidero)
* Fix downgrade of constraint layout version (#2396) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls v2] The `ButtonComponentView`'s progress indicator now takes margin into account (#2401) via JayShortway (@JayShortway)
* Correctly translates variables when a fuzzy match is required. (#2402) via JayShortway (@JayShortway)
### Customer Center
#### ✨ New Features
* Add support for cross product promotional offers (#2385) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2403) via RevenueCat Git Bot (@RCGitBot)
* [Paywalls V2] Support stroke color and width in Carousel indicator (#2398) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2400) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2397) via RevenueCat Git Bot (@RCGitBot)
* Use SideEffect for refreshing vm states inside InternalPaywall (#2393) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2394) via RevenueCat Git Bot (@RCGitBot)
* Bump nokogiri from 1.18.6 to 1.18.8 (#2371) via dependabot[bot] (@dependabot[bot])
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2390) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2388) via RevenueCat Git Bot (@RCGitBot)

## 8.17.1
## RevenueCat SDK
### 🐞 Bugfixes
* Handles configuring while the device is locked (#2077) via JayShortway (@JayShortway)
* [EXTERNAL] fix: Optional property deserialisation error (#2376) by @thomas-coldwell (#2377) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls v2] Avoid image cropping when combining single-sided margin with Fixed size constraint (#2380) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Fix pre commit hook (#2384) via Cesar de la Vega (@vegaro)
* Fixes pre-commit script (#2386) via JayShortway (@JayShortway)
* Update pre-commit hooks rule for checking metalava (#2378) via Jaewoong Eum (@skydoves)
* [Paywalls v2] Adds design as reviewer of the paywall template screenshot PRs. (#2383) via JayShortway (@JayShortway)
* [Diagnostics] add host parameter to http_request_performed event (#2369) via Antonio Pallares (@ajpallares)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2381) via RevenueCat Git Bot (@RCGitBot)
* Use fallback API hosts when receiving server down response (#2368) via Antonio Pallares (@ajpallares)
* Fixes a typo: matalava -> metalava. (#2375) via JayShortway (@JayShortway)
* Introduce Metalava for tracking the public API surfaces (#2370) via Jaewoong Eum (@skydoves)
* Add Purchases Coroutine extensions for Result (#2358) via Jaewoong Eum (@skydoves)
* [Paywalls v2] Supports purchase buttons inside packages (#2372) via JayShortway (@JayShortway)
* [Paywalls v2] Adds some more tests asserting top window insets behavior. (#2374) via JayShortway (@JayShortway)
* Do not fail the build if `local.properties` does not exist. (#2373) via JayShortway (@JayShortway)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2342) via RevenueCat Git Bot (@RCGitBot)
* [Paywalls V2] Avoids re-parsing the component tree to handle window insets (#2258) via JayShortway (@JayShortway)
* Migrate Constants to local.properties for the custom entitlement sample project (#2357) via Jaewoong Eum (@skydoves)
* Adjusts precision of some Emerge Snapshots (#2348) via JayShortway (@JayShortway)
* [Paywalls v2] Records and pushes template screenshots to paywall-rendering-validation (#2365) via JayShortway (@JayShortway)
* Remove redundant Kotlin bom dependencies (#2363) via Jaewoong Eum (@skydoves)

## 8.17.0
## RevenueCat SDK
### ✨ New Features
* Add async storefront country code apis (#2361) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Product name truncation issue in Customer Center on Android (#2364) via Cesar de la Vega (@vegaro)

## 8.16.1
## RevenueCat SDK
### 🐞 Bugfixes
* Add missing response fields to `CustomerInfo`. Including transaction Prices (#2128) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls v2] Fixes timeline layout when width is Fit (#2354) via JayShortway (@JayShortway)
* [Paywalls V2] Improves fuzzy matching locale when the region doesn't match (#2355) via JayShortway (@JayShortway)
* [Paywalls V2] Norwegian Bokmål and Norwegian Nynorsk fall back to Norwegian. (#2329) via JayShortway (@JayShortway)
### Customer Center
#### 🐞 Bugfixes
* Clean up on restoring functionality in customer center (#2316) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [Paywalls v2] Adds the unsupported Background type to the error message (#2350) via JayShortway (@JayShortway)
* [Paywalls v2] Uses a fixed date for template previews to avoid daily changes. (#2351) via JayShortway (@JayShortway)
* [Paywalls v2] Adds a default case to `Background` and `ButtonComponent` (#2347) via JayShortway (@JayShortway)
* [Paywalls v2] Improves `PaywallComponentsTemplatePreviewRecorder` stability (#2352) via JayShortway (@JayShortway)
* [Paywalls v2] Adds a default case to most enums and sealed types (#2346) via JayShortway (@JayShortway)
* Adds `EnumDeserializerWithDefault` and `SealedDeserializerWithDefault` (#2345) via JayShortway (@JayShortway)
* [Paywalls V2] Renders template previews in a fixed resolution using Paparazzi (#2214) via JayShortway (@JayShortway)
* Bump fastlane from 2.227.0 to 2.227.1 (#2344) via dependabot[bot] (@dependabot[bot])
* Migrate root gradle file to KTS (#2343) via Jaewoong Eum (@skydoves)
* Migrate tester modules to KTS format (#2340) via Jaewoong Eum (@skydoves)
* Introduce purchases-bom package (#2339) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2338) via RevenueCat Git Bot (@RCGitBot)
* [Diagnostics] Removes Android 7 requirement (#2335) via JayShortway (@JayShortway)
* [Paywalls v2] Fixes `update-paywall-preview-resources-submodule` CI job (#2337) via JayShortway (@JayShortway)
* Make purchases module to transitive dependency for the ui module (#2334) via Jaewoong Eum (@skydoves)
* Migrate settings.gradle to KTS and add the dependency resolutions (#2328) via Jaewoong Eum (@skydoves)

## 8.16.0
## RevenueCat SDK
### 📦 Dependency Updates
* Remove redundant kotlin stdlib dependencies (#2310) via Jaewoong Eum (@skydoves)
### Customer Center
#### 🐞 Bugfixes
* fix: Check eligible for promo offers (#2325) via Facundo Menzella (@facumenzella)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* [Paywalls v2] Supports custom fonts in `assets/fonts` (#2320) via JayShortway (@JayShortway)
#### 🐞 Bugfixes
* [Paywalls v2] Ignores already-consumed window insets. (#2326) via JayShortway (@JayShortway)
### Customer Center
#### 🐞 Bugfixes
* Reload customer center after successful promo (#2317) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Fix PurchasesIntegrationTest  (#2330) via Cesar de la Vega (@vegaro)
* Migrate Groovy to KTS for purchases-ui module (#2323) via Jaewoong Eum (@skydoves)
* Add `X-Is-Backgrounded` header to all requests (#2321) via Toni Rico (@tonidero)
* Clean up libs.versions.toml (#2318) via Jaewoong Eum (@skydoves)
* feat: Change default copy for web_subscription_manage #4921 (#2298) via Facundo Menzella (@facumenzella)
* Improve Korean translation (#2319) via Jaewoong Eum (@skydoves)
* [Paywalls V2] Renders images in template previews (#2311) via JayShortway (@JayShortway)
* [Paywalls V2] Removes Git LFS and adds submodule for paywall preview resources (#2315) via JayShortway (@JayShortway)
* [Paywalls V2] Make `radialGradient` use bigger size for radius calculation (#2314) via Toni Rico (@tonidero)
* Migrate purchases Gradle module to KTS (#2313) via Jaewoong Eum (@skydoves)
* Attempts to have Emerge Snapshots handle merge queue branches. (#2309) via JayShortway (@JayShortway)
* [AUTOMATIC][Paywalls V2] Updates Compose previews of all templates (#2306) via RevenueCat Git Bot (@RCGitBot)
* Use coil-base instead of coil-compose for purchases package (#2307) via Jaewoong Eum (@skydoves)
* Fix load shedder backend integration tests (#2308) via Toni Rico (@tonidero)
* [Paywalls V2] Fixes the update-paywall-templates job (#2305) via JayShortway (@JayShortway)
* [Paywalls V2] Adjusts CI to support images in template previews (#2302) via JayShortway (@JayShortway)
* [Diagnostics] Sync diagnostics if file reaches lower limit (#2304) via Toni Rico (@tonidero)
* Wrap Billing client crash with ours with more information (#2296) via Toni Rico (@tonidero)

## 8.15.1
## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls V2] Fuzzy-matches language without region (#2287) via JayShortway (@JayShortway)
* [Paywalls V2] Avoids adding spurious digits to per-period prices (#2277) via JayShortway (@JayShortway)

### 🔄 Other Changes
* [Diagnostics] Add Google event missing properties (#2294) via Antonio Pallares (@ajpallares)
* [Diagnostics] add `PURCHASE_STARTED` and `PURCHASE_RESULT` events (#2291) via Antonio Pallares (@ajpallares)
* [Diagnostics] add GET_CUSTOMER_INFO_STARTED and GET_CUSTOMER_INFO_RESULT events (#2289) via Antonio Pallares (@ajpallares)
* Bump fastlane-plugin-revenuecat_internal from `05ef095` to `9a29f63` (#2281) via dependabot[bot] (@dependabot[bot])
* [Diagnostics] Add amazon event missing properties (#2290) via Toni Rico (@tonidero)
* Bump fastlane from 2.226.0 to 2.227.0 (#2257) via dependabot[bot] (@dependabot[bot])
* [Diagnostics] Add restore purchases events (#2288) via Toni Rico (@tonidero)
* [Diagnostics] Add sync purchases started and result events (#2286) via Toni Rico (@tonidero)
* [Diagnostics] Add `amazon_purchase_attempt` event (#2285) via Toni Rico (@tonidero)
* [Diagnostics] add GET_PRODUCTS_STARTED and GET_PRODUCTS_RESULT events (#2283) via Antonio Pallares (@ajpallares)
* Bump nokogiri from 1.17.2 to 1.18.4 (#2284) via dependabot[bot] (@dependabot[bot])
* [AUTOMATIC][Paywalls V2] Updates Compose previews of all templates (#2282) via RevenueCat Git Bot (@RCGitBot)
* [Diagnostics] add `GET_OFFERINGS_STARTED` and `GET_OFFERINGS_RESULT` events (#2280) via Antonio Pallares (@ajpallares)
* [Diagnostics] Add google purchase started and update received events (#2279) via Toni Rico (@tonidero)
* Downgrades Emerge Snapshots SDK to 1.3.0. (#2278) via JayShortway (@JayShortway)
* [Diagnostics] add `is_retry` to `http_request_performed` event (#2276) via Antonio Pallares (@ajpallares)
* [Diagnostics] Tracks BillingClient connected/disconnected events. (#2267) via JayShortway (@JayShortway)
* [Diagnostics] add `no_entitlement_mapping_available` reason to error_entering_offline_entitlements_mode` event (#2274) via Antonio Pallares (@ajpallares)

## 8.15.0
## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix landscape mode in Paywalls v1 Template 3 (#2265) via Josh Holtz (@joshdholtz)
### Customer Center
#### ✨ New Features
* feat: Introduce `CustomerCenterView` for hybrid usage (#2170) via Cesar de la Vega (@vegaro)
* Add `onManagementOptionSelected` to `CustomerCenterListener` (#2270) via Cesar de la Vega (@vegaro)
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls V2] Ignores missing font alias if it's blank. (#2271) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes badges not being overriden (#2266) via JayShortway (@JayShortway)

### 🔄 Other Changes
* [Diagnostics] Add play_store_version and play_services_version to all google events (#2269) via Toni Rico (@tonidero)
* [Diagnostics] Add `id` and `app_session_id` to events (#2268) via Toni Rico (@tonidero)
* Uploads Paparazzi screenshots to Emerge. (#2232) via JayShortway (@JayShortway)

## 8.14.3
### 🔄 Other Changes
* Allows configuring `showInAppMessagesAutomatically` and `pendingTransactionsForPrepaidPlansEnabled` in customEntitlementComputation mode (#2260) via JayShortway (@JayShortway)
* [Paywalls V2] Tweaks the `MissingPackage` error message. (#2256) via JayShortway (@JayShortway)
* [Paywalls V2] Avoids re-parsing the component tree to find packages and tabs (#2252) via JayShortway (@JayShortway)
* [EXTERNAL] Set metaspace size to avoid "Daemon disappeared" failures (#2237) contributed by @runningcode (#2255) via Toni Rico (@tonidero)
* [Paywalls V2] Timeline component fixes (#2253) via Toni Rico (@tonidero)
* Updates Gemfile.lock (#2254) via RevenueCat Git Bot (@RCGitBot)
* Update fastlane plugin (#2251) via Toni Rico (@tonidero)
* [Paywalls V2] Refactors `StyleFactory` (#2247) via JayShortway (@JayShortway)
* Add refresh button to PaywallTester app (#2244) via Toni Rico (@tonidero)

## 8.14.2
## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* [Paywalls V2] Considers script when matching locales (#2245) via JayShortway (@JayShortway)

### 🔄 Other Changes
* [Paywalls V2] Reorganizes stack arrangement logic (#2238) via JayShortway (@JayShortway)

## 8.14.1
## RevenueCat SDK
### 🐞 Bugfixes
* fix: Remove API dependency with Coil (#2229) via Toni Rico (@tonidero)
* Set default interfaces compiler flag to `all-compatibility` (#2227) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Prevent Paywalls V1 Template 7 from defaulting empty badge to showing percent off (#2222) via Josh Holtz (@joshdholtz)
* Fixes inconsistent package ordering on tiered paywalls (template 7) (#2221) via JayShortway (@JayShortway)
### Paywallv2
#### 🐞 Bugfixes
* Fix background painting outside composable (#2236) via Toni Rico (@tonidero)
* [Paywalls V2] Avoids the theme from influencing the text color (#2235) via JayShortway (@JayShortway)
* [Paywalls V2] Handles potential locale id inconsistency between strings and variables (#2234) via JayShortway (@JayShortway)
* [Paywalls V2] Disable all buttons when any action is in progress (#2219) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes locales without region and `X-Preferred-Locales` (#2230) via JayShortway (@JayShortway)
* [Paywalls V2] Fix CarouselComponent peek (#2228) via Toni Rico (@tonidero)
### Customer Center
#### 🐞 Bugfixes
* Fix `CustomerCenterActivity` colors on dark mode (#2225) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [AUTOMATIC][Paywalls V2] Updates Compose previews of all templates (#2233) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC][Paywalls V2] Updates Compose previews of all templates (#2226) via RevenueCat Git Bot (@RCGitBot)

## 8.14.0
## RevenueCat SDK
### ✨ New Features
* Add `hasPaywall` property to `Offering` (#2212) via Antonio Pallares (@ajpallares)
### 🐞 Bugfixes
* Fix empty options in NoActive subscriptions screen (#2168) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Create `CustomerCenterListener` (#2199) via Cesar de la Vega (@vegaro)
#### 🐞 Bugfixes
* Reload Customer Center after a successful restore (#2203) via Cesar de la Vega (@vegaro)
* Fixes CustomerCenter state not refreshing when reopening (#2202) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Improves PaywallsTester multi-API-key support (#2218) via JayShortway (@JayShortway)
* [EXTERNAL] Bump Emerge Gradle Plugin and Snaphsots version (#2211) via @runningcode (#2217) via JayShortway (@JayShortway)
* [AUTOMATIC][Paywalls V2] Updates Compose previews of all templates (#2207) via RevenueCat Git Bot (@RCGitBot)
* [Paywalls V2] Enables template previews again (#2215) via JayShortway (@JayShortway)
* Adds support for switching between 2 API keys to PaywallsTester (#2213) via JayShortway (@JayShortway)
* Adds a `LocalPreviewImageLoader` `CompositionLocal`. (#2201) via JayShortway (@JayShortway)
* Logs from RevenueCatUI are now tagged with `[Purchases]` too. (#2206) via JayShortway (@JayShortway)
* [Paywalls V2] Ignores template previews for now. (#2209) via JayShortway (@JayShortway)
* [Paywalls V2] Some more template previews optimizations (#2208) via JayShortway (@JayShortway)
* chore: Delete key from customer center survey event (#2204) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Extract ImageLoader to use a single one (#2146) via Toni Rico (@tonidero)
* [Paywalls V2] Adds progress indicator to buttons (#2198) via JayShortway (@JayShortway)
* Avoids triggering "unscheduled" workflows when triggering workflows via the CircleCI API (#2200) via JayShortway (@JayShortway)
* [Paywalls V2] Adds a note on publishing to the missing paywall error. (#2193) via JayShortway (@JayShortway)
* Adds X-Kotlin-Version header. (#2197) via JayShortway (@JayShortway)
* [Paywalls V2] Adds docs on ignored arguments for Paywalls V2 in more places. (#2195) via JayShortway (@JayShortway)
* chore: Add backend integration test for events (#2189) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Adds CI job to update template previews (#2192) via JayShortway (@JayShortway)

## 8.13.0
## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Creates a `ShowCustomerCenter ` contract for displaying as an Activity (#2181) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [Paywalls V2] Adds template previews (#2184) via JayShortway (@JayShortway)
* Removes Intent.FLAG_ACTIVITY_NEW_TASK when starting CustomerCenterActivity (#2190) via Cesar de la Vega (@vegaro)
* chore: Introduce preview for no active subscriptions (#2188) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Allow manual scrolling if auto-advance is enabled (#2182) via Toni Rico (@tonidero)
* [Paywalls V2] Adds `visible` property to all components (#2172) via JayShortway (@JayShortway)

## 8.12.2
## RevenueCatUI SDK
#### 🐞 Bugfixes
* Fix issue with release minification with Emerge's annotation  (#2176) via Toni Rico (@tonidero)
### Customer Center
#### 🐞 Bugfixes
* fix: Track customer center events (#2178) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* Enable paywall tester minification (#2177) via Toni Rico (@tonidero)

## 8.12.1
## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Fix promotional offer not being displayed (#2173) via Cesar de la Vega (@vegaro)
* Fix back button dismissing the Customer Center (#2171) via Cesar de la Vega (@vegaro)

## 8.12.0
## RevenueCat SDK
### 🐞 Bugfixes
* fix: Add prepaid as a period type (#2141) via Greenie (@greenietea)

## RevenueCatUI SDK
### Paywalls v2
#### ✨ New Features
* [Paywalls V2] Adds support for Paywalls V2! (#2127) via JayShortway (@JayShortway)
### Customer Center
#### ✨ New Features
* Adds support for  `CustomerCenter` (#2156) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [Paywalls V2] Deprecate PaywallFooter in favor of OriginalTemplatePaywallFooter (#2111) via Toni Rico (@tonidero)
* feat: Filter CANCEL from help path if lifetime (#2140) via Facundo Menzella (@facumenzella)
* fix: Localize Restore Purchases Dialog (#2139) via Facundo Menzella (@facumenzella)
* fix: Hide contact support if there's no email (#2135) via Facundo Menzella (@facumenzella)
* feat: Track IMPRESSION and SURVER_OPTION_CHOSEN for CustomerCenter (#2124) via Facundo Menzella (@facumenzella)
* fix: Use TextButton instead of outline for Promotionals (#2138) via Facundo Menzella (@facumenzella)
* Rename carousel slide to page (#2144) via Josh Holtz (@joshdholtz)
* [Paywalls V2] Make border draw on top of the image overlay (#2165) via Toni Rico (@tonidero)
* [Paywalls V2] Gradients Reloaded: Makes linear gradients match CSS even more closely (#2166) via JayShortway (@JayShortway)
* [Paywalls V2] Fix wrong spacers in Stack when some children are Fill (#2164) via Toni Rico (@tonidero)
* chore: Unify Json encoding with JsonHelper (#2160) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Fixes the top system bar padding being applied to all children in a vertical container (#2162) via JayShortway (@JayShortway)
* [Paywalls V2] Refactor to use a single ImageLoader singleton in RevenueCatUI (#2161) via Toni Rico (@tonidero)
* [Paywalls V2] Apply stack margins to overlay badges (#2158) via Toni Rico (@tonidero)
* [Paywalls V2] Disables the click handler for the selected package (#2159) via JayShortway (@JayShortway)
* [Paywalls V2] Change stack distribution system to use spacers instead of custom arrangements (#2154) via Toni Rico (@tonidero)
* [Paywalls V2] Fixes empty stacks not showing up. (#2157) via JayShortway (@JayShortway)
* Explicitly sets the `defaults` flavor as the default. (#2155) via JayShortway (@JayShortway)
* Updates on no active purchases screen (#2150) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Fix issue with sizing of stacks with nested badges (#2152) via Toni Rico (@tonidero)
* [Paywalls V2] Fix issue with shadows overlapping long edgeToEdge badges (#2149) via Toni Rico (@tonidero)
* [Paywalls V2] Adjusts the convex/concave offset to 10% of the image height (#2151) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes variables for multi month periods (#2148) via JayShortway (@JayShortway)
* [Paywalls V2] Correctly handles variables for lifetime products (#2145) via JayShortway (@JayShortway)
* [Paywalls V2] Add shadows to badge previews (#2147) via Toni Rico (@tonidero)
* [Paywalls V2] Predownload paywall low res images (#2143) via Toni Rico (@tonidero)
* [Paywalls V2] Makes linear gradients consistent with CSS (#2142) via JayShortway (@JayShortway)
* [Paywalls V2] Adds edge-to-edge support (#2137) via JayShortway (@JayShortway)
* [Paywalls V2] Make root stack component use all the available space by default (#2136) via Toni Rico (@tonidero)
* [Paywalls V2] Avoids a crash if a package is missing (#2130) via JayShortway (@JayShortway)
* [Paywalls V2] Fix stack clipping issues (#2126) via Toni Rico (@tonidero)
* fix: Address typo and wrong doc for CustomerCenter events (#2133) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Fix merge conflict (#2134) via Toni Rico (@tonidero)
* [Paywalls V2] Actually ignores the font provider for V2. (#2129) via JayShortway (@JayShortway)
* Rename `PromotionalOfferView` to `PromotionalOfferScreen` (#2132) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Support background images in StackComponent, CarouselComponent, TabsComponent (#2131) via Toni Rico (@tonidero)
* [Paywalls V2] Support scroll orientation in StackComponent (#2108) via Toni Rico (@tonidero)
* [Paywalls V2] Correctly selects packages on tabs (#2122) via JayShortway (@JayShortway)
* Revamp UI in Customer Center (#2123) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Remove cursive as a generic font (#2118) via Josh Holtz (@joshdholtz)
* Publishes PaywallsTester to Internal testing track on every commit to `main` (#2100) via JayShortway (@JayShortway)
* Fixes `X` in promo offer not dismissing the promo (#2110) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Published version of PaywallTester now uses the Paywalls V2 Alpha RC project (#2093) via JayShortway (@JayShortway)
* [Paywalls V2] New overrides structure (#2120) via Toni Rico (@tonidero)
* [Paywalls V2] Adds `TabsComponent` samples and tests (#2115) via JayShortway (@JayShortway)
* Ignores `RestorePurchasesDialog` previews using `IgnoreEmergeSnapshot` (#2116) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Adds `TabsComponentView` (#2114) via JayShortway (@JayShortway)
* Fixes "View field header collides with a variable" data binding error in PurchaseTester (#2119) via JayShortway (@JayShortway)
* feat: Add support for tracking customer center events (#2117) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Adds `TabsComponentStyle` (#2113) via JayShortway (@JayShortway)
* [Paywalls V2] Adds deserialization of `TabsComponent` (#2101) via JayShortway (@JayShortway)
* [Paywalls V2] Implements Variables V2 (#2099) via JayShortway (@JayShortway)
* refactor: Introduce EventsManager to track events for different features (#2096) via Facundo Menzella (@facumenzella)
* Rename to Web Billing (#2094) via Antonio Borrero Granell (@antoniobg)
* [Paywalls V2] Fixes shadows drawing behind transparent components (#2112) via JayShortway (@JayShortway)
* Fix title not being reset after closing Feedback Survey (#2109) via Cesar de la Vega (@vegaro)
* Support for opening custom urls in Customer Center (#2107) via Cesar de la Vega (@vegaro)
* Prices in accept promotional offer button (#2104) via Cesar de la Vega (@vegaro)
* Customer Center use remote appearance config (#2102) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Use original paywall fallback when trying to use Footer modes in a Components paywall (#2106) via Toni Rico (@tonidero)
* [Paywalls V2] Add more `ImageComponentView` preview tests (#2103) via Toni Rico (@tonidero)
* [Paywalls V2] Add CarouselComponent page indicator animation (#2105) via Toni Rico (@tonidero)
* [Paywalls V2] Add `CarouselComponentView` (#2095) via Toni Rico (@tonidero)
* [Paywalls V2] Add `CarouselComponent` infrastructure (#2092) via Toni Rico (@tonidero)
* [Paywalls V2] Adds new price calculations needed for Variables V2 (#2098) via JayShortway (@JayShortway)
* [Paywalls V2] Preparation for Variables V2 (#2097) via JayShortway (@JayShortway)
* Promotional offers (#2011) via Cesar de la Vega (@vegaro)
* [Paywalls V2] No longer provides the entire `UiConfig` to `StyleFactory` (#2091) via JayShortway (@JayShortway)
* [Paywalls V2] Adds support for custom fonts (#2090) via JayShortway (@JayShortway)
* feat: Add basic customer center events (#2075) via Facundo Menzella (@facumenzella)
* Add support to manage non-Google purchases in Customer Center (#2067) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Add `TimelineComponentView` (#2083) via Toni Rico (@tonidero)
* [Paywalls V2] Process `TimelineComponent` overrides and state (#2082) via Toni Rico (@tonidero)
* [Paywalls V2] Cleans up after implementing color aliases (#2087) via JayShortway (@JayShortway)
* [Paywalls V2] Implements color aliases for backgrounds (#2086) via JayShortway (@JayShortway)
* `PaywallTesterApp` is no longer wrapped in a `Surface` (#2089) via JayShortway (@JayShortway)
* [Paywalls V2] Implements color aliases for shadows (#2085) via JayShortway (@JayShortway)
* [Paywalls V2] Implements color aliases for borders (#2084) via JayShortway (@JayShortway)
* [Paywalls V2] Implements color aliases for `TextComponent` (#2080) via JayShortway (@JayShortway)
* [Paywalls V2] Add `TimelineComponent` network parsing (#2047) via Toni Rico (@tonidero)
* [Paywalls V2] Implements color aliases for `ImageComponent` (#2079) via JayShortway (@JayShortway)
* [Paywalls V2] Implements color aliases for `IconComponent`  (#2078) via JayShortway (@JayShortway)
* Build `SubscriptionDetailsView` using `CustomerInfo` (#2057) via Cesar de la Vega (@vegaro)
* Some Material 3 updates to the `ManageSubscriptionsView` (#2072) via JayShortway (@JayShortway)
* [Paywalls V2] Implements color aliases for `StackComponent` (#2076) via JayShortway (@JayShortway)
* Fixes compilation of `IconComponentView`. (#2073) via JayShortway (@JayShortway)
* [Paywalls V2] Make `edgeToEdge` top/bottom badge extend to entire stack background (#2070) via Toni Rico (@tonidero)
* [Paywalls V2] Parses `UiConfig` (#2068) via JayShortway (@JayShortway)
* [Paywalls V2] Add `IconComponent` (#2071) via Toni Rico (@tonidero)
* [Paywalls V2] Badge: Handle main stack border width correctly in overlay and nested badge styles (#2069) via Toni Rico (@tonidero)
* [Paywalls V2] Add `edgeToEdge` badge trailing/leading style layout (#2054) via Toni Rico (@tonidero)
* [Paywalls V2] Adds support for fallback components (#2064) via JayShortway (@JayShortway)

## 8.11.0
## RevenueCat SDK
### ✨ New Features
* Add `subscriptionsByProductIdentifier` to `CustomerInfo` (#2052) via Cesar de la Vega (@vegaro)
### 🐞 Bugfixes
* Fix `OwnershipType` enum serialization (#2061) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Allow repurchasing custom packages (#2044) via Toni Rico (@tonidero)

### 🔄 Other Changes
* [Paywalls V2] Do not attempt to purchase if currently subscribed (#2062) via JayShortway (@JayShortway)
* [Trusted Entitlements] Enable `Trusted Entitlements` by default (#2050) via Toni Rico (@tonidero)
* [Trusted Entitlements] Do not clear CustomerInfo upon enabling Trusted Entitlements (#2049) via Toni Rico (@tonidero)
* [Paywalls V2] Removes `MaskShape.Pill` in favor of `MaskShape.Circle`. (#2063) via JayShortway (@JayShortway)
* [Paywalls V2] Font sizes are integers now. (#2059) via JayShortway (@JayShortway)
* [Paywalls V2] Handles intro offer eligibility overrides (#2058) via JayShortway (@JayShortway)
* [Paywalls V2] Implements `Convex` and `Concave` image masks (#2055) via JayShortway (@JayShortway)
* [Paywalls V2] Add new `ImageComponent` properties (#2056) via Toni Rico (@tonidero)
* [Paywalls V2] Add `Badge`'s `nested` style layout (#2041) via Toni Rico (@tonidero)
* [Paywalls V2] Add `Badge`'s `edgeToEdge` `Top`/`Bottom` alignment style layout (#2039) via Toni Rico (@tonidero)
* [Paywalls V2] Various `PaywallViewModel` fixes and tests (#2051) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes minimum spacing when distribution is `SPACE_BETWEEN`, `SPACE_AROUND` or `SPACE_EVENLY` (#2053) via JayShortway (@JayShortway)
* [Paywalls V2] Correctly determines when to show or hide decimals for prices (#2048) via JayShortway (@JayShortway)
* [Paywalls V2] `TextComponentView` uses the correct `Package` for variable values (#2042) via JayShortway (@JayShortway)
* [Paywalls V2] Adds Custom Tabs to support in-app browser URL destinations (#2035) via JayShortway (@JayShortway)
* Update `agp` to 8.8.0 (#2045) via Toni Rico (@tonidero)
* [Paywalls V2] Add `Badge`'s `overlay` style layout (#2009) via Toni Rico (@tonidero)
* [Paywalls V2] Implements all button actions (#2034) via JayShortway (@JayShortway)
* Convert error message property into computed property (#2038) via Toni Rico (@tonidero)

## 8.10.8
## RevenueCat SDK
### 📦 Dependency Updates
* Updates the Play Billing Library to 7.1.1. (#2032) via JayShortway (@JayShortway)

### 🔄 Other Changes
* [Paywalls V2] Fixes click consumption in text buttons (#2033) via JayShortway (@JayShortway)
* [Paywalls V2] Handles gradient percentages in the 0..100 range (#2030) via JayShortway (@JayShortway)
* [Paywalls V2] Add `Badge` property to StackComponent (#2003) via Toni Rico (@tonidero)
* [Paywalls V2] Improves image previews (#2029) via JayShortway (@JayShortway)
* Make `PurchasesError` `java.io.Serializable` (#2031) via Toni Rico (@tonidero)
* [Paywalls V2] Fixes `ImageComponentView` size when axes are Fit and Fixed (#2024) via JayShortway (@JayShortway)
* [Paywalls V2] Image backgrounds support `fit_mode` and `color_overlay` (#2021) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes various rendering issues (#2020) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes various deserialization issues (#2018) via JayShortway (@JayShortway)
* [Paywalls V2] Simplifies validation of `PaywallComponentsData` (#2017) via JayShortway (@JayShortway)
* [Paywalls V2] Moves click handling from `ButtonComponentStyle` to `ButtonComponentView` (#2016) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `PackageComponentView` (#2014) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `ImageComponentState` and override functionality (#2012) via JayShortway (@JayShortway)

## 8.10.7
## RevenueCat SDK
### 🐞 Bugfixes
* improvements for fr translations (#2019) via Andy Boedo (@aboedo)

### 🔄 Other Changes
* Feedback Surveys (#2010) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Fixes previews (#2015) via JayShortway (@JayShortway)
* [Paywalls V2] Moves validation logic to the Loading phase (#2007) via JayShortway (@JayShortway)
* Add Cancel subscriptions support  (#2008) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Localizations are kept in a NonEmptyMap (#2001) via JayShortway (@JayShortway)
* [Paywalls V2] `TextComponentState` handles locale changes (#2000) via JayShortway (@JayShortway)
* [Paywalls V2] Some minor cleanup (#1994) via JayShortway (@JayShortway)
* [Paywalls V2] `StackComponentView` handles overrides with `StackComponentState` (#1993) via JayShortway (@JayShortway)
* Customer Center restores [CC-2] (#1999) via Cesar de la Vega (@vegaro)
* [Paywalls V2] Add more image component previews to test parent being smaller than image size (#2004) via Toni Rico (@tonidero)
* [Paywalls V2] Add `PurchaseButtonComponent` support (#2002) via Toni Rico (@tonidero)
* Build Customer Center from JSON (#1998) via Cesar de la Vega (@vegaro)
* Fix missing import after PR merge conflict (#1997) via Toni Rico (@tonidero)
* [Paywalls V2] Add `StickyFooterComponentView` (#1991) via Toni Rico (@tonidero)
* [Paywalls V2] `TextComponentView` handles overrides with `TextComponentState` (#1989) via JayShortway (@JayShortway)

## 8.10.6
## RevenueCat SDK
### 📦 Dependency Updates
* Bump fastlane from 2.225.0 to 2.226.0 (#1973) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `3b1e7cf` to `4ee8579` (#1980) via dependabot[bot] (@dependabot[bot])

### 🔄 Other Changes
* [Paywalls V2] Fixes previews of `ButtonComponentView`, `StackComponentView` and `TextComponentView` (#1992) via JayShortway (@JayShortway)
* [Paywalls V2] Adds a `FLAG_PAYWALL_COMPONENTS` build flag (#1990) via JayShortway (@JayShortway)
* [Paywalls V2] Moves more state to `PaywallState` (#1988) via JayShortway (@JayShortway)
* [PaywallsV2] Improve `ButtonComponentView` to handle disabling the button and add `PurchasePackage` action (#1984) via Toni Rico (@tonidero)
* [Paywalls V2] `TextComponentStyle` no longer needs a Composable context (#1986) via JayShortway (@JayShortway)
* Make web redemption link APIs stable (#1985) via Toni Rico (@tonidero)
* [WEB-1757] Handle new backend error codes that may show in the redemption endpoint (#1987) via Toni Rico (@tonidero)
* [Paywalls V2] Stack background, borders and shadows update depending on the theme (#1982) via JayShortway (@JayShortway)
* [Paywalls V2] Fixes `TextComponentView`'s  font size (#1981) via JayShortway (@JayShortway)
* [Paywalls V2] Various minor fixes (#1979) via JayShortway (@JayShortway)
* [Paywalls V2] Adds support to PaywallsTester (#1978) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `PaywallComponentsData` to `Offering` (#1977) via JayShortway (@JayShortway)
* Parse web redemption link strings more defensively (#1976) via Toni Rico (@tonidero)
* Add web redemption parse method for strings to support hybrids (#1972) via Toni Rico (@tonidero)

## 8.10.5
## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix multi-tier template to allow optional header image (#1971) via Josh Holtz (@joshdholtz)

### 🔄 Other Changes
* [Paywalls V2] Adds first version of `LoadedPaywallComponents` (#1970) via JayShortway (@JayShortway)
* [Paywalls V2] Add `ButtonComponentView` (#1963) via Toni Rico (@tonidero)
* [Paywalls V2] Adds `PaywallState.Loaded.Components` (#1969) via JayShortway (@JayShortway)
* [Paywalls V2] Adds image background tests (#1967) via JayShortway (@JayShortway)
* [Paywalls V2] `TextComponentView` updates when the theme changes (#1966) via JayShortway (@JayShortway)
* [Paywalls V2] Adds a `StyleFactory` (#1965) via JayShortway (@JayShortway)
* [EXTERNAL] fix: update polish translations (#1919) via @radko93 (#1964) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `ImageComponentView` (#1959) via Toni Rico (@tonidero)
* WebPurchaseRedemption: Rename `AlreadyRedeemed` result to `PurchaseBelongsToOtherUser` (#1962) via Toni Rico (@tonidero)
* [Paywalls V2] Extends support for blurred shadows to all Android versions (#1957) via JayShortway (@JayShortway)

## 8.10.4
## RevenueCat SDK
### 🐞 Bugfixes
* Paywalls: Update Finnish "restore" localization (#1918) via Jeffrey Bunn (@Jethro87)

### 🔄 Other Changes
* [Paywalls V2] Adds `StackComponentView` (#1956) via JayShortway (@JayShortway)
* Avoid generating documentation for symbols annotated with `@InternalRevenueCatAPI` (#1958) via JayShortway (@JayShortway)
* [Paywalls V2] Preparation for `StackComponentView` (#1954) via JayShortway (@JayShortway)
* [Paywalls V2] Adds borders and image backgrounds (#1953) via JayShortway (@JayShortway)
* [Paywalls V2] Adds support for shadows (#1952) via JayShortway (@JayShortway)
* [Paywalls V2] Adds override system via `PresentedPartial` (#1948) via JayShortway (@JayShortway)
* Add reachability tests to integration tests (#1955) via Toni Rico (@tonidero)
* [Paywalls V2] Add support for markdown in text component (#1951) via Toni Rico (@tonidero)
* Modify intent extension API to parse redemption links to mimick iOS (#1949) via Toni Rico (@tonidero)
* [Paywalls V2] Adds `TextComponentView` (#1941) via JayShortway (@JayShortway)

## 8.10.3
## RevenueCat SDK
### 🐞 Bugfixes
* Uses `Sequence` instead of `Stream` to avoid errors due to unavailable Java 8 APIs (#1943) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Increase integration test timeout (#1946) via Toni Rico (@tonidero)
* Removes `@RequiresApi(N)` from `FileHelper` and related classes (#1944) via JayShortway (@JayShortway)
* [Paywalls V2] Minimizes Java API (#1942) via JayShortway (@JayShortway)
* [Paywalls V2] Makes `TextComponent` public (#1939) via JayShortway (@JayShortway)
* Introduces an `@InternalRevenueCatAPI` annotation (#1938) via JayShortway (@JayShortway)
* [Paywalls V2] Moves any non-component file to a new `common` package. (#1937) via JayShortway (@JayShortway)
* [Paywalls V2] `LocalizationKey` is an inline value class now. (#1936) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `PaywallComponentsData` (#1935) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `StickyFooterComponent` (#1934) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `PurchaseButtonComponent` (#1933) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `PackageComponent` (#1932) via JayShortway (@JayShortway)
* Ensure the correct error message is shown when failing to open a Uri in paywalls (#1922) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `ButtonComponent` (#1931) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `StackComponent` (#1930) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `ComponentOverrides` (#1929) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `ImageComponent` (#1928) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `TextComponent` (#1927) via JayShortway (@JayShortway)
* [Paywalls V2] Adds all enum properties (#1926) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `SizeConstraints`, `Size`, `Padding` and `Shadow` properties (#1925) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `CornerRadiuses`, `Shape` and `MaskShape` properties (#1924) via JayShortway (@JayShortway)
* [Paywalls V2] Adds `ImageUrls` and `ColorInfo` properties (#1923) via JayShortway (@JayShortway)

## 8.10.2
## RevenueCat SDK
### 🐞 Bugfixes
* Catch `NoSuchMethodError` on Ads library (#1914) via Cesar de la Vega (@vegaro)

## 8.10.1
## RevenueCatUI SDK
### 🐞 Bugfixes
* Handle `IllegalArgumentException` when opening a Uri in the paywalls (#1904) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Create sample web purchase redemption app (#1908) via Toni Rico (@tonidero)
* Add extra error cases for web purchases redemptions (#1899) via Toni Rico (@tonidero)
* Use new web redemption endpoint (#1903) via Toni Rico (@tonidero)

## 8.10.0
## RevenueCat SDK
### ✨ New Features
* Add `tenjinAnalyticsInstallationId` setter property (#1897) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* [Fix] Consider a network error as not successfully synced for paywall events (#1900) via Mark Villacampa (@MarkVillacampa)

### 🔄 Other Changes
* [Paywalls] Synchronize paywall events on app backgrounding and after a purchase (#1901) via Mark Villacampa (@MarkVillacampa)
* Dispatcher catches and rethrows Throwable instead of Exception to avoid swallowing errors (#1894) via JayShortway (@JayShortway)

## 8.9.0
## RevenueCat SDK
### ✨ New Features
* [Experimental] Web purchase redemption (#1889) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Keeps the org.json package. (#1891) via JayShortway (@JayShortway)
### 📦 Dependency Updates
* Bump rexml from 3.3.8 to 3.3.9 (#1892) via dependabot[bot] (@dependabot[bot])
* Bump danger from 9.5.0 to 9.5.1 (#1885) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.224.0 to 2.225.0 (#1884) via dependabot[bot] (@dependabot[bot])

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix application of modifiers in `Markdown` component (#1893) via Toni Rico (@tonidero)

### 🔄 Other Changes
* [CI] Allow publishing snapshot releases manually from branches (#1888) via Toni Rico (@tonidero)
* Detekt auto-fixes (#1886) via Toni Rico (@tonidero)

## 8.8.2
### 🔄 Other Changes
* Adds X-Is-Debug-Build header (#1873) via JayShortway (@JayShortway)

## 8.8.1
## RevenueCat SDK
### 🐞 Bugfixes
* Save SDK configuration on paywall activity so it can reconfigure automatically (#1872) via Toni Rico (@tonidero)
### 📦 Dependency Updates
* Bump fastlane from 2.223.1 to 2.224.0 (#1870) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `5b2e35c` to `3b1e7cf` (#1865) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.222.0 to 2.223.1 (#1860) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `55a0455` to `5b2e35c` (#1858) via dependabot[bot] (@dependabot[bot])

### 🔄 Other Changes
* [CustomerCenter] Fix help path deserializing when unknown type (#1869) via Toni Rico (@tonidero)
* [CustomerCenter] Create `CustomerCenter` composable and view model with some initial UI (#1867) via Toni Rico (@tonidero)
* [CustomerCenter] Add networking layer and models (#1859) via Toni Rico (@tonidero)
* [CustomerCenter] Adds SubscriptionDetailsView (#1863) via JayShortway (@JayShortway)

## 8.8.0
## RevenueCat SDK
### ✨ New Features
* Add `kochava` integration (#1844) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix multiple paywalls display issues (#1854) via Toni Rico (@tonidero)
* Fix interaction not disabled during purchases (#1850) via Toni Rico (@tonidero)
* Fix crash if activity finished while calculating presentation logic (#1846) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Adds some more test cases validating {{ total_price_and_per_month }} for quarterly packages.  (#1853) via JayShortway (@JayShortway)
* Converts CustomEntitlementComputationSample's Gradle files to Kotlin (#1852) via JayShortway (@JayShortway)
* Converts MagicWeatherCompose's Gradle files to Kotlin (#1851) via JayShortway (@JayShortway)
* [EXTERNAL] Wireup Emerge gradle plugin config for PR snapshot diffs (#1841) by @rbro112 (#1843) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `5140dbc` to `55a0455` (#1845) via Cesar de la Vega (@vegaro)

## 8.7.2
### Bugfixes
* Fixes a Paywall Template 7 crash when none of the tiers have any available products. (#1834) via JayShortway (@JayShortway)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `85e1c83` to `5140dbc` (#1835) via dependabot[bot] (@dependabot[bot])
### Other Changes
* [EXTERNAL] Add Emerge snapshot testing (#1785) by @rbro112 (#1831) via Toni Rico (@tonidero)

## 8.7.1
### Bugfixes
* [EXTERNAL] Fixes Amazon subscription period parsing (#1813) by @tessmerandre (#1828) via Toni Rico (@tonidero)
### Other Changes
* [Diagnostics] Send backend_error_code in http request failures if available (#1825) via Toni Rico (@tonidero)

## 8.7.0
### New Features
* Paywalls can use custom in-app purchase/restore code (#1777) via James Borthwick (@jamesrb1)
### Bugfixes
* [Paywalls] Add spacing between paragraphs and make text go full width so text align applies (#1824) via Josh Holtz (@joshdholtz)
### Dependency Updates
* Bump rexml from 3.3.4 to 3.3.6 (#1823) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `4c4b8ce` to `85e1c83` (#1812) via dependabot[bot] (@dependabot[bot])
* Adds Detekt rules for Compose. Updates detekt to 1.23.6. (#1822) via JayShortway (@JayShortway)
### Other Changes
* Converts MagicWeather's Gradle files to Kotlin (#1821) via JayShortway (@JayShortway)

## 8.6.0
### Dependency updates
* Update `compose-bom` to 2024.08.00 (#1817) via Toni Rico (@tonidero)
### Other Changes
* Update debug view snapshot tests (#1819) via Toni Rico (@tonidero)

## 8.5.0
### New Features
* [Paywalls] Add default locale (#1809) via Josh Holtz (@joshdholtz)
* Support to round+truncate prices displayed on paywalls for backend-specified countries (#1806) via James Borthwick (@jamesrb1)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `d5f0742` to `4c4b8ce` (#1807) via dependabot[bot] (@dependabot[bot])
* Bump danger from 9.4.3 to 9.5.0 (#1802) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Publish paywall tester on each release (#1803) via Toni Rico (@tonidero)

## 8.4.0
### New Features
* Adds pricePerPeriod functions to PricingPhase (#1789) via JayShortway (@JayShortway)
### Dependency Updates
* Bump rexml from 3.2.9 to 3.3.2 (#1788) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.221.1 to 2.222.0 (#1797) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Adds the baseline file to the Detekt IntelliJ plugin settings. (#1798) via JayShortway (@JayShortway)
* Adds the Detekt IntelliJ plugin settings to version control. (#1796) via JayShortway (@JayShortway)
* Diagnostics: Remove unused anonymizer (#1795) via Toni Rico (@tonidero)

## 8.3.1
### RevenueCatUI
* [Paywalls] Fix `RemoteImage` flicker on activity changes (#1791) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `5f55466` to `d5f0742` (#1787) via dependabot[bot] (@dependabot[bot])
* Updates Gradle (8.9), AGP (8.5.1) and maven.publish (0.28.0) (#1783) via JayShortway (@JayShortway)

## 8.3.0
### RevenueCatUI
* Added new paywall template to support multiple tiered subscriptions (#1720) via Josh Holtz (@joshdholtz)

## 8.2.1
### Bugfixes
* Attempt to fix ANRs by moving some tasks during configure to background (#1772) via Cesar de la Vega (@vegaro)
* Use trimmed API key (#1768) via Toni Rico (@tonidero)
### Other Changes
* Remove leftovers `ExperimentalPreviewRevenueCatUIPurchasesAPI` (#1778) via Cesar de la Vega (@vegaro)
* Update migration docs adding back support for `DEFERRED` upgrades (#1774) via Toni Rico (@tonidero)
* Add 7.12.0 to CHANGELOG (#1769) via Toni Rico (@tonidero)

## 8.2.0
### New Features
* Add `DEFERRED` replacement mode (#1751) via Toni Rico (@tonidero)
### Bugfixes
* Use correct product context with `DEFERRED` purchases (#1766) via Toni Rico (@tonidero)
* Call callbacks on `DEFERRED` purchases (#1764) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane from 2.221.0 to 2.221.1 (#1758) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Adds .aiexclude (#1762) via JayShortway (@JayShortway)

## 8.1.0
### New Features
* `Amazon`: Add getAmazonLWAConsentStatus method to support Quick Subscribe (#1666) via Mark Villacampa (@MarkVillacampa)
### Bugfixes
* Makes `ITEM_NOT_OWNED` message more clear when restoring and acknowledging/consuming (#1757) via Cesar de la Vega (@vegaro)
* Support ISO8601-2 period durations (#1756) via Toni Rico (@tonidero)
* Paywalls: Add bottom padding when there are no buttons in footer (#1750) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane from 2.220.0 to 2.221.0 (#1755) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Update test lane to require active entitlements (#1754) via Toni Rico (@tonidero)
* SDK-3482 Updates various orbs and images (#1752) via JayShortway (@JayShortway)
* Update observermode docs URLs (#1721) via JayShortway (@JayShortway)
* Remove unneeded Android Lollipop version checks (#1749) via Toni Rico (@tonidero)

## 8.0.0
For a full migration guide check [v8-MIGRATION.md](migrations/v8-MIGRATION.md)

### Breaking Changes
* Support BC7 (#1737) via Toni Rico (@tonidero)

#### New APIs:
- `SubscriptionOption.installmentInfo` which contains information about installment plans you might want to display in your paywall.
- `PurchasesConfiguration.pendingTransactionsForPrepaidPlansEnabled` allows to enable pending prepaid subscriptions.

#### Breaking changes
- Increase min Sdk to 21. This was increased in BC7
- Increase min kotlin version to 1.7.0
- Remove `UpgradeInfo`
- Remove all `purchasePackage` and `purchaseProduct` methods using `UpgradeInfo`. If you want to perform upgrades/downgrades, please migrate to use the `purchase(PurchaseParams)` method, which has options to perform that operation.

### Other Changes
* Bump kotlin to 1.8 (#1726)

## 7.12.0
### New features
* Add `DEFERRED` replacement mode (#1751) via Toni Rico (@tonidero)
### Bugfixes
* Call callbacks on DEFERRED purchases (#1764) via Toni Rico (@tonidero)
* Use correct product context with DEFERRED purchases (#1766) via Toni Rico (@tonidero)

## 7.11.1
### Behavior Changes
* Stops collecting the ANDROID_ID. (#1729) via JayShortway (@JayShortway)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `8ec0072` to `5f55466` (#1722) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Diagnostics: sync only on first activity foregrounded (#1728) via Toni Rico (@tonidero)
* Adds 6.9.8 to the changelog. (#1731) via JayShortway (@JayShortway)
* Remove internal ObserverMode mentions (#1710) via JayShortway (@JayShortway)
* Deprecate observerMode in favor of purchasesAreCompletedBy (#1708) via JayShortway (@JayShortway)
* Set INFORMATIONAL trusted entitlements mode as the default in the purchase tester app (#1716) via Toni Rico (@tonidero)
* Remove `force-release-without-waiting-for-tests` job (#1719) via Cesar de la Vega (@vegaro)
* Fix purchase tester offerings not reloading (#1715) via Toni Rico (@tonidero)

## 7.11.0
### New Features
* Support non consumable products (#1697) via Toni Rico (@tonidero)
### Bugfixes
* Fix non-consumables not getting acknowledged (#1709) via Cesar de la Vega (@vegaro)
* Fix bad copy in log message (#1702) via Andy Boedo (@aboedo)
### Dependency Updates
* Bump amazon SDK version to 3.0.5 (#1713) via Mark Villacampa (@MarkVillacampa)
* Bump rexml from 3.2.6 to 3.2.8 (#1701) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `dd5e21f` to `8ec0072` (#1696) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Fix offline integration tests (#1704) via Toni Rico (@tonidero)
* Do not consume IAPs when backend returns 4xx errors (#1699) via Toni Rico (@tonidero)
* Update fastlane plugin and fix docs index path (#1695) via Toni Rico (@tonidero)
* Add trigger_bump lane (#1692) via Cesar de la Vega (@vegaro)

## 7.10.1
### Bugfixes
* Checks if callback has already been called in `canMakePayments` (#1690) via Cesar de la Vega (@vegaro)
* `Paywalls`: Update Norwegian "restore" localization (#1688) via Josh Holtz (@joshdholtz)
### Other Changes
* Make clearing diagnostics file happen during tracking (#1685) via Toni Rico (@tonidero)
* Add new diagnostics events for diagnostics (#1684) via Toni Rico (@tonidero)

## 7.10.0
### New Features
* Update BillingClient version to 6.2.1 (#1680) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `f88dcd4` to `dd5e21f` (#1686) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-revenuecat_internal from `1e62420` to `f88dcd4` (#1681) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Only update docs index on latest stable releases (#1676) via Toni Rico (@tonidero)

## 7.9.0
### New Features
* Paywalls: Allow closed button color to be configured (#1674) via Josh Holtz (@joshdholtz)
### Other Changes
* Add offline entitlements diagnostics events (#1671) via Toni Rico (@tonidero)

## 7.8.2
### Dependency Updates
* Bump fastlane from 2.219.0 to 2.220.0 (#1668) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Add store entry for External Purchases API (#1664) via Mark Villacampa (@MarkVillacampa)
* Remove confusing placements error log (#1670) via Toni Rico (@tonidero)
* Improve diagnostics data tracking (#1651) via Toni Rico (@tonidero)

## 7.8.1
### Other Changes
* Update android executor to a non-deprecated tag (#1659) via Cesar de la Vega (@vegaro)
* Add `RC_BILLING` store  (#1657) via Toni Rico (@tonidero)

## 7.8.0
### New Features
* Create AssetFont (#1650) via Cesar de la Vega (@vegaro)

## 7.7.3
### RevenueCatUI
* Add space to title in Template 5 (#1652) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `9c82c7a` to `8d4d9b1` (#1647) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Correctly support RGBA colors (#1649) via Toni Rico (@tonidero)
* Create android test apps to test Android SDK regressions (#1648) via Toni Rico (@tonidero)

## 7.7.2
### Other Changes
* Revert to previous androidx core version (1.8.0) (#1640) via Toni Rico (@tonidero)
* Add 6.9.7 to changelog (#1639) via Cesar de la Vega (@vegaro)

## 7.7.1
### Bugfixes
* Fix amazon zero price (#1634) via Cesar de la Vega (@vegaro)

## 7.7.0
### New Features
* Make `PresentedOfferingContext` public (#1630) via Josh Holtz (@joshdholtz)
* New `syncAttributesAndOfferingsIfNeeded` method (#1626) via Josh Holtz (@joshdholtz)
* Add targeting to `PresentedOfferingContext` (#1628) via Josh Holtz (@joshdholtz)
* Add `getCurrentOffering(forPlacement: String)` to `Offerings` (#1604) via Josh Holtz (@joshdholtz)
### Other Changes
* Paywalls: Improve reliability in case the state changes from loaded to a different state (#1620) via Toni Rico (@tonidero)

## 7.6.0
### New Features
* Add setDisplayDismissButton to PaywallView (#1608) via Cesar de la Vega (@vegaro)
### Bugfixes
* Fix runtime crash when using amazon and targetting android 14 (#1622) via Toni Rico (@tonidero)
* Paywalls: No-op on all view model methods in the Loading paywall screen (#1617) via Toni Rico (@tonidero)
* Fix safe insets on full screen (#1613) via Cesar de la Vega (@vegaro)
### Other Changes
* Change default paywall background with a lower quality image (#1623) via Toni Rico (@tonidero)
* Fix load shedder integration tests (#1618) via Cesar de la Vega (@vegaro)
* Refactor `presentedOfferingIdentifier` into `presentedOfferingContext` object (#1612) via Toni Rico (@tonidero)
* [External] Update russian and kazakh translations (#1577 by @janbolat) (#1616) via Toni Rico (@tonidero)

## 7.5.2
### RevenueCatUI
* Paywalls: Call `PaywallDialog` dismiss handler after successful restore if needed (#1610) via Toni Rico (@tonidero)

## 7.5.1
### Bugfixes
* Fix serialization proguard rules (#1606) via Cesar de la Vega (@vegaro)
* Fix paywall POST receipt data for paywall revision (#1605) via Josh Holtz (@joshdholtz)
### Other Changes
* `Paywalls`: add test coverage for locales with different region (#1600) via NachoSoto (@NachoSoto)

## 7.5.0
### New Features
* [EXTERNAL] Exposing fontprovider setter in `PaywallView` and `PaywallFooterView` by @Jjastiny (#1589) via Toni Rico (@tonidero)
### RevenueCatUI
* `Paywalls`:  add 4 new variables (#1599) via NachoSoto (@NachoSoto)
### Bugfixes
* Catch Exception instead of only IllegalArgumentException when decoding paywallData (#1601) via Cesar de la Vega (@vegaro)
* Revert making PaywallFooterView an AbstractComposeView (#1598) via Cesar de la Vega (@vegaro)
* Add missing forwarding to `PaywallListener.onPurchaseCancelled` (#1590) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump danger from 9.4.2 to 9.4.3 (#1597) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Update CHANGELOG with 6.9.6 release notes (#1596) via Toni Rico (@tonidero)

## 7.4.1
### RevenueCatUI
* `Paywalls`: fix template 5 title alignment (#1585) via NachoSoto (@NachoSoto)
* `Paywalls`: replace `TextAlign.Left` with `TextAlign.Start` to better support RTL (#1584) via NachoSoto (@NachoSoto)
* Paywall views: Support updating options state after initial layout (#1582) via Toni Rico (@tonidero)
* `Paywalls`: improve `PaywallData.configForLocale()` disambiguation (#1579) via NachoSoto (@NachoSoto)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `e6ba247` to `9c82c7a` (#1583) via dependabot[bot] (@dependabot[bot])
### Other Changes
* `OfferingsFactory`: debug logs when creating `Offerings` (#1576) via NachoSoto (@NachoSoto)
* `Paywalls`: log error when creating `PaywallState.Error` (#1574) via NachoSoto (@NachoSoto)

## 7.4.0
### New Features
* Add name property to StoreProduct (#1567) via Toni Rico (@tonidero)
### RevenueCatUI
* Fix paywall product name variable to not include app name (#1569) via Toni Rico (@tonidero)
### Bugfixes
* Use activity instead of context to calculate window size (#1570) via Toni Rico (@tonidero)
### Other Changes
* `VariableProcessor`: fix tests (#1572) via NachoSoto (@NachoSoto)

## 7.3.6
### RevenueCatUI
* `Paywalls`: improve template 5 layout for long product names (#1564) via NachoSoto (@NachoSoto)

## 7.3.5
### Bugfixes
* Revert google ads library bump to remove `AD_ID` permission (#1561) via Toni Rico (@tonidero)

## 7.3.4
### RevenueCatUI
* `Paywalls`: improve error display (#1558) via NachoSoto (@NachoSoto)
* Fix paywall dialog composable sizing issues (#1556) via Toni Rico (@tonidero)
* `Paywalls`: improve landscape layout on all templates (#1551) via NachoSoto (@NachoSoto)
### Bugfixes
* Fix AmazonConfiguration for Java projects (#1554) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `0ddee10` to `e6ba247` (#1557) via dependabot[bot] (@dependabot[bot])

## 7.3.3
### Bugfixes
* Fix NullPointerException issues getting advertising id (#1550) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane from 2.218.0 to 2.219.0 (#1549) via dependabot[bot] (@dependabot[bot])

## 7.3.2
### RevenueCatUI
* PaywallActivityLauncher: Add callback indicating whether the paywall was displayed when presented conditionally to an entitlement identifier (#1542) via Toni Rico (@tonidero)
* Report restore errors when using PaywallActivityLauncher (#1544) via Toni Rico (@tonidero)
* Remove scroll in Template 2 and 5 in Footer mode (#1545) via Cesar de la Vega (@vegaro)
### Bugfixes
* Opens PaywallFooterView (#1541) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump fastlane from 2.217.0 to 2.218.0 (#1543) via dependabot[bot] (@dependabot[bot])

## 7.3.1
### RevenueCatUI
* `Paywalls`: `PurchaseCancelledError` sends `onPurchaseCancelled` instead of an error (#1538) via NachoSoto (@NachoSoto)
* Paywalls: add Chinese from mainland China (#1537) via Andy Boedo (@aboedo)

## 7.3.0
### RevenueCatUI
* Add `launchIfNeeded` with `offeringIdentifier` (#1533) via Cesar de la Vega (@vegaro)
* Hide action bar from paywall activity (#1532) via Toni Rico (@tonidero)
* Paywalls: improve Chinese localization (#1529) via Andy Boedo (@aboedo)
### Other Changes
* Wait for tests to pass when deploying (#1521) via Cesar de la Vega (@vegaro)

## 7.2.9
### RevenueCatUI
* Paywalls: fix empty description when using custom package type and Offer Period (#1519) via Andy Boedo (@aboedo)
### Bugfixes
* Disable close button when action is in progress (#1528) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump danger from 9.4.1 to 9.4.2 (#1527) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Add revenuecatui docs to reference docs (#1526) via Toni Rico (@tonidero)

## 7.2.8
### RevenueCatUI
* `Paywalls`: fix traditional chinese string (#1522) via NachoSoto (@NachoSoto)
* `Paywalls`: use `webp` images (#1520) via NachoSoto (@NachoSoto)

## 7.2.7
### RevenueCatUI
* Paywalls: Add `PaywallFooterView` (#1509) via Toni Rico (@tonidero)
* Paywalls: Remove `PaywallActivity` theme to pickup application's theme by default (#1511) via Toni Rico (@tonidero)
* Paywalls: Auto-close paywall activity if restore grants required entitlement identifier (#1507) via Toni Rico (@tonidero)
### Bugfixes
* Improve pricePerYear price calculation precision (#1515) via Toni Rico (@tonidero)
* Improve price per month accuracy for weekly subscriptions (#1504) via Andy Boedo (@aboedo)
### Dependency Updates
* Bump danger from 9.4.0 to 9.4.1 (#1512) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Remove unnecessary appInBackground parameters (#1508) via Cesar de la Vega (@vegaro)
* Create `PurchasesStateProvider` (#1502) via Cesar de la Vega (@vegaro)

## 7.2.6
### Bugfixes
* Fix white line around buttons in Template 4 (#1499) via Cesar de la Vega (@vegaro)
### Performance Improvements
* Avoid creating threads when dispatching tasks (#1496) via Toni Rico (@tonidero)
### Other Changes
* Refactor QueryPurchasesUseCase and adds QueryPurchasesByTypeUseCase (#1495) via Cesar de la Vega (@vegaro)
* Fixes on `update_snapshots_repo` (#1501) via Cesar de la Vega (@vegaro)
* Fixes on generate_snapshots_RCUI lane (#1500) via Cesar de la Vega (@vegaro)
* Fix integration tests (#1498) via Toni Rico (@tonidero)
* Cleanup warnings in AcknowledgePurchaseUseCaseTest (#1497) via Cesar de la Vega (@vegaro)
* Add AcknowledgePurchaseUseCase (#1492) via Cesar de la Vega (@vegaro)
* Refactor findPurchaseInPurchaseHistory (#1493) via Cesar de la Vega (@vegaro)
* ConsumePurchaseUseCase (#1487) via Cesar de la Vega (@vegaro)
* Add retries with backoff and different treatment to SERVICE_UNAVAILABLE (#1490) via Cesar de la Vega (@vegaro)

## 7.2.5
### RevenueCatUI
* `Paywalls`: Fix Turkish translation (#1478) via iremkaraoglu (@iremkaraoglu)
### Bugfixes
* Check diagnostics file size in background thread (#1488) via Toni Rico (@tonidero)
### Dependency Updates
* Update Billing client to 6.1.0 (#1481) via Toni Rico (@tonidero)
### Other Changes
* Add retries to queryPurchases (#1460) via Cesar de la Vega (@vegaro)
* Initialize shared preferences lazily (#1489) via Toni Rico (@tonidero)
* Create a BaseBillingUseCaseTest (#1486) via Cesar de la Vega (@vegaro)
* Add retries to queryPurchaseHistoryAsync (#1448) via Cesar de la Vega (@vegaro)
* Add `X-Storefront` header to requests (#1484) via Toni Rico (@tonidero)
* Paywalls: Add paywalls UI tests (#1470) via Toni Rico (@tonidero)
* Use state listener in Amazon store (#1483) via Toni Rico (@tonidero)
* Add method to get storefront for Google and Amazon stores (#1482) via Toni Rico (@tonidero)
* Add retries to queryProductDetailsAsync (#1444) via Cesar de la Vega (@vegaro)

## 7.2.4
### Bugfixes
* Prevents queryPurchases from being called twice (#1468) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump fastlane from 2.216.0 to 2.217.0 (#1475) via dependabot[bot] (@dependabot[bot])
* Bump danger from 9.3.2 to 9.4.0 (#1474) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Fix flaky test in BillingWrapperTest (#1477) via Cesar de la Vega (@vegaro)
* Update AGP to 8.1.3 (#1476) via Toni Rico (@tonidero)

## 7.2.3
### RevenueCatUI
* Paywalls: fix template 5 header aspect ratio (#1465) via NachoSoto (@NachoSoto)
* Paywalls: Fix template 1 header aspect ratio (#1466) via Toni Rico (@tonidero)
* Paywalls: Support condensed footer in template 4 (#1469) via Toni Rico (@tonidero)
* `Paywalls`: improve image loading (#1464) via NachoSoto (@NachoSoto)
* `Paywalls`: override locale with paywall localization (#1418) via NachoSoto (@NachoSoto)
### Other Changes
* Paywalls: Fix purchasing regression by providing real activity (#1467) via Toni Rico (@tonidero)
* Bump compile/target version to 34 (#1462) via Toni Rico (@tonidero)
* Update circleci orb to latest version (#1456) via Toni Rico (@tonidero)
* `Snapshots`: fix Fastlane job (#1461) via NachoSoto (@NachoSoto)
* Update gradle plugin to version 8.1.1 (#1458) via Toni Rico (@tonidero)

## 7.2.2
### RevenueCatUI
* `Paywalls`: improve error log when images fail to load (#1454) via NachoSoto (@NachoSoto)
### Other Changes
* Paywall events: Send paywall data with post receipt requests (#1452) via Toni Rico (@tonidero)
* Paywalls: Track paywall events (#1447) via Toni Rico (@tonidero)
* Paywall events: Handle errors parsing specific paywall event lines (#1451) via Toni Rico (@tonidero)
* Paywalls: Move logic for events file helper to common generic class (4) (#1446) via Toni Rico (@tonidero)
* Paywalls: Add paywall events flush logic and tests (3) (#1445) via Toni Rico (@tonidero)
* Paywalls: Support sending paywall events to servers (2) (#1442) via Toni Rico (@tonidero)
* `CircleCI`: fix `record-revenuecatui-snapshots` (#1455) via NachoSoto (@NachoSoto)
* Lower request jitter log level from warning to debug (#1453) via Toni Rico (@tonidero)

## 7.2.1
### RevenueCatUI
* `PaywallActivityLauncher`: new constructor for a generic `ActivityResultCaller` (#1441) via NachoSoto (@NachoSoto)
* Improve fullscreen templates in landscape orientation (#1435) via Toni Rico (@tonidero)
* `Paywalls`: improve Japanese localization (#1439) via NachoSoto (@NachoSoto)
### Other Changes
* Remove side effect from setting purchasesUpdatedListener (#1443) via Cesar de la Vega (@vegaro)
* Paywalls: Store paywall events on disk and API (1) (#1436) via Toni Rico (@tonidero)

## 7.2.0
### New Features
* `StoreProduct`: new `pricePerWeek` and `pricePerYear` (#1426) via NachoSoto (@NachoSoto)
### RevenueCatUI
* Fix RevenueCatUI API issues and add API tests  (#1433) via Toni Rico (@tonidero)
* Paywalls: Add initial snapshot testings for RevenueCatUI library (#1432) via Toni Rico (@tonidero)
* `Paywalls`: new `{{ sub_price_per_week }}` variable (#1427) via NachoSoto (@NachoSoto)
* `Paywalls`: new `{{ sub_relative_discount }}` variable (#1425) via NachoSoto (@NachoSoto)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `a297205` to `0ddee10` (#1431) via dependabot[bot] (@dependabot[bot])
### Other Changes
* `Offering`: restore constructor with no `PaywallData` (#1437) via NachoSoto (@NachoSoto)

## 7.1.1
### RevenueCatUI
* Paywalls: Decrease max aspect ratio for tablets to have a max height (#1422) via Toni Rico (@tonidero)
### Bugfixes
* Improvements on BillingWrapper retrying mechanism  (#1420) via Cesar de la Vega (@vegaro)

## 7.1.0
### New Features
#### ✨📱 Introducing Android Paywalls 🐾🧱

RevenueCat's Paywalls allow you to remotely configure your entire paywall view without any code changes or app updates.
Our paywall templates use native code to deliver smooth, intuitive experiences to your customers when you’re ready to deliver them an Offering; and you can use our Dashboard to pick the right template and configuration to meet your needs.

To use RevenueCat Paywalls on Android, simply:

1. Create a Paywall on the Dashboard for the `Offering` you intend to serve to your customers
2. Add the `RevenueCatUI` dependency to your project:
```groovy build.gradle
implementation 'com.revenuecat.purchases:purchases:7.1.0'
implementation 'com.revenuecat.purchases:purchases-ui:7.1.0'
```
3. Display a paywall:
```kotlin
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
private fun LockedScreen() {
    YourContent()

    PaywallDialog(
        PaywallDialogOptions.Builder()
            .setRequiredEntitlementIdentifier(Constants.ENTITLEMENT_ID)
            .build()
    )
}
```

> **Note**
> Android paywalls is currently behind an experimental flag: `ExperimentalPreviewRevenueCatUIPurchasesAPI`.
> It is safe to release app updates with it. We guarantee that paywalls will continue to work and any changes will always be backwards compatible.
> They are stable, but migration steps may be required in the future. We'll do our best to minimize any changes you have to make.

You can find more information in [our documentation](https://rev.cat/paywalls).

<details>

<summary>List of changes</summary>

* `Paywalls`: Improve PaywallDialog look on tablets (#1419) via Toni Rico (@tonidero)
* `Paywalls`: disallow Markdown links in PurchaseButton (#1421) via NachoSoto (@NachoSoto)
* `Paywalls`: implemented template 5 (#1412) via NachoSoto (@NachoSoto)
* `Paywalls`: Fix wrong view model data after configuration changes (#1417) via Toni Rico (@tonidero)
* `Paywalls`: update template 4 colors (#1410) via NachoSoto (@NachoSoto)
* `Paywalls`: Add bundled font to paywall tester (#1415) via Toni Rico (@tonidero)
* `Paywalls`: Fix purchase button text flashing when changing selected package (#1416) via Toni Rico (@tonidero)
* `Paywalls`: support text3 and accent3 colors (#1409) via NachoSoto (@NachoSoto)
* `Paywalls`: PaywallData errors shouldn't make Offerings fail to decode (#1402) via NachoSoto (@NachoSoto)
* `Paywalls`: convert empty strings to null via NachoSoto (@NachoSoto)
* `Paywalls`: footer view should not render background image via NachoSoto (@NachoSoto)
* `Paywalls`: improve template 4 A11y support via NachoSoto (@NachoSoto)
* `Paywalls`: Add close button option (#1390) via Toni Rico (@tonidero)
* `Paywalls`: fix MarkdownText stealing touches from package buttons (#1411) via NachoSoto (@NachoSoto)
* Fix onDismiss getting called twice in PaywallDialogs (#1408) via Cesar de la Vega (@vegaro)
* `Paywalls`: ignore URL deserialization errors (#1399) via NachoSoto (@NachoSoto)
* Animate transition between loading and loaded paywall (#1404) via Cesar de la Vega (@vegaro)
* Fix button padding in Loading paywall (#1405) via Cesar de la Vega (@vegaro)
* Add test for `packageInfo.localizedDiscount` (#1407) via Cesar de la Vega (@vegaro)
* `Paywalls`: RemoteImage always renders a placeholder on previews via NachoSoto (@NachoSoto)
* `Paywalls`: decode empty images as null via NachoSoto (@NachoSoto)
* `Paywalls`: Allow trigger manual paywall tester release (#1406) via Toni Rico (@tonidero)
* Fix navigation after closing paywall in Paywalls screen (#1403) via Cesar de la Vega (@vegaro)
* `Paywalls`: Add experimental annotation to all public APIs in RevenueCat UI (#1400) via Toni Rico (@tonidero)
* `Paywalls`: improve template 2 layout (#1396) via NachoSoto (@NachoSoto)
* `Paywalls`: template 4 (#1349) via NachoSoto (@NachoSoto)
* `Paywalls`: fix template 3 offer details color (#1394) via NachoSoto (@NachoSoto)
* `Paywalls`: PaywallDialogOptions no longer requires dismissRequest (#1386) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed accessibility across templates (#1392) via NachoSoto (@NachoSoto)
* `Paywalls`: increase PaywallBackground blur radius to match iOS via NachoSoto (@NachoSoto)
* `Paywalls`: use LocalUriHandler for opening links (#1388) via NachoSoto (@NachoSoto)
* `Paywalls`: support for Markdown links (#1387) via NachoSoto (@NachoSoto)
* `Paywalls`: display purchase/restore errors (#1384) via NachoSoto (@NachoSoto)
* `Paywalls`: improve error handling (#1383) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed incorrect background on footer (#1382) via NachoSoto (@NachoSoto)
* `Paywalls`: fix backwards-compatible blurring of default paywall background (#1380) via NachoSoto (@NachoSoto)
* `Paywalls`: polish template 2 spacing (#1381) via NachoSoto (@NachoSoto)
* `Paywalls`: optimize backwards compatible blurring (#1379) via NachoSoto (@NachoSoto)
* `Paywalls`: Predownload offering images if paywalls sdk exists (#1372) via Toni Rico (@tonidero)
* `Paywalls`: PurchaseButton supports gradients (#1378) via NachoSoto (@NachoSoto)
* `Paywalls`: optimize AdaptiveComposable (#1377) via NachoSoto (@NachoSoto)
* `Paywalls`: improve FooterDialog corner radius (#1374) via NachoSoto (@NachoSoto)
* `Paywalls`: optimize PurchaseButton (#1376) via NachoSoto (@NachoSoto)
* `Paywalls`: Enable footer modes in paywall tester paywalls tab (#1368) via Toni Rico (@tonidero)
* `Paywalls`: Support footer in template 3 (#1367) via Toni Rico (@tonidero)
* `Paywalls`: calculate discountRelativeToMostExpensivePerMonth (#1370) via NachoSoto (@NachoSoto)
* `Paywalls`: Add offer details in template 2 when in condensed form (#1371) via Toni Rico (@tonidero)
* `Paywalls`: improve handling of lifetime/custom packages (#1363) via NachoSoto (@NachoSoto)
* `Paywalls`: Support footer in template 1 (#1366) via Toni Rico (@tonidero)
* Add `StoreProduct.pricePerMonth` (#1369) via NachoSoto (@NachoSoto)
* `Paywalls`: Support condensed footer presentation in template 2 (#1365) via Toni Rico (@tonidero)
* `Paywalls`: finished localization support (#1362) via NachoSoto (@NachoSoto)
* `Paywalls`: backwards compatible blurring (#1327) via Andy Boedo (@AndyBoedo)
* `Paywalls`: PaywallViewModel tests (#1357) via NachoSoto (@NachoSoto)
* `Paywalls`: improve LoadingPaywall (#1364) via NachoSoto (@NachoSoto)
* `Paywalls`: Fix paywall compose previews (#1360) via Toni Rico (@tonidero)
* `Paywalls`: Fix proguard rules kotlinx serialization (#1356) via Toni Rico (@tonidero)
* `Paywalls`: Add custom font example to paywalls screen (#1358) via Toni Rico (@tonidero)
* `Paywalls`: Support Google fonts and font families with multiple fonts (#1338) via Toni Rico (@tonidero)
* `Paywalls`: Support custom fonts through FontProvider (#1328) via Toni Rico (@tonidero)
* `Paywalls`: fixed Footer padding (#1354) via NachoSoto (@NachoSoto)
* `Paywalls`: Rename PaywallView to Paywall (#1351) via Toni Rico (@tonidero)
* `Paywalls`: disable PurchaseButton during purchases (#1352) via NachoSoto (@NachoSoto)
* `Paywalls`: enable library publishing (#1353) via NachoSoto (@NachoSoto)
* `Paywalls`: handle "action in progress" state (#1346) via NachoSoto (@NachoSoto)
* `Paywalls`: support {{ sub_duration_in_months }} (#1348) via NachoSoto (@NachoSoto)
* `Paywalls`: disallow purchasing currently subscribed products (#1334) via NachoSoto (@NachoSoto)
* `Paywalls`: new PaywallActivityLauncher.launchIfNeeded methods (#1335) via NachoSoto (@NachoSoto)
* `Paywalls`: PaywallColor supports RGBA (#1332) via NachoSoto (@NachoSoto)
* `Paywalls`: Make DialogScaffold private (#1329) via Toni Rico (@tonidero)
* Add `revenuecatui` `gradle.properties` to specify name of dependency (#1324) via Toni Rico (@tonidero)
* `Paywalls`: log error when failing to load images (#1321) via NachoSoto (@NachoSoto)
* Log errors when displaying default paywall (#1318) via Cesar de la Vega (@vegaro)
* Rename packages to packageIds in `PaywallData` (#1309) via Cesar de la Vega (@vegaro)
* Add support for multiple intro offers in `IntroEligibilityStateView` (#1319) via Cesar de la Vega (@vegaro)
* Fix material theme references to use Material3 versions (#1326) via Toni Rico (@tonidero)
* `Paywalls`: Add support to launch paywall as activity (#1317) via Toni Rico (@tonidero)
* Parse `{{ sub_offer_price_2 }}` and `{{ sub_offer_duration_2 }}` variables (#1313) via Cesar de la Vega (@vegaro)
* `Paywalls`: changed PaywallsTester app icon (#1323) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed PaywallDialog.setRequiredEntitlementIdentifier (#1322) via NachoSoto (@NachoSoto)
* `Paywalls`: Markdown support (#1312) via NachoSoto (@NachoSoto)
* `PaywallsTester`: added template name to offerings list (#1316) via NachoSoto (@NachoSoto)
* `Paywalls`: Update paywall tester to be able to display paywall footer (#1315) via Toni Rico (@tonidero)
* `Paywalls`: Add PaywallFooter composable to present a minified paywall UI that allows for custom paywalls (#1314) via Toni Rico (@tonidero)
* `Paywalls`: use IntroEligibilityStateView (#1311) via NachoSoto (@NachoSoto)
* `PaywallData` validation tests (#1310) via Cesar de la Vega (@vegaro)
* `Paywalls`: implemented LoadingPaywallView with placeholder (#1284) via NachoSoto (@NachoSoto)
* `Paywalls`: created LoadingPaywallView (#1282) via NachoSoto (@NachoSoto)
* Fix template test data (#1308) via Cesar de la Vega (@vegaro)
</details>

### Other Changes
Add `purchases.setOnesignalUserID` (#1304) via Raquel Diez (@Raquel10-RevenueCat)

## 7.0.1
### Bugfixes
* Optimize billing client connection retries (#1300) via Toni Rico (@tonidero)

## 7.0.0
**RevenueCat Android SDK v7** is here!! 😻

This latest release updates the SDK to use BillingClient 6. This version of BillingClient brings little change compared
with BillingClient 5 which brought an entire new subscription model which resulted in large changes across the entire SDK.

The only modification at the API level involves replacing "ProrationMode" with "ReplacementMode". The specific replacement
modes remain unchanged.

If your app doesn't currently use DEFERRED replacement modes, then you should be safe to upgrade to this version without
changes in behavior.

If your app supports product changes using [DEFERRED replacement mode](https://www.revenuecat.com/docs/managing-subscriptions#google-play),
then you can either stick with the previous major version until support for DEFERRED is re-introduced in this major version,
or you can remove DEFERRED replacement options from your app.

See the [Android Native - 6.x to 7.x Migration](https://github.com/RevenueCat/purchases-android/blob/main/migrations/v7-MIGRATION.md) for more details.

If you come from an older version of the RevenueCat SDK, see [Android Native - 5.x to 6.x Migration](https://www.revenuecat.com/docs/android-native-5x-to-6x-migration)
for a more thorough explanation of the new Google subscription model announced with BillingClient 5 and how to take advantage of it.

### Bumped minimum Android SDK version

RevenueCat SDK v7 bumps minimum Android SDK version from Android 4.0 (API level 14) to Android 4.4 (API level 19).

### Support for Google Play InApp Messages

We've added support for [Google Play InApp Messages](https://rev.cat/googleplayinappmessaging) which will show users a snackbar message during grace period and account hold once per day and provide them an opportunity to fix their payment without leaving the app.

InApp Messages are shown by default. If you want to disable this behaviour during configuration of the RevenueCat SDK:

```kotlin
Purchases.configure(
    PurchasesConfiguration.Builder(applicationContext, apiKey)
        .showInAppMessagesAutomatically(false)
        .build()
)
```

### Bugfixes
* Add `awaitRestore` to customEntitlementComputation library (#1275) via Toni Rico (@tonidero)

## 6.9.8
### Bugfixes
* Stops collecting the ANDROID_ID (#1729) via JayShortway (@JayShortway)

## 6.9.7
### Bugfixes
* Fix amazon zero price (#1634) via Cesar de la Vega (@vegaro)

## 6.9.6
### Bugfixes
* Fix `DEFERRED` purchases when passing old product id in invalid format (#1593) via Toni Rico (@tonidero)

## 6.9.5
### Bugfixes
* Catch IllegalStateException and forward StoreProblemError (#1248) via Cesar de la Vega (@vegaro)
### Dependency Updates
* Bump fastlane from 2.215.1 to 2.216.0 (#1268) via dependabot[bot] (@dependabot[bot])
* Bump danger from 9.3.1 to 9.3.2 (#1260) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.214.0 to 2.215.1 (#1256) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Improve offerings start log (#1225) via Toni Rico (@tonidero)
* Make sure to unmock mocked static methods after tests finished (#1205) via Mark Villacampa (@MarkVillacampa)

## 6.9.4
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `b2108fb` to `a297205` (#1210) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Rename `queue` initiation source to `unsynced_active_purchases` (#1214) via Toni Rico (@tonidero)
* Send `initiation_source` in post receipt requests (#1212) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Add `restorePurchases` (#1211) via Toni Rico (@tonidero)
* Unify caching offerings and updating update timestamp (#1206) via Toni Rico (@tonidero)

## 6.9.3
### Performance Improvements
* Optimize SDK initialization when requests executed before any activity starts (#1204) via Toni Rico (@tonidero)
* Optimize diagnostics file management (#1194) via Toni Rico (@tonidero)
### Other Changes
* Use real debug view dependencies in magic weather compose (#1203) via Toni Rico (@tonidero)

## 6.9.2
### Bugfixes
* Fix offering metadata not fulling expanding (#1199) via Josh Holtz (@joshdholtz)
### Other Changes
* Fix deploying extra packages (#1200) via Toni Rico (@tonidero)
* Debug view: Support deploying both noop and debug version of `debugview` package (#1192) via Toni Rico (@tonidero)

## 6.9.1
### Bugfixes
* Fix NoSuchElementException by using poll when accessing serviceRequests (#1190) via Cesar de la Vega (@vegaro)
### Other Changes
* Debug view: rename package (#1191) via Toni Rico (@tonidero)
* Debugview: Add snapshot tests for debug view using Paparazzi (#1187) via Toni Rico (@tonidero)
* Debug view: Add offerings section and purchasing capabilities (#1186) via Toni Rico (@tonidero)
* Debug view: Initial UI + Usage in MagicWeatherCompose (#1075) via Toni Rico (@tonidero)
* Remove customEntitlementComputation flavor for non-purchases modules (#1180) via Toni Rico (@tonidero)

## 6.9.0
### New Features
* Make coroutines APIs stable (#1184) via Toni Rico (@tonidero)
* Add coroutines support for `logIn`, `logOut`, `restore`, `syncPurchases`, and `getProducts` (#1177) via Cesar de la Vega (@vegaro)
### Bugfixes
* Support receiving productId with basePlanId in upgrades/downgrades (#1183) via Toni Rico (@tonidero)
### Other Changes
* Add setup_dev (#1185) via Cesar de la Vega (@vegaro)
* Remove unnecessary buildToolsVersion config (#1182) via Toni Rico (@tonidero)
* Update AGP to 8.1.0 (#1181) via Toni Rico (@tonidero)
* Update Kotlin from 1.6.21 to 1.7.21 (#1179) via Toni Rico (@tonidero)
* Fix sample app build by supporting snapshots versions (#1178) via Toni Rico (@tonidero)
* Auto-update sample apps SDK versions (#1176) via Toni Rico (@tonidero)
* Run linter in CustomEntitlementComputation sample app (#1175) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Integration tests (#1164) via Toni Rico (@tonidero)

## 6.8.0
### New Features

Introduced Custom Entitlements Computation mode.

This is new library intended for apps that will do their own entitlement computation separate from RevenueCat. It's distributed as a separate artifact in Maven.

Apps using this mode rely on webhooks to signal their backends to refresh entitlements with RevenueCat.

See the [demo app for an example and usage instructions](https://github.com/RevenueCat/purchases-android/tree/main/examples/CustomEntitlementComputationSample).

* Custom entitlements: add README and other improvements (#1167) via Andy Boedo (@aboedo)
* Update Custom Entitlements Sample app (#1166) via Andy Boedo (@aboedo)
* purchase coroutine (#1142) via Andy Boedo (@aboedo)
* Add switchUser (#1156) via Cesar de la Vega (@vegaro)
* CustomEntitlementsComputation: disable first listener callback when set (#1152) via Andy Boedo (@aboedo)
* CustomEntitlementsComputation: Prevent posting subscriber attributes in post receipt (#1151) via Andy Boedo (@aboedo)
* Fix `customEntitlementComputation` library deployment (#1169) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Configure method for customEntitlementComputation mode (#1168) via Toni Rico (@tonidero)
* Add publish system for customEntitlementComputation package (#1149) via Cesar de la Vega (@vegaro)
* Use purchase coroutine in CustomEntitlementComputationSample (#1162) via Cesar de la Vega (@vegaro)
* Adds CustomEntitlementComputationSample (#1160) via Cesar de la Vega (@vegaro)
* Fix tests in customEntitlementComputation after merges (#1161) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Remove custom entitlement computation flavor for amazon module (#1158) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Generate dokka docs only for defaults flavor (#1159) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Create different PurchasesConfiguration that requires an appUserId parameter (#1154) via Toni Rico (@tonidero)
* CustomEntitlementComputation: New Purchases class (#1153) via Toni Rico (@tonidero)
* CustomEntitlementComputation: Disable automatic cache refresh (#1157) via Toni Rico (@tonidero)
* Add `customEntitlementComputation` flavor (#1147) via Toni Rico (@tonidero)
* Make `customEntitlementComputation` singular (#1148) via Toni Rico (@tonidero)
* Disable offline entitlements in custom entitlements computation mode (#1146) via Toni Rico (@tonidero)
* Remove integration test flavor (#1143) via Toni Rico (@tonidero)
* Add header to requests when in custom entitlement computation mode (#1145) via Toni Rico (@tonidero)
* Add internal customEntitlementsComputation mode to app config (#1141) via Toni Rico (@tonidero)

### New Coroutines
* `awaitPurchase` is available as a coroutine-friendly alternative to `purchase()`. (#1142) via Andy Boedo (@aboedo)

### Dependency Updates
* Bump fastlane from 2.213.0 to 2.214.0 (#1140) via dependabot[bot] (@dependabot[bot])

### Other changes
* CI: make all Codecov jobs informational (#1155) via Cesar de la Vega (@vegaro)
* Creates PurchasesOrchestrator (#1144) via Cesar de la Vega (@vegaro)

## 6.7.0
### New Features
* Diagnostics: Add diagnostics for FEATURE_NOT_SUPPORTED error (#1137) via Mark Villacampa (@MarkVillacampa)
### Other Changes
* Trusted entitlements: Support post params hash header (#1136) via Toni Rico (@tonidero)

## 6.6.0
### New Features
* `Trusted Entitlements`: made API stable (#1105) via NachoSoto (@NachoSoto)

This new feature prevents MitM attacks between the SDK and the RevenueCat server.
With verification enabled, the SDK ensures that the response created by the server was not modified by a third-party, and the entitlements received are exactly what was sent.
This is 100% opt-in. `EntitlementInfos` have a new `VerificationResult` property, which will indicate the validity of the responses when this feature is enabled.

```kotlin
fun configureRevenueCat() {
    val configuration = PurchasesConfiguration.Builder(context, apiKey)
        .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
        .build()
    Purchases.configure(configuration)
}
```
### Experimental features
* Add await offerings (#1096) via Cesar de la Vega (@vegaro)
### Bugfixes
* Fix issue updating customer info on app open (#1128) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `13773d2` to `b2108fb` (#1095) via dependabot[bot] (@dependabot[bot])
### Other Changes
* [PurchaseTester] Add option to purchase an arbitrary product id (#1099) via Mark Villacampa (@MarkVillacampa)
* Fix release path after module refactor (#1129) via Toni Rico (@tonidero)
* Fix load shedder integration tests (#1125) via Toni Rico (@tonidero)
* Trusted entitlements: New trusted entitlements signature format (#1117) via Toni Rico (@tonidero)
* Fix integration tests and change to a different project (#1123) via Toni Rico (@tonidero)
* Move files into src/main/kotlin (#1122) via Cesar de la Vega (@vegaro)
* Remove public module (#1113) via Cesar de la Vega (@vegaro)
* Remove common module (#1106) via Cesar de la Vega (@vegaro)
* Fix flaky integration tests: Wait for coroutines to finish before continuing (#1120) via Toni Rico (@tonidero)
* Move amazon module into purchases (#1112) via Cesar de la Vega (@vegaro)
* Trusted entitlements: Add IntermediateSignatureHelper to handle intermediate signature verification process (#1110) via Toni Rico (@tonidero)
* Trusted entitlements: Add Signature type to process new signature response format (#1109) via Toni Rico (@tonidero)
* [EXTERNAL] Add `awaitCustomerInfo` / coroutines tests to `TrustedEntitlementsInformationalModeIntegrationTest` (#1077) via @pablo-guardiola (#1107) via Toni Rico (@tonidero)
* Remove feature:google module (#1104) via Cesar de la Vega (@vegaro)
* Remove identity module (#1103) via Cesar de la Vega (@vegaro)
* Remove subscriber attributes module (#1102) via Cesar de la Vega (@vegaro)
* Delete utils module (#1098) via Cesar de la Vega (@vegaro)
* Remove strings module (#1097) via Cesar de la Vega (@vegaro)
* Update CHANGELOG.md to include external contribution (#1100) via Cesar de la Vega (@vegaro)
* [EXTERNAL] Add missing `fetchPolicy` parameter to `awaitCustomerInfo` API (#1086) via @pablo-guardiola (#1090) via Toni Rico (@tonidero)

## 6.5.2
### Dependency Updates
* Update billing library to 5.2.1 (#1092) via Toni Rico (@tonidero)
### Other Changes
* Fix backend integration tests (#1091) via Toni Rico (@tonidero)
* Compose sample app: refactor listener (#1083) via Toni Rico (@tonidero)

## 6.5.1
### Bugfixes
* Default customer info schema version to latest known by SDK (#1080) via Toni Rico (@tonidero)
* Handle other diagnostics-related exceptions (#1076) via Toni Rico (@tonidero)
* Return error in queryPurchases if error connecting to billing client (#1072) via Toni Rico (@tonidero)
### Other Changes
* Fix offline entitlements integration tests (#1085) via Toni Rico (@tonidero)
* Add defaultsRelease variant tests run configuration (#1074) via Toni Rico (@tonidero)
* Compose sample app: move to gradle catalog (#1081) via Toni Rico (@tonidero)
* Compose sample app: automate builds (#1082) via Toni Rico (@tonidero)
* Compose sample app (#1056) via Toni Rico (@tonidero)
* Migrate to Gradle version catalog (#1033) via @mikescamell (#1059) via Cesar de la Vega (@vegaro)
* Trusted entitlements: Add logs with verification mode (#1067) via Toni Rico (@tonidero)
* Sync pending purchases before getting customer info (#1073) via Toni Rico (@tonidero)
* Refactor syncing pending transactions logic out of `Purchases` (#1058) via Toni Rico (@tonidero)
* Refactor CustomerInfo listener and cache logic into CustomerInfoUpdater (#1052) via Toni Rico (@tonidero)
* Trusted entitlements: Add integration tests (#1071) via Toni Rico (@tonidero)
* Trusted entitlements: Add internal mechanism to force signing errors for tests (#1070) via Toni Rico (@tonidero)

## 6.5.0
### New experimental features
* Trusted entitlements (#1048) via Toni Rico (@tonidero)

This new feature prevents MitM attacks between the SDK and the RevenueCat server.
With verification enabled, the SDK ensures that the response created by the server was not modified by a third-party, and the entitlements received are exactly what was sent.
This is 100% opt-in. `EntitlementInfos` have a new `VerificationResult` property, which will indicate the validity of the responses when this feature is enabled.

This feature is experimental and requires opt in. It's also only available in Kotlin.

```kotlin
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
fun configureRevenueCat() {
    val configuration = PurchasesConfiguration.Builder(context, apiKey)
        .informationalVerificationModeAndDiagnosticsEnabled(true)
        .build()
    Purchases.configure(configuration)
}
```
* Add Coroutine support to Purchases#getCustomerInfo (#1012) via Cesar de la Vega (@vegaro)
### Bugfixes
* [EXTERNAL] Fix `README` `CONTRIBUTING` guide broken link (#1035) via @pablo-guardiola (#1036) via Toni Rico (@tonidero)
### Dependency Updates
* Update targetSDK version to 33 (#1050) via Toni Rico (@tonidero)
* Update gradle plugin to 8.0.2 (#1049) via Toni Rico (@tonidero)
* Bump danger from 9.3.0 to 9.3.1 (#1054) via dependabot[bot] (@dependabot[bot])
* [EXTERNAL] Update Kover to 0.7.0 (#1031) via @mikescamell (#1037) via Toni Rico (@tonidero)
### Other Changes
* [EXTERNAL] Add `ExperimentalPreviewRevenueCatPurchasesAPI` opt-in requirement to `Purchases.awaitCustomerInfo()` (#1060) (#1065) via Toni Rico (@tonidero)
* Update to latest android executor image (#1062) via Toni Rico (@tonidero)
* Diagnostics: Track http requests as counters  (#1047) via Toni Rico (@tonidero)
* Fix backend integration test flakiness (#1053) via Toni Rico (@tonidero)
* [EXTERNAL] Replace `@Deprecated` usage of `skus` by `productIds` from`PurchasedProductsFetcher` (#986) via @pablo-guardiola (#1044) via Toni Rico (@tonidero)
* [EXTERNAL] Add `MagicWeather` app note to the main `README` and fix a typo (#1041) (#1043) via Toni Rico (@tonidero)
* Detekt enable trailing comma (#1046) via Toni Rico (@tonidero)
* Detekt update to 1.23.0 and run autocorrect (#1045) via Toni Rico (@tonidero)
* [PurchaseTester] Update CustomerInfo from listener in overview screen (#1034) via Toni Rico (@tonidero)

## 6.4.0
### New Features
* Offline entitlements support (#1030) via Toni Rico (@tonidero)
### Bugfixes
* Fix billing connection error when querying purchases early in the process lifetime (#1032) via Toni Rico (@tonidero)
### Performance Improvements
* Perform product entitlement mapping request after more critical requests (#1017) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane from 2.212.2 to 2.213.0 (#1024) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Get offerings response from disk cache if available (#1029) via Toni Rico (@tonidero)
* Improve offline entitlements logs to mention limitations of this mode (#1039) via Toni Rico (@tonidero)
* Improve error message when backend returns internal error code (#1038) via Toni Rico (@tonidero)
* PurchaseTester: Add new UI to configure internal proxy behavior (#1016) via Toni Rico (@tonidero)
* Updated readme to include links to migration guides (#1021) via Marcos Castany (@mcastany)
* Store offerings response in SharedPreferences (#1028) via Toni Rico (@tonidero)
* Refactor offerings code out of Purchases (#1027) via Toni Rico (@tonidero)

## 6.3.0
### New Features
* Support DEFERRED mode (#985) via swehner (@swehner)
* Add completion callback to syncPurchases API (#1002) via Toni Rico (@tonidero)
### Bugfixes
* Workaround bug in android 4 for JSON objects with List<String> (#942) via Andy Boedo (@aboedo)
### Dependency Updates
* Bump fastlane-plugin-revenuecat_internal from `fe45299` to `13773d2` (#1015) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Bump dokka to 1.8.10 to support Gradle 8 (#1009) via Toni Rico (@tonidero)
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
