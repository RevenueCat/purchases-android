## RevenueCat SDK
### 🐞 Bugfixes
* Handle button sheet destination without an inline sheet (#3733) via Monika Mateska (@MonikaMateska)
* [EXTERNAL] fix(events): catch FileNotFoundException in openBufferedReader to prevent TOCTOU crash (#3704) contributed by @tsushanth (#3719) via Toni Rico (@tonidero)
* fix(paywalls): pre-download Paywalls V2 icons from the full base URL path (#3713) via Tarek M. Ben Lechhab (@bilqisium)

## RevenueCatUI SDK
### Paywalls_v2
#### ✨ New Features
* feat(paywalls): add state-driven paywalls data layer (PWENG-57) (#3656) via Álvaro Brey (@AlvaroBrey)

### 🔄 Other Changes
* Fall back to the offerings paywall when the workflow fetch fails (#3709) via Cesar de la Vega (@vegaro)
* Serve workflows from the config endpoint (#3716) via Cesar de la Vega (@vegaro)
* Add `WorkflowsConfigProvider` (#3723) via Cesar de la Vega (@vegaro)
* refactor(workflows): render workflows with ui_config from the config endpoint (#3721) via Cesar de la Vega (@vegaro)
* UI Config via config endpoint (#3711) via Cesar de la Vega (@vegaro)
* refactor(remote-config): Add `mergeItemsBlobData` to `RemoteConfigManager` (#3725) via Toni Rico (@tonidero)
* Rename workflow analytics events to singular (#3670) via Cesar de la Vega (@vegaro)
* Remove bc7 flavor and billingclient dimension (#3717) via Toni Rico (@tonidero)
* Add ForbiddenPublicEnum detekt rule (#3707) via Cesar de la Vega (@vegaro)
* fix(remote-config): Remote-config fixes/improvements (#3714) via Toni Rico (@tonidero)
* First pass at local encrypted storage (#3680) via Dave DeLong (@davedelong)
* refactor(remote-config): Disable in CEC mode + add observability logs (#3705) via Toni Rico (@tonidero)
* refactor(remote-config): add RemoteConfigManager read facade (topic/body) (#3703) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3706) via RevenueCat Git Bot (@RCGitBot)
* refactor(remote-config): Disable /v1/config for the session on a 4xx response (#3700) via Toni Rico (@tonidero)
* Enable Gradle build cache for local/worktree builds (#3523) via Cesar de la Vega (@vegaro)
* refactor(remote-config): Re-arm blob sources only when exhausted + in-memory blob-store index (#3698) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3701) via RevenueCat Git Bot (@RCGitBot)
* refactor(remote-config): Wire the blob fetcher into RemoteConfigManager (prefetch, re-arm, clear) (#3683) via Toni Rico (@tonidero)
* feat(remote-config): support fallback base URL for the config endpoint (#3697) via Antonio Pallares (@ajpallares)
