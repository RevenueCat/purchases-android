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
