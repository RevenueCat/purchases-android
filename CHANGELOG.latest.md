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
* [RENOVATE] Update build-dependencies (#2515) via RevenueCat Git Bot (@RCGitBot)
* [RENOVATE] Update build-dependencies (#2472) via RevenueCat Git Bot (@RCGitBot)
* Increase compile/target SDK version to 35 (#2525) via Toni Rico (@tonidero)
* Fix snapshot deployment (#2526) via Toni Rico (@tonidero)
* Update CircleCI android orb (#2521) via Toni Rico (@tonidero)
* Bump AGP to 8.10.1 and Gradle to 8.14.2 (#2440) via Jaewoong Eum (@skydoves)
* Fixes compilation of the CEC sample app. (#2512) via JayShortway (@JayShortway)
* Update CustomEntitlementComputation sample app kotlin version (#2510) via Toni Rico (@tonidero)
* Fix `Switch` component previews (#2509) via Toni Rico (@tonidero)
        
