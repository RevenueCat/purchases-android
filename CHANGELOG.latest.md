## RevenueCat SDK
### ✨ New Features
* feat: Add Roku and PayPal store via Facundo Menzella (@facumenzella)
* feat(events): include app session ID in paywall and workflow events via Takeichi Kanzaki Cabrera (@tkanzakic)
* feat(ads): local promo entitlement expiration via Peter Porfy (@peterporfy)
* feat: add device identifier collection mode option via Álvaro Brey (@AlvaroBrey)
### 🐞 Bugfixes
* [EXTERNAL] Paywalls V2: don't inherit letter spacing from the MaterialTheme (#4499) via @unadamlar via Álvaro Brey (@AlvaroBrey)
* fix: enable automatic Play Billing service reconnection via Cesar de la Vega (@vegaro)
* fix(checkpoints): Treat checkpoint identifiers as case-insensitive via Toni Rico (@tonidero)
* fix(events): prevent event storage failures from crashing apps via Cesar de la Vega (@vegaro)
### 📦 Dependency Updates
* [RENOVATE] Update dependency fastlane to v2.242.0 via RevenueCat Git Bot (@RCGitBot)
* [RENOVATE] Update dependency revenuecat to v4.13.1 via RevenueCat Git Bot (@RCGitBot)
* [RENOVATE] Update circleci-orb via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### Paywallsv2
#### 🐞 Bugfixes
* fix(paywalls): keep low-res images visible when full-size images fail via Cesar de la Vega (@vegaro)
* fix(paywalls): preserve purchase attribution when the view model changes via Cesar de la Vega (@vegaro)
* fix(paywalls): use the selected tab’s sheet for package prices via Cesar de la Vega (@vegaro)
* fix(paywalls): show localized images instead of the default-locale image via Sufi Gaffar (@sufigaffar)
* fix(paywalls): format period counts with the paywall currency locale via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Bump Gradle to 9.6.1 and AGP to 9.5.2, cap Gradle below 9.7 via Toni Rico (@tonidero)
* Allow passing a version to the CircleCI bump action via Toni Rico (@tonidero)
* test(events): reduce memory used by file helper spy via Takeichi Kanzaki Cabrera (@tkanzakic)
* docs: rewrite the README via Aloysius Paredes (@aloysius-rc)
* test: fail billing tests on unverified diagnostics calls via Álvaro Brey (@AlvaroBrey)
* ci: Bump Liberica JDK to 21.0.13.1 and 17.0.21.1 via Álvaro Brey (@AlvaroBrey)
* ci: move Ruby gem updates to Renovate via Álvaro Brey (@AlvaroBrey)
* chore(deps): bump fastlane-plugin-revenuecat_internal to v0.3.1 via Antonio Pallares (@ajpallares)
* test: use the audience fixture clock for subscription expiry via Facundo Menzella (@facumenzella)
