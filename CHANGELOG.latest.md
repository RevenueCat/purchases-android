## RevenueCat SDK
### ✨ New Features
* feat(ads): fire reward-verification tracking events from the poll (#3884) via Peter Porfy (@peterporfy)
### 🐞 Bugfixes
* fix(paywalls): don't leave a hidden package selected by default (#3915) via Facundo Menzella (@facumenzella)
### 📦 Dependency Updates
* [RENOVATE] Update dependency revenuecat to v4.6.0 (#3973) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### ✨ New Features
* Support headless offering checkpoints (#3951) via Cesar de la Vega (@vegaro)
### 🐞 Bugfixes
* fix(paywalls): Expose compose-foundation-layout so PaywallFooter is callable without extra setup (#3961) via Álvaro Brey (@AlvaroBrey)
* Fix multi-page paywall state after dismissal (#3944) via Cesar de la Vega (@vegaro)
* fix(paywalls): Prevent an invisible leftover header from blocking taps on workflow paywalls (#3947) via Álvaro Brey (@AlvaroBrey)
### Paywallsv2
#### ✨ New Features
* feat(paywalls): Preboot the WebView engine when a paywall has a web_view component (#3904) via Álvaro Brey (@AlvaroBrey)

### 🔄 Other Changes
* refactor(ads): expose Outcome from the reward-verification poller (#3883) via Peter Porfy (@peterporfy)
* ci(danger): fail PRs that grow a suppression baseline (#3982) via Álvaro Brey (@AlvaroBrey)
* Report public API changes on PRs and in the SDK API feed (#3976) via Álvaro Brey (@AlvaroBrey)
* feat(ads): expose AdTracker API for reward-verification events (#3892) via Peter Porfy (@peterporfy)
* feat(ads): wire-encode reward-verification events (#3891) via Peter Porfy (@peterporfy)
* feat(ads): add reward-verification event data types (#3890) via Peter Porfy (@peterporfy)
* Update baseline profiles (#3974) via RevenueCat Git Bot (@RCGitBot)
* feat(checkpoints): Add StoreDimensionProvider with store.country (#3969) via Toni Rico (@tonidero)
* Deserialize audience rules for rules engine evaluation (#3970) via Cesar de la Vega (@vegaro)
* refactor(checkpoints): Reuse CustomVariableValue for checkpoint custom variables (#3960) via Toni Rico (@tonidero)
* feat(checkpoints): Evaluate checkpoint custom variables as custom.* dimensions (#3949) via Toni Rico (@tonidero)
* build: apply the AGP 9 config migrations that already work on AGP 8 (#3963) via Álvaro Brey (@AlvaroBrey)
* build: move baseline profiles to the plugin's default source dir (#3931) via Álvaro Brey (@AlvaroBrey)
* refactor(checkpoints): Add DeviceDimensionProvider (#3943) via Toni Rico (@tonidero)
