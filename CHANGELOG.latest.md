## RevenueCat SDK
### 🐞 Bugfixes
* fix: store ETag cache payload verbatim to avoid OOM on large responses (#3774) via Cesar de la Vega (@vegaro)
* fix(remote-config): lazy-decode workflows cache to reduce memory (#3793) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Eagerly prewarm offering's workflow assets (#3775) via Cesar de la Vega (@vegaro)
* fix(remote-config): Avoid redundant offerings reload on disabled paywall re-presentation (#3802) via Toni Rico (@tonidero)
* test: Remove backend golden response verification (#3801) via Toni Rico (@tonidero)
* feat(remote-config): source API base host from remote-config sources (#3715) via Antonio Pallares (@ajpallares)
* Open the default paywall for offerings without a workflow (#3790) via Cesar de la Vega (@vegaro)
* refactor(remote-config): RCContainer improvements (#3791) via bisho (@bisho)
* Add Android workflow paywall Maestro flows (local only) (#3797) via Cesar de la Vega (@vegaro)
* Trust user CAs in debug builds of paywalls tester for proxying (#3795) via Cesar de la Vega (@vegaro)
* feat(remote-config): add internal usesRemoteConfigAPISources dangerous setting (#3792) via Antonio Pallares (@ajpallares)
