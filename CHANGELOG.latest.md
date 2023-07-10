### New Features
* `Trusted Entitlements`: made API stable (#1105) via NachoSoto (@NachoSoto)

This new feature prevents MitM attacks between the SDK and the RevenueCat server.
With verification enabled, the SDK ensures that the response created by the server was not modified by a third-party, and the entitlements received are exactly what was sent.
This is 100% opt-in. `EntitlementInfos` have a new `VerificationResult` property, which will indicate the validity of the responses when this feature is enabled.

```kotlin
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
