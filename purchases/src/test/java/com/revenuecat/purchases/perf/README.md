# Perf regression gate

This package is a Robolectric-based CI gate for the `getOfferings()` request path. It compares
the legacy path against the workflows path (`DangerousSettings.forWorkflows()`) in the same test
run, driven through a local `MockWebServer` under injected network conditions. It needs no
emulator and rides the normal `src/test` unit-test lane — see "Where this runs in CI" below.

## What it asserts

The suite (`GetOfferingsPerfTest`) gates on three tiers, in order of how load-bearing they are:

1. **Round-trip structure (primary).** The legacy path must never hit `/config`; the workflows
   path must hit it exactly once, and both paths issue exactly one `/offerings` and one
   `/products` round-trip. The workflows path adds exactly one request over baseline — no other
   extra round-trips. This is the main regression gate: if a future change makes the workflows
   path issue more requests than the documented `+1 /config` sync, this test fails regardless of
   how fast or slow the machine running it is.

2. **Same-run feature/baseline latency ratio (secondary).** Under an injected slow ("BAD")
   network profile, the median `getOfferings()` latency over several cold-start iterations is
   compared: `feature / baseline < RATIO_BOUND` (currently `3.0`). Both numbers are measured in
   the same test run against the same MockWebServer, so the ratio is a relative signal, not an
   absolute one.

3. **Behavior under adversity.** The warm (cached) path must still return offerings under a bad
   network — we deliberately do not assert warm is faster than cold, because this SDK's warm path
   revalidates via conditional requests rather than doing a pure cache read, so warm latency
   ≈ cold latency and asserting `warm < cold` on two near-equal, high-variance timings would flake
   (that's the exact failure mode this design avoids). Separately, the workflows `/config` sync is
   best-effort: if it fails (`FLAKY` profile with `failMatch = "/config"`), `getOfferings()` must
   still succeed and return offerings.

## Why round-trip counts + a same-run ratio, not absolute-ms thresholds

Absolute latency thresholds (`assert elapsed < 500ms`) are machine-speed-dependent and flake on
shared/loaded CI runners — a slower runner or a noisy-neighbor build agent produces false
positives unrelated to the SDK. Counting round-trips is exact and deterministic regardless of
machine speed. The ratio check is normalized against a baseline measured in the same run on the
same hardware, so it stays meaningful even if absolute numbers vary between CI runs. Neither
signal requires calibrating a magic millisecond number per environment.

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
`purchases/src/test/resources/perf-fixtures/`. `NetworkProfile` (`GOOD` / `BAD` / `FLAKY`) wraps
that dispatcher to inject per-request delay or targeted failures. Because the SDK posts its final
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
