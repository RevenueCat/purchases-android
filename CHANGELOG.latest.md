### New Features
#### ✨📱 Introducing Android Paywalls 🐾🧱

RevenueCat's Paywalls allow you to remotely configure your entire paywall view without any code changes or app updates.
Our paywall templates use native code to deliver smooth, intuitive experiences to your customers when you’re ready to deliver them an Offering; and you can use our Dashboard to pick the right template and configuration to meet your needs.

To use RevenueCat Paywalls on Android, simply:

1. Create a Paywall on the Dashboard for the `Offering` you intend to serve to your customers
2. Add the `RevenueCatUI` dependency to your project:
```groovy build.gradle
implementation 'com.revenuecat.purchases:purchases:7.1.0'
implementation 'com.revenuecat.purchases:purchases-ui:7.1.0'
```
3. Display a paywall:
```kotlin
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
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

> **Note**
> Android paywalls is currently behind an experimental flag: `ExperimentalPreviewRevenueCatUIPurchasesAPI`.
> It is safe to release app updates with it. We guarantee that paywalls will continue to work and any changes will always be backwards compatible.
> They are stable, but migration steps may be required in the future. We'll do our best to minimize any changes you have to make.

You can find more information in [our documentation](https://rev.cat/paywalls).

<details>

<summary>List of changes</summary>

* `Paywalls`: Improve PaywallDialog look on tablets (#1419) via Toni Rico (@tonidero)
* `Paywalls`: disallow Markdown links in PurchaseButton (#1421) via NachoSoto (@NachoSoto)
* `Paywalls`: implemented template 5 (#1412) via NachoSoto (@NachoSoto)
* `Paywalls`: Fix wrong view model data after configuration changes (#1417) via Toni Rico (@tonidero)
* `Paywalls`: update template 4 colors (#1410) via NachoSoto (@NachoSoto)
* `Paywalls`: Add bundled font to paywall tester (#1415) via Toni Rico (@tonidero)
* `Paywalls`: Fix purchase button text flashing when changing selected package (#1416) via Toni Rico (@tonidero)
* `Paywalls`: support text3 and accent3 colors (#1409) via NachoSoto (@NachoSoto)
* `Paywalls`: PaywallData errors shouldn't make Offerings fail to decode (#1402) via NachoSoto (@NachoSoto)
* `Paywalls`: convert empty strings to null via NachoSoto (@NachoSoto)
* `Paywalls`: footer view should not render background image via NachoSoto (@NachoSoto)
* `Paywalls`: improve template 4 A11y support via NachoSoto (@NachoSoto)
* `Paywalls`: Add close button option (#1390) via Toni Rico (@tonidero)
* `Paywalls`: fix MarkdownText stealing touches from package buttons (#1411) via NachoSoto (@NachoSoto)
* Fix onDismiss getting called twice in PaywallDialogs (#1408) via Cesar de la Vega (@vegaro)
* `Paywalls`: ignore URL deserialization errors (#1399) via NachoSoto (@NachoSoto)
* Animate transition between loading and loaded paywall (#1404) via Cesar de la Vega (@vegaro)
* Fix button padding in Loading paywall (#1405) via Cesar de la Vega (@vegaro)
* Add test for `packageInfo.localizedDiscount` (#1407) via Cesar de la Vega (@vegaro)
* `Paywalls`: RemoteImage always renders a placeholder on previews via NachoSoto (@NachoSoto)
* `Paywalls`: decode empty images as null via NachoSoto (@NachoSoto)
* `Paywalls`: Allow trigger manual paywall tester release (#1406) via Toni Rico (@tonidero)
* Fix navigation after closing paywall in Paywalls screen (#1403) via Cesar de la Vega (@vegaro)
* `Paywalls`: Add experimental annotation to all public APIs in RevenueCat UI (#1400) via Toni Rico (@tonidero)
* `Paywalls`: improve template 2 layout (#1396) via NachoSoto (@NachoSoto)
* `Paywalls`: template 4 (#1349) via NachoSoto (@NachoSoto)
* `Paywalls`: fix template 3 offer details color (#1394) via NachoSoto (@NachoSoto)
* `Paywalls`: PaywallDialogOptions no longer requires dismissRequest (#1386) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed accessibility across templates (#1392) via NachoSoto (@NachoSoto)
* `Paywalls`: increase PaywallBackground blur radius to match iOS via NachoSoto (@NachoSoto)
* `Paywalls`: use LocalUriHandler for opening links (#1388) via NachoSoto (@NachoSoto)
* `Paywalls`: support for Markdown links (#1387) via NachoSoto (@NachoSoto)
* `Paywalls`: display purchase/restore errors (#1384) via NachoSoto (@NachoSoto)
* `Paywalls`: improve error handling (#1383) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed incorrect background on footer (#1382) via NachoSoto (@NachoSoto)
* `Paywalls`: fix backwards-compatible blurring of default paywall background (#1380) via NachoSoto (@NachoSoto)
* `Paywalls`: polish template 2 spacing (#1381) via NachoSoto (@NachoSoto)
* `Paywalls`: optimize backwards compatible blurring (#1379) via NachoSoto (@NachoSoto)
* `Paywalls`: Predownload offering images if paywalls sdk exists (#1372) via Toni Rico (@tonidero)
* `Paywalls`: PurchaseButton supports gradients (#1378) via NachoSoto (@NachoSoto)
* `Paywalls`: optimize AdaptiveComposable (#1377) via NachoSoto (@NachoSoto)
* `Paywalls`: improve FooterDialog corner radius (#1374) via NachoSoto (@NachoSoto)
* `Paywalls`: optimize PurchaseButton (#1376) via NachoSoto (@NachoSoto)
* `Paywalls`: Enable footer modes in paywall tester paywalls tab (#1368) via Toni Rico (@tonidero)
* `Paywalls`: Support footer in template 3 (#1367) via Toni Rico (@tonidero)
* `Paywalls`: calculate discountRelativeToMostExpensivePerMonth (#1370) via NachoSoto (@NachoSoto)
* `Paywalls`: Add offer details in template 2 when in condensed form (#1371) via Toni Rico (@tonidero)
* `Paywalls`: improve handling of lifetime/custom packages (#1363) via NachoSoto (@NachoSoto)
* `Paywalls`: Support footer in template 1 (#1366) via Toni Rico (@tonidero)
* Add `StoreProduct.pricePerMonth` (#1369) via NachoSoto (@NachoSoto)
* `Paywalls`: Support condensed footer presentation in template 2 (#1365) via Toni Rico (@tonidero)
* `Paywalls`: finished localization support (#1362) via NachoSoto (@NachoSoto)
* `Paywalls`: backwards compatible blurring (#1327) via Andy Boedo (@AndyBoedo)
* `Paywalls`: PaywallViewModel tests (#1357) via NachoSoto (@NachoSoto)
* `Paywalls`: improve LoadingPaywall (#1364) via NachoSoto (@NachoSoto)
* `Paywalls`: Fix paywall compose previews (#1360) via Toni Rico (@tonidero)
* `Paywalls`: Fix proguard rules kotlinx serialization (#1356) via Toni Rico (@tonidero)
* `Paywalls`: Add custom font example to paywalls screen (#1358) via Toni Rico (@tonidero)
* `Paywalls`: Support Google fonts and font families with multiple fonts (#1338) via Toni Rico (@tonidero)
* `Paywalls`: Support custom fonts through FontProvider (#1328) via Toni Rico (@tonidero)
* `Paywalls`: fixed Footer padding (#1354) via NachoSoto (@NachoSoto)
* `Paywalls`: Rename PaywallView to Paywall (#1351) via Toni Rico (@tonidero)
* `Paywalls`: disable PurchaseButton during purchases (#1352) via NachoSoto (@NachoSoto)
* `Paywalls`: enable library publishing (#1353) via NachoSoto (@NachoSoto)
* `Paywalls`: handle "action in progress" state (#1346) via NachoSoto (@NachoSoto)
* `Paywalls`: support {{ sub_duration_in_months }} (#1348) via NachoSoto (@NachoSoto)
* `Paywalls`: disallow purchasing currently subscribed products (#1334) via NachoSoto (@NachoSoto)
* `Paywalls`: new PaywallActivityLauncher.launchIfNeeded methods (#1335) via NachoSoto (@NachoSoto)
* `Paywalls`: PaywallColor supports RGBA (#1332) via NachoSoto (@NachoSoto)
* `Paywalls`: Make DialogScaffold private (#1329) via Toni Rico (@tonidero)
* Add `revenuecatui` `gradle.properties` to specify name of dependency (#1324) via Toni Rico (@tonidero)
* `Paywalls`: log error when failing to load images (#1321) via NachoSoto (@NachoSoto)
* Log errors when displaying default paywall (#1318) via Cesar de la Vega (@vegaro)
* Rename packages to packageIds in `PaywallData` (#1309) via Cesar de la Vega (@vegaro)
* Add support for multiple intro offers in `IntroEligibilityStateView` (#1319) via Cesar de la Vega (@vegaro)
* Fix material theme references to use Material3 versions (#1326) via Toni Rico (@tonidero)
* `Paywalls`: Add support to launch paywall as activity (#1317) via Toni Rico (@tonidero)
* Parse `{{ sub_offer_price_2 }}` and `{{ sub_offer_duration_2 }}` variables (#1313) via Cesar de la Vega (@vegaro)
* `Paywalls`: changed PaywallsTester app icon (#1323) via NachoSoto (@NachoSoto)
* `Paywalls`: fixed PaywallDialog.setRequiredEntitlementIdentifier (#1322) via NachoSoto (@NachoSoto)
* `Paywalls`: Markdown support (#1312) via NachoSoto (@NachoSoto)
* `PaywallsTester`: added template name to offerings list (#1316) via NachoSoto (@NachoSoto)
* `Paywalls`: Update paywall tester to be able to display paywall footer (#1315) via Toni Rico (@tonidero)
* `Paywalls`: Add PaywallFooter composable to present a minified paywall UI that allows for custom paywalls (#1314) via Toni Rico (@tonidero)
* `Paywalls`: use IntroEligibilityStateView (#1311) via NachoSoto (@NachoSoto)
* `PaywallData` validation tests (#1310) via Cesar de la Vega (@vegaro)
* `Paywalls`: implemented LoadingPaywallView with placeholder (#1284) via NachoSoto (@NachoSoto)
* `Paywalls`: created LoadingPaywallView (#1282) via NachoSoto (@NachoSoto)
* Fix template test data (#1308) via Cesar de la Vega (@vegaro)
</details>

### Other Changes
Add `purchases.setOnesignalUserID` (#1304) via Raquel Diez (@Raquel10-RevenueCat)