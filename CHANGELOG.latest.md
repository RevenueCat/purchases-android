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
