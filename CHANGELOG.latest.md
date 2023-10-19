### New Features
#### ✨ Introducing Android Paywalls (beta) 🐾🧱

RevenueCat's Paywalls allow you to to remotely configure your entire paywall view without any code changes or app updates.
Our paywall templates use native code to deliver smooth, intuitive experiences to your customers when you’re ready to deliver them an Offering; and you can use our Dashboard to pick the right template and configuration to meet your needs.

To use RevenueCat Paywalls on Android, simply:

1. Create a Paywall on the Dashboard for the `Offering` you intend to serve to your customers
2. Add the `purchases-ui` dependency to your project:
```groovy
implementation 'com.revenuecat.purchases:purchases-ui:7.1.0-beta.2'
```
3. Display the paywall in your app:
```kotlin
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

Check out [our documentation](https://www.revenuecat.com/docs/paywalls) for more information.


<details>

<summary>List of changes</summary>

* Add `StoreProduct.pricePerMonth` (#1369) via NachoSoto (@NachoSoto)
* Paywalls: Support condensed footer presentation in template 2 (#1365) via Toni Rico (@tonidero)
* Paywalls: Support Google fonts and font families with multiple fonts (#1338) via Toni Rico (@tonidero)
* Paywalls: Support custom fonts through FontProvider (#1328) via Toni Rico (@tonidero)
* Paywalls: Enable footer modes in paywall tester paywalls tab (#1368) via Toni Rico (@tonidero)
* `Paywalls`: calculate `discountRelativeToMostExpensivePerMonth` (#1370) via NachoSoto (@NachoSoto)
* `Paywalls`: improve handling of lifetime/custom packages (#1363) via NachoSoto (@NachoSoto)
* `Paywalls`: finished localization support (#1362) via NachoSoto (@NachoSoto)
* Paywalls: backwards compatible blurring (#1327) via Andy Boedo (@aboedo)
* `Paywalls`: improve `LoadingPaywall` (#1364) via NachoSoto (@NachoSoto)
* Paywalls: Fix paywall compose previews (#1360) via Toni Rico (@tonidero)
* Paywalls: Add custom font example to paywalls screen (#1358) via Toni Rico (@tonidero)
* Add sample templates to paywalls tester  (#1330) via Cesar de la Vega (@vegaro)
* Expose new `TestStoreProduct` (#1333) via NachoSoto (@NachoSoto)
* Add support for multiple intro offers in IntroEligibilityStateView (#1319) via Cesar de la Vega (@vegaro)
* Paywalls: Add support to launch paywall as activity (#1317) via Toni Rico (@tonidero)
* Parse `{{ sub_offer_price_2 }}` and `{{ sub_offer_duration_2 }}` variables (#1313) via Cesar de la Vega (@vegaro)
* Paywalls: Add PaywallFooter composable to present a minified paywall UI that allows for custom paywalls (#1314) via Toni Rico (@tonidero)
* Paywalls: Add API to display paywall as a composable dialog (#1297) via Toni Rico (@tonidero)
* Template 3 (#1294) via Cesar de la Vega (@vegaro)
* PaywallData validation (#1273) via Cesar de la Vega (@vegaro)
* Creates default PaywallData (#1261) via Cesar de la Vega (@vegaro)
* Paywalls: Add support for `total_price_and_per_month` variable in paywalls (#1285) via Toni Rico (@tonidero)
* Paywalls: Add support for `sub_offer_duration` variable in paywalls (#1283) via Toni Rico (@tonidero)
* Paywalls: Add support for `sub_offer_price` variable processing in paywalls (#1279) via Toni Rico (@tonidero)
* Created enum with variable names (#1270) via Cesar de la Vega (@vegaro)
* Paywalls: Add support for SUB_DURATION variable in paywalls (#1276) via Toni Rico (@tonidero)
* Paywalls: Implement `price_per_period` variable support (#1269) via Toni Rico (@tonidero)
* Paywalls: Add support for `sub_period` variable (#1264) via Toni Rico (@tonidero)
* Paywalls: Add `product_name` variable support (#1263) via Toni Rico (@tonidero)
* Adds localizedConfiguration (#1238) via Cesar de la Vega (@vegaro)
* Paywalls: Add variable processing to paywall strings (#1251) via Toni Rico (@tonidero)
* Add `formattedPricePerMonth` helper to `StoreProduct` and `PricingPhase` (#1255) via Toni Rico (@tonidero)
* Add `valueInMonths` helper to `Period` class (#1250) via Toni Rico (@tonidero)
* Show dialog after completed or error purchasing (#1246) via Cesar de la Vega (@vegaro)
* Hook up purchasing listeners (#1243) via Cesar de la Vega (@vegaro)
* Add callToActionSecondaryBackground (#1236) via Cesar de la Vega (@vegaro)
* Add configForLocale to PaywallData  (#1227) via Cesar de la Vega (@vegaro)
* Add revision and remove default_locale (#1226) via Cesar de la Vega (@vegaro)
* Serializes PaywallData (#1222) via Cesar de la Vega (@vegaro)
* Adds kotlinx serialization (#1221) via Cesar de la Vega (@vegaro)
* Add PaywallData and its classes (#1219) via Cesar de la Vega (@vegaro)
* `Paywalls`: fixed `Footer` padding (#1354) via NachoSoto (@NachoSoto)
* Paywalls: Rename `PaywallView` to `Paywall` (#1351) via Toni Rico (@tonidero)
* `Paywalls`: disable `PurchaseButton` during purchases (#1352) via NachoSoto (@NachoSoto)
* `Paywalls`: enable library publishing (#1353) via NachoSoto (@NachoSoto)
* `Paywalls`: handle "action in progress" state (#1346) via NachoSoto (@NachoSoto)
* `Paywalls`: support `{{ sub_duration_in_months }}` (#1348) via NachoSoto (@NachoSoto)
* `Paywalls`: new `PaywallActivityLauncher.launchIfNeeded` methods (#1335) via NachoSoto (@NachoSoto)
* `Paywalls`: `ViewThatFits` equivalent to improve `Footer` (#1258) via NachoSoto (@NachoSoto)
* `Paywalls`: polish template 1 (#1343) via NachoSoto (@NachoSoto)
* `Paywalls`: polish template 3 (#1344) via NachoSoto (@NachoSoto)
* `Paywalls`: improved default paywall (#1342) via NachoSoto (@NachoSoto)
* `Paywalls`: display default template in paywalls tester (#1341) via NachoSoto (@NachoSoto)
* `Paywalls`: animate package selection transitions (#1337) via NachoSoto (@NachoSoto)
* `Paywalls`: polished `Template2`, `PurchaseButton`, and `Footer` (#1336) via NachoSoto (@NachoSoto)
* Add revenuecatui gradle.properties to specify name of dependency (#1324) via Toni Rico (@tonidero)
* `Paywalls`: log error when failing to load images (#1321) via NachoSoto (@NachoSoto)
* Log errors when displaying default paywall (#1318) via Cesar de la Vega (@vegaro)
* Rename packages to packageIds in PaywallData (#1309) via Cesar de la Vega (@vegaro)
* Fix material theme references to use Material3 versions (#1326) via Toni Rico (@tonidero)
* `Paywalls`: changed PaywallsTester app icon (#1323) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed `PaywallDialog.setRequiredEntitlementIdentifier` (#1322) via NachoSoto (@NachoSoto)
* `Paywalls`: Markdown support (#1312) via NachoSoto (@NachoSoto)
* `PaywallsTester`: added template name to offerings list (#1316) via NachoSoto (@NachoSoto)
* Paywalls: Update paywall tester to be able to display paywall footer (#1315) via Toni Rico (@tonidero)
* `Paywalls`: use `IntroEligibilityStateView` (#1311) via NachoSoto (@NachoSoto)
* PaywallData validation tests (#1310) via Cesar de la Vega (@vegaro)
* `Paywalls`: implemented `LoadingPaywallView` with `placeholder` (#1284) via NachoSoto (@NachoSoto)
* `Paywalls`: created `LoadingPaywallView` (#1282) via NachoSoto (@NachoSoto)
* Fix template test data (#1308) via Cesar de la Vega (@vegaro)
* `Paywalls`: changed `PurchaseButton` to use `IntroEligibilityStateView` (#1305) via NachoSoto (@NachoSoto)
* Refactor checking for available packages when creating package configuration (#1307) via Cesar de la Vega (@vegaro)
* Extract templates from TestData (#1299) via Cesar de la Vega (@vegaro)
* Paywalls: Add some tests to intro eligibility calculation (#1303) via Toni Rico (@tonidero)
* Paywalls: Template2 improve select button (#1301) via Toni Rico (@tonidero)
* Paywalls: Add multiple offers fields to the paywall response and processing (#1302) via Toni Rico (@tonidero)
* `Paywalls`: Intro Eligibility dependent composable (#1286) via NachoSoto (@NachoSoto)
* `Paywalls`: `InternalPaywallView` now takes a `PaywallViewMode` parameter (#1281) via NachoSoto (@NachoSoto)
* `Paywalls`: new `PaywallIcon` (#1274) via NachoSoto (@NachoSoto)
* Paywalls: Add tests for additional periods in variable processor (#1278) via Toni Rico (@tonidero)
* Paywalls: Adds some initial spanish strings and some initial preview (#1266) via Toni Rico (@tonidero)
* `Paywalls`: added transition when loading images (#1272) via NachoSoto (@NachoSoto)
* `Paywalls`: initial template 1 implementation (#1259) via NachoSoto (@NachoSoto)
* Created `Footer` (#1245) via NachoSoto (@NachoSoto)
* Paywalls: Add more tests in `VariableProcessorTest` (#1262) via Toni Rico (@tonidero)
* Paywalls: Add template configuration factory tests (#1249) via Toni Rico (@tonidero)
* `Paywalls`: extracted `ApplicationContext` interface and `MockViewModel` (#1257) via NachoSoto (@NachoSoto)
* PaywallTester: Improve AppInfo screen to allow LogIn/LogOut and display debug menu (#1253) via Toni Rico (@tonidero)
* PaywallTester: Do not show error dialog if cancellation (#1252) via Toni Rico (@tonidero)
* Paywalls: Map data inputs into consumable TemplateConfiguration class (#1242) via Toni Rico (@tonidero)
* `Paywalls`: new `TestData` to store fake paywalls (#1239) via NachoSoto (@NachoSoto)
* `Paywalls`: created `PaywallBackground` (#1240) via NachoSoto (@NachoSoto)
* Paywalls: Organize files into packages (#1241) via Toni Rico (@tonidero)
* Add PaywallDataTest (#1237) via Cesar de la Vega (@vegaro)
* Paywalls: Use coil to load images (#1235) via Toni Rico (@tonidero)
* Paywalls: Add logger class to unify logging behavior (#1233) via Toni Rico (@tonidero)
* Paywalls: Add initial Template2 UI and using colors and texts in paywall (#1232) via Toni Rico (@tonidero)
* Paywalls: Use coroutines to interact with SDK (#1224) via Toni Rico (@tonidero)
* Paywalls: Add simple paywall and use in tester app (#1223) via Toni Rico (@tonidero)
* PaywallTester: Visualize offerings in offerings tab and navigate to new screen (#1220) via Toni Rico (@tonidero)
* Add RevenueCatUI module and initial API (#1213) via Toni Rico (@tonidero)
* Paywalls: Add restore paywall callbacks (#1350) via Toni Rico (@tonidero)
* Update to use name instead of id when creating sample offering  (#1347) via Cesar de la Vega (@vegaro)
* Fix loading another template in Paywalls screen (#1345) via Cesar de la Vega (@vegaro)
* Paywalls: Make DialogScaffold private (#1329) via Toni Rico (@tonidero)
* Better handling of packages not found for id error (#1295) via Cesar de la Vega (@vegaro)
* Add preview for default data (#1292) via Cesar de la Vega (@vegaro)
* Paywalls: Fix state update upon locale changes (#1287) via Toni Rico (@tonidero)
* Paywalls: Fix locale selection logic for previews (#1267) via Toni Rico (@tonidero)
* Fix OfferingsParser exceptions being swallowed (#1228) via Cesar de la Vega (@vegaro)
* Fix tests that broke when adding PaywallData (#1229) via Cesar de la Vega (@vegaro)
* `Paywalls`: disallow purchasing currently subscribed products (#1334) via NachoSoto (@NachoSoto)
* `Paywalls`: `PaywallColor` supports RGBA (#1332) via NachoSoto (@NachoSoto)
* offerdetails is optional via Cesar de la Vega (@vegaro)
* Nightly deploy of paywall tester (#1231) via Cesar de la Vega (@vegaro)
* Paywalls: Use paywall data in paywall (#1230) via Toni Rico (@tonidero)
* Create paywall tester app (#1218) via Toni Rico (@tonidero)

</details>
