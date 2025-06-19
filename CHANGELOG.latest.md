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
