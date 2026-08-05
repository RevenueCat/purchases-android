## RevenueCat SDK
### 🐞 Bugfixes
* fix: avoid OutOfMemoryError when caching large offerings responses (#3872) via Álvaro Brey (@AlvaroBrey)
* fix: reduce memory reading cached responses and recover from corrupted cache entries (#3873) via Álvaro Brey (@AlvaroBrey)
* fix: make interface default methods usable from Java in all published modules (#3874) via Álvaro Brey (@AlvaroBrey)

## RevenueCatUI SDK
### Paywallsv2
#### 🐞 Bugfixes
* fix(paywalls): apply tab state rules on workflow-backed paywalls (#3875) via Álvaro Brey (@AlvaroBrey)

### 🔄 Other Changes
* Add missing Maestro tests for workflow paywalls (#3827) via Cesar de la Vega (@vegaro)
* refactor(checkpoints): Checkpoints core engine - SDK-4423 (#3888) via Toni Rico (@tonidero)
* Ingest empty checkpoint remote config (#3895) via Cesar de la Vega (@vegaro)
* Checkpoints public API v0 (Internally annotated APIs only) - SDK-4418 (#3887) via Toni Rico (@tonidero)
* test: cover workflow config failures before launch and after restart (#3843) via Cesar de la Vega (@vegaro)
* build(deps): bump fastlane-plugin-revenuecat_internal from `dd577ee` to `b4e1e7f` (#3893) via dependabot[bot] (@dependabot[bot])
* Add a Maestro flow for the config kill switch tripping mid-session (#3878) via Rick (@rickvdl)
* build(deps): bump danger from 9.6.0 to 9.6.1 (#3880) via dependabot[bot] (@dependabot[bot])
* build(deps): bump fastlane-plugin-revenuecat_internal from `3421c88` to `dd577ee` (#3879) via dependabot[bot] (@dependabot[bot])
* Update baseline profiles (#3877) via RevenueCat Git Bot (@RCGitBot)
* Bound blob downloads with per-source timeouts (#3749) via Antonio Pallares (@ajpallares)
* Re-tier HTTP request timeouts around per-host memory (#3746) via Antonio Pallares (@ajpallares)
* fix(remote-config): improve logging when remote config is disabled through to killswitch (#3870) via Rick (@rickvdl)
