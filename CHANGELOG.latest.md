## RevenueCat SDK
### 🐞 Bugfixes
* fix(customer-center): prefer active entitlement when decorating subsc… (#3313) via Guilherme (@GuilhermeMota93)
* fix: migrate `await*` coroutines to `suspendCancellableCoroutine` to prevent `IllegalStateException` when callbacks fire more than once. **Behavior change:** `CancellationException` now propagates from public `await*` functions — callers wrapping these in a broad `catch (e: Exception)` should re-throw `CancellationException` to preserve structured concurrency.
* fix: move Google BillingClient connection off the main thread (#3369) via Toni Rico (@tonidero)
* [EXTERNAL] fix(google): guard showInAppMessages against BillingClient runtime crashes (#3367) by @matteinn (#3368) via Monika Mateska (@MonikaMateska)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Add Workflows network layer (#3300) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Fix `revenuecat.useWorkflowsEndpoint` compiler flag (#3374) via Cesar de la Vega (@vegaro)
* Create paywall from workflow response. Add `USE_WORKFLOWS_ENDPOINT` BuildConfig (#3350) via Cesar de la Vega (@vegaro)
* Refactor: Remove unnecessary lint suppressions (#3373) via cursor[bot] (@cursor[bot])
* Bump fastlane-plugin-revenuecat_internal from `a1eed48` to `b822f01` (#3371) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.232.2 to 2.233.0 (#3370) via dependabot[bot] (@dependabot[bot])
* Attempt to fix `AssertionError` "ms is denormalized" in `QueryPurchasesUseCaseTest` (#3361) via Cesar de la Vega (@vegaro)
* Update baseline profiles (#3296) via Jaewoong Eum (@skydoves)
* fix: reduce precision for flaky HeaderDirectHeroImage snapshot (#3362) via Cesar de la Vega (@vegaro)
* Fix test failures reported twice (#3360) via Cesar de la Vega (@vegaro)
* refactor: extract `updateStateFromOffering` in `PaywallViewModel` (#3359) via Cesar de la Vega (@vegaro)
* [Fix] Include parent tabs component_name in tab-control switch interaction events (#3358) via Monika Mateska (@MonikaMateska)
* Refactor: Remove unnecessary lint suppressions (#3348) via cursor[bot] (@cursor[bot])
* fix: always upload CI test results even when tests fail (#3357) via Cesar de la Vega (@vegaro)
* refactor: extract `RevenueCatDialogScaffold` (#3355) via Cesar de la Vega (@vegaro)
* Fix Slack notifications for nightly integration tests (#3354) via Toni Rico (@tonidero)
* UI events for paywall component interactions (#3287) via Monika Mateska (@MonikaMateska)
* Bump fastlane-plugin-revenuecat_internal from `20911d1` to `a1eed48` (#3351) via dependabot[bot] (@dependabot[bot])
