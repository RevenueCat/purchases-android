## RevenueCat SDK
### 🐞 Bugfixes
* fix: exclude pending purchases from offline entitlements calculation (#3847) via Toni Rico (@tonidero)
### 📦 Dependency Updates
* [RENOVATE] Update dependency revenuecat to v4.5.1 (#3833) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### ✨ New Features
* feat: add onUrlOpened to PaywallListener (#3859) via Toni Rico (@tonidero)
* Add onWebCheckoutOpened to PaywallListener (#3809) via Álvaro Brey (@AlvaroBrey)
* feat(paywalls): Support Custom components (#3787) via Jacob Rakidzich (@JZDesign)
### 🐞 Bugfixes
* FIX: Web View Scroll Arbitration  (#3856) via Jacob Rakidzich (@JZDesign)
* fix(paywalls): render price for products with no billing period (#3850) via Cesar de la Vega (@vegaro)
* fix(paywalls): stop Fill content collapsing to zero under an unbounded ancestor (#3837) via Álvaro Brey (@AlvaroBrey)
### Paywallsv2
#### ✨ New Features
* Enable support for multipage paywalls (#3864) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* fix(remote-config): clear last attempt timestamp for cooldown after successful response (#3861) via Rick (@rickvdl)
* fix(maestro): tap the relabeled Continue buttons in the workflow flows (#3866) via Antonio Pallares (@ajpallares)
* Fix test value for Last-Refreshed-At header (#3862) via swehner (@swehner)
* Send workflow `trace_id` on paywall events (#3860) via Cesar de la Vega (@vegaro)
* Send workflow trace_id on transactions (#3858) via Cesar de la Vega (@vegaro)
* feat(remote-config): use the server clock for X-RC-Last-Refresh-Time (#3855) via Toni Rico (@tonidero)
* feat(remote-config): device connectivity pre-check for API source failover (#3840) via Toni Rico (@tonidero)
* feat(remote-config): send X-RC-Last-Refresh-Time on config requests (#3853) via Toni Rico (@tonidero)
* fix: render default paywall when workflow config is transiently unavailable (#3852) via Cesar de la Vega (@vegaro)
* test(remote-config): de-flake the inline workflows blob integration test (#3854) via Toni Rico (@tonidero)
* build(deps): bump fastlane-plugin-revenuecat_internal from `d392939` to `3421c88` (#3848) via dependabot[bot] (@dependabot[bot])
* refactor(remote-config): increase API failover restart interval to 2 hours (#3846) via Toni Rico (@tonidero)
* fix(paywalls): tighten web_view gesture-arbitration probe, track pointer id (#3842) via Álvaro Brey (@AlvaroBrey)
* refactor(remote-config): add API sources failover with health check endpoint support (#3812) via Toni Rico (@tonidero)
* refactor: move rules engine into core SDK as internal (#3838) via Antonio Pallares (@ajpallares)
* Update baseline profiles (#3835) via RevenueCat Git Bot (@RCGitBot)
