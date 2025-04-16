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
