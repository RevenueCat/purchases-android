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
