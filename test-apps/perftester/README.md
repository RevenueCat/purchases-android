# perftester

Measures `configure()` + `getOfferings()` latency, baseline vs. workflows, from a real app.

## Setup

Set `PERF_TESTER_API_KEY` in `local.properties` (a Test Store key: products then resolve through
`SimulatedStoreBillingWrapper` with no Play setup) and install:

    ./gradlew :test-apps:perftester:installDebug

## Use

- Toggle picks the arm: baseline or workflows (`DangerousSettings.forWorkflows()`).
- "Configure + getOfferings" runs one measured cycle: `close()` if configured, record `had_*` cache
  flags, `configure()`, `getOfferings()`, then (workflows arm) `ui_config` and first workflow body
  readiness. One JSON line is appended to `results.jsonl` in the app's external files dir per press.
- "Clear SDK caches" deletes the SDK prefs and cache dirs, so the next press is a cold sample
  without reinstalling.
- The first press after install/clear is cold; later presses are warm. The `had_*` flags in the data
  record which one each sample actually was.

## Scripted runs

    adb shell am force-stop com.revenuecat.perftester
    adb shell am start -n com.revenuecat.perftester/.MainActivity -e autorun true -e workflows true

Loop that with `sleep` between iterations, then:

    adb pull /sdcard/Android/data/com.revenuecat.perftester/files/results.jsonl .

Each line is one press; aggregate percentiles per arm (`workflows_enabled`) and cache state
(`had_offerings_cache`, `had_remote_config_cache`).
