## RevenueCat SDK
### ✨ New Features
* Log reward verification failure reason (#3535) via Pol Miro (@polmiro)
* Wire workflows fetching and add cache persistence (#3508) via Cesar de la Vega (@vegaro)
### 🐞 Bugfixes
* fix(test-store): don’t mention Play Store in error logs when the Test Store is used (#3538) via Rick (@rickvdl)
### Galaxy Store
#### ✨ New Features
* [Galaxy]: Add Samsung IAP billing permission to Galaxy manifest (#3539) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix paywall crash when a downloaded font file fails to load (#3568) via Toni Rico (@tonidero)
* Look for embedded paywall fonts in public/assets (#3571) via Toni Rico (@tonidero)
### Paywalls_v2
#### ✨ New Features
* Fade the multipage paywall header during page transitions (#3525) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Remove workflow details fallback on 4xx errors (#3567) via Cesar de la Vega (@vegaro)
* Align paywall-rendering-validation density with iOS (#3577) via JayShortway (@JayShortway)
* Exclude rules-engine-internal from public sealed/data class detekt rules (#3581) via Antonio Pallares (@ajpallares)
* Add Danger check to discourage large PRs (#3543) via Toni Rico (@tonidero)
* Use the `log` operator to strengthen rules-engine fixtures (#3564) via Antonio Pallares (@ajpallares)
* build(deps): bump fastlane from 2.235.0 to 2.236.0 (#3570) via dependabot[bot] (@dependabot[bot])
* Add JSON Logic `log` operator (#3561) via Antonio Pallares (@ajpallares)
* Add JSON Logic iteration operators (`none`, `map`, `filter`, `reduce`) (#3553) via Antonio Pallares (@ajpallares)
* AdMob reward verification doc & test fixes (#3565) via Pol Miro (@polmiro)
* Clean up changelogs from workflows PRs (#3566) via Cesar de la Vega (@vegaro)
* AdMob reward verification (#3465) via Pol Miro (@polmiro)
* Remove workflows fallback on 4xx errors (#3562) via Cesar de la Vega (@vegaro)
* Wire workflow step lifecycle events in PaywallViewModel (#3487) via Cesar de la Vega (@vegaro)
* Replace workflows `forceStale` flag with a `forceRefresh` param (#3541) via Cesar de la Vega (@vegaro)
* Stale-while-revalidate for the workflow detail fetch (#3540) via Cesar de la Vega (@vegaro)
* Represent ±Infinity in test fixtures via test-only variables (#3556) via Antonio Pallares (@ajpallares)
* Fix `X ms is denormalized` crash in AmazonBillingTest (#3558) via Cesar de la Vega (@vegaro)
* Add JSON Logic min and max operators (#3552) via Antonio Pallares (@ajpallares)
* Remove test-galaxy CI job (#3549) via Will Taylor (@fire-at-will)
* Persist workflow detail envelopes for recovery when backend is down (#3537) via Cesar de la Vega (@vegaro)
* Add JSON Logic iteration operators (`some`, `all`) (#3551) via Antonio Pallares (@ajpallares)
* Migrate base RulesEngineInternal operator unit tests to JSON predicate fixtures (#3545) via Antonio Pallares (@ajpallares)
* Remove Samsung IAP SDK version warning log (#3546) via Will Taylor (@fire-at-will)
* Make PaywallViewModel dispatcher injectable to fix flaky tests (#3544) via Cesar de la Vega (@vegaro)
