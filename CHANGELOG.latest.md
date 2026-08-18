## RevenueCat SDK
### 🐞 Bugfixes
* fix: don't share a MessageDigest across coroutines in FontLoader and DefaultFileCache (#3930) via Tarek M. Ben Lechhab (@bilqisium)
* Fix misleading ui_config warnings for projects without paywalls (#3926) via Rick (@rickvdl)
* fix: Declare kotlinx-coroutines-android as a dependency (#3916) via Álvaro Brey (@AlvaroBrey)
* fix(paywalls): prewarm the current offering's workflow assets on load (#3910) via Álvaro Brey (@AlvaroBrey)

## RevenueCatUI SDK
### Paywallsv2
#### 🐞 Bugfixes
* Default missing text localizations to empty instead of fallback paywall (#3903) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* ci: install mise Ruby 3.3.0 so CI honors Gemfile.lock (#3952) via Álvaro Brey (@AlvaroBrey)
* refactor(checkpoints): Local rule evaluation foundation (#3942) via Toni Rico (@tonidero)
* refactor(paywalls): move image warming to `purchases-ui` and use a ServiceLoader to reach it (#3913) via Álvaro Brey (@AlvaroBrey)
* build: fix api-tester customEntitlementComputation variant resolution after Kover bump (#3945) via Álvaro Brey (@AlvaroBrey)
* feat(checkpoints): expose CheckpointParams customProperties as paywall custom variables (#3941) via Toni Rico (@tonidero)
* Ingest audiences config topic (#3936) via Cesar de la Vega (@vegaro)
* build: update Kover to 0.9.9 (#3932) via Álvaro Brey (@AlvaroBrey)
* build: update Metalava to 0.5.0 (#3935) via Álvaro Brey (@AlvaroBrey)
* refactor(checkpoints): Cleanup to offeringIdByWorkflowId map + custom checkpoint screen in checkpoint tester (#3919) via Toni Rico (@tonidero)
* refactor(paywalls): collect web_view assets in the existing predownload walk (#3902) via Álvaro Brey (@AlvaroBrey)
* Update baseline profiles (#3927) via RevenueCat Git Bot (@RCGitBot)
* Fix flaky cached offerings integration test and cover the fully-offline case (#3920) via Álvaro Brey (@AlvaroBrey)
* chore: stop persisting ProductEntitlementMapping.originalSource to disk (#3917) via Álvaro Brey (@AlvaroBrey)
* refactor(checkpoints): Resolve a checkpoint's workflow from its rules (#3914) via Toni Rico (@tonidero)
* Ingest checkpoint rules remote config (#3907) via Cesar de la Vega (@vegaro)
* refactor(checkpoints): Implement checkpoint use cases in `CheckpointTester` (#3900) via Toni Rico (@tonidero)
* refactor(checkpoints): Move the checkpoints API to the RevenueCatUI module (#3909) via Toni Rico (@tonidero)
* build(deps): bump fastlane-plugin-revenuecat_internal from `b4e1e7f` to `7fbbe66` (#3908) via dependabot[bot] (@dependabot[bot])
* refactor(checkpoints): Create empty `CheckpointTester` project (#3898) via Toni Rico (@tonidero)
* refactor(checkpoints): Checkpoints wiring and sample usage in paywall tester - SDK-4423 (#3863) via Toni Rico (@tonidero)
* refactor(checkpoints): Checkpoints workflow presenter UI - SDK-4423 (#3889) via Toni Rico (@tonidero)
