# Perf regression gate

This package is a Robolectric-based CI gate for the `getOfferings()` request path, driven through
a local `MockWebServer`. It needs no emulator and rides the normal `src/test` unit-test lane — see
"Where this runs in CI" below.

The remote-config/workflows path is now the **default**: `DangerousSettings.useWorkflows` and
`DangerousSettings.forWorkflows()` were removed, and the config layer is on for every
configuration except the `customEntitlementComputation` flavor
(`PurchasesFactory.kt`: `remoteConfigEnabled = !appConfig.customEntitlementComputation`). This
suite exists to catch a commit that makes that path heavier — either in network work or in
memory — for every default-flavor user, since there is no workflows-off arm left to opt out of it.

The `/config` fetch genuinely sits on the `getOfferings()` critical path: it is triggered lazily
from the offerings success path and awaited by the paywall-config readiness gate
(`workflowManager?.onPaywallConfigReady(onComplete = dispatchSuccess) ?: dispatchSuccess()`,
`OfferingsManager.kt:171` and `:296`).

## What it asserts

1. **Round-trip structure** (`GetOfferingsPerfTest.defaultPathMakesExactlyTheExpectedRoundTrips`).
   On the default path, a cold `getOfferings()` issues exactly one `/offerings`, one `/products`,
   and one `/config` round trip — three requests total, no more. If a future change makes the
   default path issue more requests than that, this test fails regardless of how fast or slow the
   machine running it is.

2. **Allocated bytes** (`GetOfferingsMemoryTest.coldCycleAllocationsStayUnderBudget`). One cold
   `configure()` + `getOfferings()` cycle, with allocations summed across every thread the SDK
   touches (background executors and coroutine dispatchers do most of the config/offerings work,
   so a single-thread measurement would miss most of it), gated against a committed budget
   constant. The budget is deliberately re-baselined when it legitimately changes, not silently
   raised — see the comment on `MAX_COLD_CYCLE_ALLOCATED_BYTES`.

3. **Resilience** (`GetOfferingsPerfTest.warmCycleStillReturnsOfferings`,
   `failingNonCriticalConfigSyncStillReturnsOfferings`). The warm (cached) path must still return
   offerings, and the `/config` sync is best-effort: if it fails
   (`Dispatcher.withPathFailure("/config")`), `getOfferings()` must still succeed and return
   offerings. This matters because config is now on the default path for essentially every
   configuration — a hard failure there can no longer be shrugged off as an opt-in feature's
   problem.

## Why round-trip counts and allocated bytes, not wall-clock time

Wall-clock thresholds (`assert elapsed < 500ms`) are machine-speed-dependent and flake on
shared/loaded CI runners — a slower runner or a noisy-neighbor build agent produces false
positives unrelated to the SDK. Round-trip counts are exact and deterministic regardless of
machine speed. Allocated bytes are counted directly via `ThreadMXBean`, not timed, so they're also
unaffected by machine speed or GC pauses. Elapsed time is still printed in both suites — useful
context for a human reading test output — but it is never asserted. Earlier iterations of this
gate injected artificial network latency (`NetworkProfile.BAD`/`FLAKY` with per-request delays)
and asserted on the resulting timings; that was removed because it was exactly this kind of
CI-flaky wall-clock assertion. `DispatcherDecorators.withPathFailure` keeps only the
failure-injection half, with no delay.

## Where this runs in CI

These are ordinary JVM unit tests under `src/test`; there is no emulator, real network, or
special CI job. They run automatically on every PR as part of the existing
`:purchases:testDefaultsDebugUnitTest` task, invoked by the `test_defaults_debug` job in the
`build-test-deploy` CircleCI workflow (`.circleci/config.yml`). There is no
`-PTEST_PACKAGE_FILTER`-style exclusion mechanism in this repo's CI/fastlane config that would
skip the `com.revenuecat.purchases.perf` package — it rides the full unit-test task like any other
test in `:purchases`.

## How the SDK is driven

`PerfHarness` (in `PerfHarness.kt`) points `Purchases.proxyURL` at a local `MockWebServer` seeded
by `PerfFixtures.dispatcher(...)`, which serves the recorded JSON under
`purchases/src/test/resources/perf-fixtures/`. The `/v1/config` response is served separately by
`PerfConfigFixture`: it's a valid RC Container Format v1 blob (workflows + ui_config topics, with
every referenced blob inlined), built via the same internal test helper
(`RCContainerTestData.buildContainer`) that backs `RCContainerTest`, so it never drifts from what
the real parser is tested against. The `offerings.json` fixture is a recorded ~23KB response with
realistic product/package shapes (including `cheapest_subs`), so the measured work — parsing,
caching, allocation — is representative of a real payload rather than a `{}` stub.
`DispatcherDecorators.withPathFailure(match, code)` wraps a dispatcher so a single matching
endpoint fails instead of injecting latency; it's used by the resilience test to fail `/config`
only. Because the SDK posts its final `getOfferings()` callback to the main `Looper`, and
Robolectric's looper is paused by default, `PerfHarness.runCycle` polls
`shadowOf(Looper.getMainLooper()).idle()` between short waits instead of a plain `latch.await()` —
a plain await deadlocks since the test itself runs on the main thread.

`GetOfferingsMemoryTest` additionally tracks allocated bytes via
`com.sun.management.ThreadMXBean#getThreadAllocatedBytes`, accessed by reflection (this module's
jvmTarget 1.8 hides `java.management` at compile time — see that class's KDoc), summed across all
threads (`ThreadMXBean#getAllThreadIds`) rather than just the calling thread. It warms up with one
discarded cold cycle first, so first-use classloading/static-init allocations aren't misattributed
to the measured samples, then takes the median of several cold-cycle samples for the gate and also
reports a warm-cycle number for context.

## Refreshing fixtures

See `purchases/src/test/resources/perf-fixtures/CAPTURE.md` for the procedure to re-record the
fixture JSON bodies (e.g. when the backend response contract changes).

## Relationship to the on-demand `getOfferings` benchmark (PR #3745)

PR #3745 added a separate, on-demand benchmark that drives a real backend from the perftester
sample app to measure absolute `getOfferings()` latency. That benchmark remains the tool for deep
absolute-latency investigation and is run manually, not on every PR. This package is complementary:
it is the automated CI regression gate — deterministic and machine-independent — that catches
structural regressions (extra round-trips, allocation blowups, broken resilience) on every PR,
while PR #3745's benchmark is reserved for deliberate, real-network latency investigations.
