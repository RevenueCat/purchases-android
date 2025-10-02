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
