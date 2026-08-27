## RevenueCat SDK
### ✨ New Features
* feat(singular): add $singularDeviceId subscriber attribute for Singular V2 (#4072) via Guillem Corominas (@guillemcorominas)
* Add `awaitShowManageSubscriptions` (#4040) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### Paywallsv2
#### ✨ New Features
* feat(paywalls): warm the web_views of the current offering and every placement's (#3938) via Álvaro Brey (@AlvaroBrey)
#### 🐞 Bugfixes
* Fix(Paywalls): Avoid POSIX localizations (#4048) via Jacob Rakidzich (@JZDesign)
* Read `zero_decimal_place_countries` from workflow screens (#4061) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Rework CheckpointParams into a Builder with a Kotlin DSL (#4085) via Toni Rico (@tonidero)
* Add tolerance to flaky ImageComponentView_Preview_Margin_Padding Emerge snapshot (#4100) via Álvaro Brey (@AlvaroBrey)
* chore: don't run danger on main (#4079) via Cesar de la Vega (@vegaro)
* chore(ads): remove experimental flag from ads apis (#4068) via Peter Porfy (@peterporfy)
* Add rc.entries and rc.fromEntries custom operators (#4062) via Antonio Pallares (@ajpallares)
* Announce public API changes from main only (#4083) via Facundo Menzella (@facumenzella)
* Add rc.semverCompare custom operator (#4077) via Antonio Pallares (@ajpallares)
* Add rc.split custom operator (#4066) via Antonio Pallares (@ajpallares)
* Sort placements when prewarming paywall web views (#4074) via Álvaro Brey (@AlvaroBrey)
* Refresh the doc comments that describe an absent variable as null (#4076) via Antonio Pallares (@ajpallares)
* Raise an error for unresolved variables instead of degrading to null (#4043) via Antonio Pallares (@ajpallares)
* Set or unset subscriber attributes from the checkpoint tester (#4046) via Toni Rico (@tonidero)
* perf(paywalls): warm a paywall's first page before its later ones (#3966) via Álvaro Brey (@AlvaroBrey)
* Add rc.length operator for strings and arrays (#4050) via Antonio Pallares (@ajpallares)
* Add rc.lower and rc.upper custom operators (#4051) via Antonio Pallares (@ajpallares)
* Update baseline profiles (#4067) via RevenueCat Git Bot (@RCGitBot)
* Update sdks-common-config orb to v4.6.1 (#4063) via Antonio Pallares (@ajpallares)
* Add rc.rootVar operator for root scope access (#4049) via Antonio Pallares (@ajpallares)
* Add Scope plumbing and custom operator extension point (#4047) via Antonio Pallares (@ajpallares)
* refactor(paywalls): share one component-config lookup across asset pre-download (#4018) via Álvaro Brey (@AlvaroBrey)
* paywalls: Add webview cache warming engine (#3897) via Álvaro Brey (@AlvaroBrey)
* Validate checkpoint identifiers (#4024) via Cesar de la Vega (@vegaro)
