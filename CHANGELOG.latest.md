## RevenueCat SDK
### 🐞 Bugfixes
* fix: include paywall offering context in observer mode receipts via Rick (@rickvdl)

## RevenueCatUI SDK
### Paywallsv2
#### 🐞 Bugfixes
* fix(paywalls): show "---" lines, indented text and HTML blocks as typed instead of hiding them via Leti Esperón (@letiesperon)
* fix(paywalls): size fit-height sticky footers to their content via Cesar de la Vega (@vegaro)
* fix(paywalls): hidden paywall components appearing after a text or style override via Cesar de la Vega (@vegaro)
* fix(paywalls): honor numbering system from `preferredUILocaleOverride` when formatting prices via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* test: verify audiences from Khepri #27578 via revenuecat-ci-cd-bot-pr-writer[bot] (@revenuecat-ci-cd-bot-pr-writer[bot])
* ci: run Maestro on the ATD emulator image via Cesar de la Vega (@vegaro)
* build(deps): bump fastlane-plugin-revenuecat_internal from `a862068` to `2f42e82` via dependabot[bot] (@dependabot[bot])
* test: make three timing-sensitive unit tests deterministic via Álvaro Brey (@AlvaroBrey)
* test: make paywall event deduplication test deterministic via Cesar de la Vega (@vegaro)
* Verify audience conditions against Khepri test cases via Facundo Menzella (@facumenzella)
* test(iam): isolate the purchase callback in the bootstrap test via Cesar de la Vega (@vegaro)
* Recover from a System UI hang before debug Maestro flows via Cesar de la Vega (@vegaro)
