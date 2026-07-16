## RevenueCat SDK
### 🐞 Bugfixes
* Improve memory usage on ETags by making `HTTPResult` body lazy (#3764) via Cesar de la Vega (@vegaro)
* Coalesce `onPaywallConfigReady` so `getOfferings` readiness runs once (#3747) via Cesar de la Vega (@vegaro)
* Fix ui_config font/color decoding bug (#3742) via Cesar de la Vega (@vegaro)
### 📦 Dependency Updates
* [RENOVATE] Update dependency revenuecat to v4.4.0 (#3753) via RevenueCat Git Bot (@RCGitBot)
### Remote-config
#### 🐞 Bugfixes
* fix(remote-config): use a dedicated fallback endpoint (#3750) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Reset bottom sheet content identity when switching sheets (#3767) via Cesar de la Vega (@vegaro)
### Paywalls_v2
#### ✨ New Features
* [PW-870] Support transparent overlapping sticky footers (#3720) via Toni Rico (@tonidero)
#### 🐞 Bugfixes
* fix(paywalls): don't let image backgrounds affect layout size (#3730) via Toni Rico (@tonidero)

### 🔄 Other Changes
* feat(remote-config): force app_start on the first config request (#3773) via Antonio Pallares (@ajpallares)
* ci: pin automatic-bump job to Ruby 3.2.0 (#3789) via Antonio Pallares (@ajpallares)
* fix(remote-config): skip decoding paywall components under workflows to save memory (#3772) via Toni Rico (@tonidero)
* fix(remote-config): cache remote-config paywall data in memory to avoid loading flash (#3759) via Toni Rico (@tonidero)
* fix(remote-config): verify remote config signature over the decoded config element (#3788) via Toni Rico (@tonidero)
* feat(remote-config): send fetch_context on config endpoint requests (#3768) via Antonio Pallares (@ajpallares)
* Make HTTPResult visibility back to internal (#3770) via Toni Rico (@tonidero)
* fix: avoid HTTPResult copy re-parsing payload in ETag cache (#3765) via Toni Rico (@tonidero)
* fix(remote-config): throttle failed refresh attempts (#3756) via Rick (@rickvdl)
* chore(ci): api-check: show diff when api-check fails, only check api files (#3761) via Álvaro Brey (@AlvaroBrey)
* Add Ruby to mise for local dev (#3758) via Álvaro Brey (@AlvaroBrey)
* Fail fast for missing workflow mappings in paywalls (#3760) via Cesar de la Vega (@vegaro)
* Upgrade Dokka to 2.2.0 and migrate to Gradle plugin v2 DSL (#3731) via Álvaro Brey (@AlvaroBrey)
* Use mise for Java in CI (#3727) via Álvaro Brey (@AlvaroBrey)
* refactor(remote-config): Clarify prefetch remote-config blobs in the server-provided order (#3755) via Toni Rico (@tonidero)
* perf(remote-config): run /v1/config on a dedicated dispatcher to overlap getOfferings (#3751) via Toni Rico (@tonidero)
* build(deps): bump fastlane from 2.236.1 to 2.237.0 (#3744) via dependabot[bot] (@dependabot[bot])
* build(deps): bump excon from 0.112.0 to 1.5.0 (#3752) via dependabot[bot] (@dependabot[bot])
* Document why host failover ignores device-connectivity errors (#3740) via Antonio Pallares (@ajpallares)
* Update baseline profiles (#3754) via RevenueCat Git Bot (@RCGitBot)
* fix(remote-config): require all UI config fields except custom_variables (#3748) via Antonio Pallares (@ajpallares)
* fix(remote-config): require UI config for workflows (#3739) via Rick (@rickvdl)
* Drop `ENABLE_REMOTE_CONFIG`, and gate the config layer on useWorkflows (#3728) via Cesar de la Vega (@vegaro)
* Remove dead workflows code (#3736) via Cesar de la Vega (@vegaro)
* refactor(remote-config): Add `awaitTopicAndPrefetchBlobsReady` to `RemoteConfigManager` (#3732) via Toni Rico (@tonidero)
* fix(remote-config): don't sync a cold read for the previous user on identity change (#3722) via Toni Rico (@tonidero)
* Experimental IAM configuration option (#3726) via Dave DeLong (@davedelong)
* ci: Bump sdks-common-config orb version (#3734) via Toni Rico (@tonidero)
