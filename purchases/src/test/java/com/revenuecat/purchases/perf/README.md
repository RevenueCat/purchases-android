# Perf regression gate

This package is a Robolectric-based CI gate for the `getOfferings()` request path, driven through
a local `MockWebServer` under injected network conditions. It needs no emulator and rides the
normal `src/test` unit-test lane — see "Where this runs in CI" below.

The remote-config/workflows path is now the **default**: `DangerousSettings.useWorkflows` and
`DangerousSettings.forWorkflows()` were removed, and the config layer is on for every
configuration except the `customEntitlementComputation` flavor
(`PurchasesFactory.kt`: `remoteConfigEnabled = !appConfig.customEntitlementComputation`). There is
therefore no workflows-off arm left to compare against. The gate instead **self-compares**: the
same default configuration, run twice in the same test, under two different network conditions.

The `/config` fetch genuinely sits on the `getOfferings()` critical path: it is triggered lazily
from the offerings success path and awaited by the paywall-config readiness gate
(`workflowManager?.onPaywallConfigReady(onComplete = dispatchSuccess) ?: dispatchSuccess()`,
`OfferingsManager.kt:171` and `:296`), so a slower `/config` response slows cold `getOfferings()`
roughly 1:1. Tier 2 below measures exactly this.

## What it asserts

The suite (`GetOfferingsPerfTest`) gates on three tiers, in order of how load-bearing they are:

1. **Round-trip structure (primary).** On the default path, a cold `getOfferings()` issues exactly
   one `/offerings`, one `/products`, and one `/config` round trip — three requests total, no more.
   This is the main regression gate: if a future change makes the default path issue more requests
   than that, this test fails regardless of how fast or slow the machine running it is.

2. **Config-cost self-comparison (secondary).** Same configuration, same test run: one cycle runs
   against a dispatcher with no added delay, another against the same dispatcher with `/config`
   delayed by 1500ms (`Dispatcher.withPathDelay("/config", ...)`). The test asserts the delta
   between the two medians stays under a 2250ms budget (1.5x the injected delay) — i.e. the config
   fetch costs at most ~one serial round trip. A second serial config hop, a retry storm, or any
   other structural regression would push the delta toward ~2x the injected delay and trip this.
   Machine speed cancels out because both samples run on the same hardware in the same test.

3. **Behavior under adversity.** The warm (cached) path must still return offerings under a bad
   network — we deliberately do not assert warm is faster than cold, because this SDK's warm path
   revalidates via conditional requests rather than doing a pure cache read, so warm latency
   ≈ cold latency and asserting `warm < cold` on two near-equal, high-variance timings would flake
   (that's the exact failure mode this design avoids). Separately, the `/config` sync is
   best-effort: if it fails (`FLAKY` profile with `failMatch = "/config"`), `getOfferings()` must
   still succeed and return offerings.

## Why round-trip counts + a same-run delta, not absolute-ms thresholds

Absolute latency thresholds (`assert elapsed < 500ms`) are machine-speed-dependent and flake on
shared/loaded CI runners — a slower runner or a noisy-neighbor build agent produces false
positives unrelated to the SDK. Counting round-trips is exact and deterministic regardless of
machine speed. The config-cost delta is normalized against a baseline measured in the same run on
the same hardware and is dominated by the injected delay, so it stays meaningful even if absolute
numbers vary between CI runs. Neither signal requires calibrating a magic millisecond number per
environment.

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
the real parser is tested against. A bare `{}` body would only exercise the SDK's parse-failure/
retry path, not the readiness gate this suite is actually measuring. `NetworkProfile`
(`GOOD` / `BAD` / `FLAKY`) wraps the dispatcher to inject per-request delay or targeted failures;
`Dispatcher.withPathDelay(match, delayMs)` (in `NetworkProfile.kt`) delays a single matching
endpoint instead, used by tier 2 to isolate `/config`'s cost. Because the SDK posts its final
`getOfferings()` callback to the main `Looper`, and Robolectric's looper is paused by default,
`PerfHarness.runCycle` polls `shadowOf(Looper.getMainLooper()).idle()` between short waits instead
of a plain `latch.await()` — a plain await deadlocks since the test itself runs on the main thread.

## Refreshing fixtures

See `purchases/src/test/resources/perf-fixtures/CAPTURE.md` for the procedure to re-record the
fixture JSON bodies (e.g. when the backend response contract changes).

## Relationship to the on-demand `getOfferings` benchmark (PR #3745)

PR #3745 added a separate, on-demand benchmark that drives a real backend from the perftester
sample app to measure absolute `getOfferings()` latency. That benchmark remains the tool for deep
absolute-latency investigation and is run manually, not on every PR. This package is complementary:
it is the automated CI regression gate — fast, deterministic, and machine-independent — that
catches structural regressions (extra round-trips, broken adversity handling) on every PR, while
PR #3745's benchmark is reserved for deliberate, real-network latency investigations.
