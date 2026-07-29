## RevenueCat SDK
### 🐞 Bugfixes
* fix: exclude pending purchases from offline entitlements calculation (#3847) via Toni Rico (@tonidero)
### 📦 Dependency Updates
* [RENOVATE] Update dependency revenuecat to v4.5.1 (#3833) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### ✨ New Features
* Add onWebCheckoutOpened to PaywallListener (#3809) via Álvaro Brey (@AlvaroBrey)
### 🐞 Bugfixes
* fix(paywalls): render price for products with no billing period (#3850) via Cesar de la Vega (@vegaro)
* fix(paywalls): stop Fill content collapsing to zero under an unbounded ancestor (#3837) via Álvaro Brey (@AlvaroBrey)
### Paywalls_v2
#### ✨ New Features
* feat(paywalls): enable web_view component (8) (#3787) via Jacob Rakidzich (@JZDesign)

### 🔄 Other Changes
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
