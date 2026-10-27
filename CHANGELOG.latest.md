## RevenueCat SDK
### ✨ New Features
* Parse paywall gating in checkpoint workflows (#4452) via Cesar de la Vega (@vegaro)
### 🐞 Bugfixes
* fix(checkpoints): track started Activities from process start (#4518) via Cesar de la Vega (@vegaro)
* fix: keep the cached offerings response out of SharedPreferences to reduce memory usage (#4455) via Álvaro Brey (@AlvaroBrey)
* Fix dropped Billing callbacks after disconnection (#4511) via Cesar de la Vega (@vegaro)
* [EXTERNAL] Fix requests hanging indefinitely when the server stops responding (#3925) via @aelaguiz (#4513) via Álvaro Brey (@AlvaroBrey)
* [EXTERNAL] Fix non-subscription transactions reporting "null" display name and store transaction ID (#4485) via @RaphaelFakhri (#4500) via Álvaro Brey (@AlvaroBrey)
### 📦 Dependency Updates
* [RENOVATE] Update cimg/android Docker tag to v2026.10.1 (#4514) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### Paywallsv2
#### 🐞 Bugfixes
* fix(paywalls): PWENG-305 preserve fixed icon sizes with overflowing margins (#4427) via Azat Valiev (@azvaliev)

### 🔄 Other Changes
* Migrate the cached offerings response out of SharedPreferences instead of dropping it (#4488) via Álvaro Brey (@AlvaroBrey)
* Rename ETagPayloadStore to PayloadFileStore (#4487) via Álvaro Brey (@AlvaroBrey)
* Add Maestro SDK update tests verifying persisted state (#4381) via Antonio Pallares (@ajpallares)
* Let the tester apps simulate API source outages (#4521) via Toni Rico (@tonidero)
* Remove the usesRemoteConfigAPISources dangerous setting and enable API sources for everyone (#4520) via Toni Rico (@tonidero)
* Add API source integration tests with a normal Purchases instance (#4508) via Antonio Pallares (@ajpallares)
* Let ForceServerErrorStrategy fail API source health checks (#4507) via Antonio Pallares (@ajpallares)
* Exclude non-shipping modules from Danger's production lines check (#4522) via Antonio Pallares (@ajpallares)
* Add Internal APIs to spend in-app currencies (#4495) via Dave DeLong (@davedelong)
* other(e2etests): add an app presenter and an attribute to the checkpoint screen (#4490) via Facundo Menzella (@facumenzella)
* Tests only: End-to-end IAM test suite (#4479) via Dave DeLong (@davedelong)
