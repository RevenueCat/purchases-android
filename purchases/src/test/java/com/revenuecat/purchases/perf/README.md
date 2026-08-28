# Config-path structural regression tests

Robolectric tests that drive a real `Purchases.configure()` + `getOfferings()` through a local
`MockWebServer`, asserting the **shape** of the request path rather than how long it takes. No
emulator, no real network — they ride the normal `src/test` unit-test lane (see "Where this runs in
CI").

The remote-config/workflows path is now the **default**: `DangerousSettings.useWorkflows` and
`DangerousSettings.forWorkflows()` were removed, and the config layer is on for every configuration
except the `customEntitlementComputation` flavor (`PurchasesFactory.kt`:
`remoteConfigEnabled = !appConfig.customEntitlementComputation`). There is no workflows-off arm left
to opt out of it, so a change that adds work to that path affects essentially every user.

The `/config` fetch genuinely sits on the `getOfferings()` critical path: it is triggered lazily
from the offerings success path and awaited by the paywall-config readiness gate
(`workflowManager?.onPaywallConfigReady(onComplete = dispatchSuccess) ?: dispatchSuccess()`,
`OfferingsManager.kt:171` and `:296`). A slower config fetch therefore slows a cold `getOfferings()`
close to 1:1 — which is why the round-trip count on this path is worth pinning down.

## What it asserts

1. **Round-trip structure** (`GetOfferingsPerfTest.defaultPathMakesExactlyTheExpectedRoundTrips`).
   A cold `getOfferings()` issues exactly one `/offerings`, one `/products` and one `/config` round
   trip — three requests, no more. This catches the regressions that actually happen here: a
   reintroduced blob/CDN fetch, lost request coalescing, a retry storm, or config becoming an extra
   serial hop. It fails deterministically regardless of how fast or loaded the machine is.

   When this test fails on an intentional change, updating the expected counts is a one-line edit —
   but do it deliberately. The point of the assertion is the moment of "did we mean to add a round
   trip to cold start?"

2. **Resilience** (`GetOfferingsPerfTest.warmCycleStillReturnsOfferings`,
   `failingNonCriticalConfigSyncStillReturnsOfferings`). The warm (cached) path must still return
   offerings, and the `/config` sync must stay best-effort: when it fails
   (`DispatcherDecorators.withPathFailure("/config")`), `getOfferings()` must still succeed. Now that
   config is on the default path, a hard failure there can no longer be shrugged off as an opt-in
   feature's problem.

## What these tests deliberately do NOT assert

**Wall-clock time.** Thresholds like `assert elapsed < 500ms` are machine-speed-dependent and flake
on shared or loaded CI runners. Elapsed time is printed for a human reading the output, never
asserted. An earlier iteration injected artificial per-request latency and asserted on the resulting
timings; that was removed for exactly this reason. `withPathFailure` keeps only the
failure-injection half, with no delay.

**Allocated bytes / memory.** An allocation gate over this end-to-end path was prototyped and
removed. It measured ~2.87 MB per cold cycle, but the top allocators were Robolectric's main thread
(~809 KB), MockWebServer's own I/O thread (~671 KB) and a short-lived pool thread (~894 KB), while
the SDK's own coroutine workers accounted for only 14–61 KB each. Two thirds of the number was test
harness, so any budget loose enough to be stable was also too loose to catch a real config-path
regression, and it would have needed re-baselining on every Kotlin/Robolectric/dependency bump.

If config-path memory becomes a concern, use a precisely scoped instrument instead:
`common/networking/ETagManagerMemoryTest.kt` is the working pattern (one class, one payload,
`ThreadMXBean` allocation counting, no harness noise), and Emerge — already wired into this repo's
CI for size analysis — measures real on-device behavior rather than JVM allocations under
Robolectric.

## Where this runs in CI

Ordinary JVM unit tests under `src/test`; no emulator, real network, or special CI job. They run on
every PR as part of the existing `:purchases:testDefaultsDebugUnitTest` task, invoked by the
`test_defaults_debug` job in the `build-test-deploy` CircleCI workflow (`.circleci/config.yml`).
Nothing in the CI/fastlane config excludes the `com.revenuecat.purchases.perf` package.

## How the SDK is driven

`PerfHarness` points `Purchases.proxyURL` at a local `MockWebServer` seeded by
`PerfFixtures.dispatcher(...)`, which serves the recorded JSON under
`purchases/src/test/resources/perf-fixtures/`. `proxyURL` covers both the main API and `/v1/config`
(they share `AppConfig.baseURL`), so the whole critical path is served from localhost.

Three details that are easy to get wrong when reusing this harness:

- **The `/v1/config` body must be a valid RC Container Format v1 blob**, served as bytes by
  `PerfConfigFixture` (workflows + ui_config topics, every referenced blob inlined), built via the
  same internal helper that backs `RCContainerTest` (`RCContainerTestData.buildContainer`). A `{}`
  stub is *not* valid and only exercises the parse-failure/retry path — it never reaches parse →
  persist → readiness.
- **Robolectric's main `Looper` is paused**, and the SDK posts its final `getOfferings()` callback
  there. `PerfHarness.runCycle` polls `shadowOf(Looper.getMainLooper()).idle()` between short waits;
  a plain `latch.await()` deadlocks, because the test itself runs on the main thread.
- **Robolectric does not auto-grant manifest permissions**, so `runCycle` grants `INTERNET`
  explicitly or `configure()` throws.

The `offerings.json` fixture is a recorded ~23 KB response with realistic product/package shapes
(including `cheapest_subs`), so the parsing and caching work is representative rather than a stub.

## Refreshing fixtures

See `purchases/src/test/resources/perf-fixtures/CAPTURE.md`.

## Relationship to the on-demand `getOfferings` benchmark (PR #3745)

PR #3745 added a separate, on-demand benchmark that drives a real backend from the perftester sample
app to measure absolute `getOfferings()` latency. That remains the tool for real-latency
investigation and is run manually. This package is complementary and narrower: it pins the request
*structure* of the config path on every PR, and makes no claim about absolute performance.
