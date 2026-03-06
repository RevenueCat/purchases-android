## RevenueCat SDK
### ✨ New Features
* [CIA-5346] Appstack integration to android sdk (#3150) via Damian Rubio (@DamianRubio)
* Create `PaywallPurchaseContext` to `PurchaseLogic` for product changes and offers (#3129) via Cesar de la Vega (@vegaro)
* [EXTERNAL] feat: introduce awaitCanMakePayments (#3136) contributed by @TheRogue76 (#3141) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Fix missing uiPreviewMode mock in integration test (#3164) via Rick (@rickvdl)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix: Low res video only on first paywall view (#3120) via Jacob Rakidzich (@JZDesign)
### Customer Center
#### 🐞 Bugfixes
* Fix cancellation URI in Customer Center (#3151) via Cesar de la Vega (@vegaro)
### Paywallv2
#### 🐞 Bugfixes
* Fix PaywallResult.Cancelled for successful MY_APP purchases (#3138) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Add internal trackCustomPaywallImpression method (#3167) via Rick (@rickvdl)
* Bump fastlane-plugin-revenuecat_internal from `8cd957f` to `f5c099b` (#3176) via dependabot[bot] (@dependabot[bot])
* Update sdks-common-config orb to 3.13.0 (#3173) via Antonio Pallares (@ajpallares)
* Add FLUSH_COMPLETED and FLUSH_SKIPPED_NO_EVENTS debug events (#3166) via Toni Rico (@tonidero)
* Add `paywall_tester_track` parameter to `manual-paywall-tester-release` (#3169) via Cesar de la Vega (@vegaro)
* Return mock CustomerInfo in preview mode (#3161) via Monika Mateska (@MonikaMateska)
* Disable attribution, diagnostics, and caching in preview mode (#3160) via Monika Mateska (@MonikaMateska)
* Add X-UI-Preview-Mode header to HTTP requests in preview mode (#3159) via Monika Mateska (@MonikaMateska)
* Paywall Tester - Helper to clear paywall file cache (#3154) via Jacob Rakidzich (@JZDesign)
* Use fixed user ID and block login/logout in preview mode (#3158) via Monika Mateska (@MonikaMateska)
* Bump fastlane-plugin-revenuecat_internal from `afc9219` to `ea6276c` (#3122) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.232.1 to 2.232.2 (#3155) via dependabot[bot] (@dependabot[bot])
* Add uiPreviewMode flag to DangerousSettings (#3144) via Monika Mateska (@MonikaMateska)
