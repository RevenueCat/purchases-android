### New Features
* Add proration modes to post to backend (#977) via swehner (@swehner)
* Added ENTITLEMENTS_COMPUTED_ON_DEVICE (#939) via Cesar de la Vega (@vegaro)
### Bugfixes
* Add non-subscriptions support to offline customer info (#958) via Cesar de la Vega (@vegaro)
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
