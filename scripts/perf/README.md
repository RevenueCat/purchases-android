# Offerings Startup Performance Harness

Measures whether loading offerings on app startup gets slower after moving workflows/paywalls to the
`/v1/config` remote configuration system, under realistic mobile network conditions.

The headline question:

```text
How long from calling getOfferings until its callback fires?
```

Plus the co-headline "render readiness" question the headline number can hide: once `getOfferings` returns,
how long until the `ui_config` and the first workflow body are actually available — and are they *complete*,
or did they silently fall back to defaults?

## Components

- **`OfferingsStartupPerfTest`** (`purchases/src/androidTestDefaults/.../perftests/`): the instrumentation
  harness. Timestamps are taken in-process with a monotonic clock around the SDK calls (no UI polling).
  Writes one JSON result file per run to `getExternalFilesDir()/revenuecat-perf/`. Self-skips when
  `PERF_TEST_API_KEY` is not provided, so it is inert in regular `connectedAndroidTest` runs.
- **`run_offerings_startup_perf.py`**: driver. Builds/installs the test APK, runs the scenario × cache-mode
  matrix via `am instrument`, pulls result files, and prints P50/P90/P95/max tables per metric.

## Scenarios (`PERF_TEST_SCENARIO`)

| Scenario | Meaning |
|---|---|
| `baseline` | Workflows disabled. The pre-migration `getOfferings` path with no `/v1/config` dependency. |
| `workflows` | Workflows enabled. `getOfferings` gates its callback on config readiness (`WorkflowManager.onPaywallConfigReady`). |
| `workflows_config_404` | Workflows enabled; `/v1/config` synthesizes a 404 in-process. Exercises the session kill-switch. The 4xx is instant, so this measures kill-switch *behavior* (no hang, fallback, steady-state cost after the switch trips), not the network cost of a real 4xx round trip — run it under shaping with a real server 4xx for that. |
| `workflows_config_500` | Workflows enabled; `/v1/config` requests are routed to the force-server-failure URL. The `SHOULD_RETRY` path: the endpoint keeps being attempted on later syncs (packet loss never trips the kill-switch — only a real 4xx does). |
| `workflows_config_unreachable` | Workflows enabled; `/v1/config` requests go to an unreachable address. Measures the gate's worst case: blocking until the HTTP client's transport timeout resolves the request. |

The failure injection targets only the `/v1/config` endpoint; offerings and blob traffic stay real, matching
an incident where just the config endpoint is unhealthy.

## Cache modes (`PERF_TEST_CACHE_MODE`)

Offerings, remote config, and the blob store are **independent** caches, so warmth is not a single bit:

| Mode | Meaning |
|---|---|
| `cold` | Everything cleared + fresh app user id. Fresh-install approximation. |
| `warm_disk` | Primed by an untimed full run, then the SDK instance is recreated. In-memory caches are gone (offerings refetches over the network, with warm ETags); config disk cache and blob store are warm. App-restart approximation. |
| `warm_memory` | Primed on the same instance. The timed call vends the in-memory offerings cache and committed config — isolates the pure gate overhead on the path that was effectively instant before the workflows migration. |
| `warm_offerings_cold_config` | Primed, instance recreated, then *only* the remote config disk cache and blob store are deleted. The post-app-update migration state: offerings data exists, the config layer starts cold, and the gate must wait for a full `/v1/config` sync. Expected to be the most regression-prone cell. |

## Recorded metrics

Per iteration (all durations in ms, monotonic clock):

- `get_offerings_ms` — headline: `getOfferings` call → callback.
- `configure_to_offerings_ms` — `Purchases.configure` → offerings callback (not recorded for `warm_memory`).
- `ui_config_ms`, `ui_config_is_default` — `ui_config` readiness after offerings, and whether the
  all-or-nothing merge silently fell back to a default `UiConfig` (a *fast but degraded* result).
- `workflow_resolution_ms`, `workflow_body_ms`, `workflow_resolved` — offering→workflow id resolution and
  first workflow body load after offerings. Under the current gate, blob-backed bodies are not guaranteed
  ready when `getOfferings` returns, so this is the number that makes blob-threshold tradeoffs visible.
- `http_events` — per-endpoint request timings (`get_offerings`, `remote_config`, ...) harvested from the
  SDK's own diagnostics events (`http_request_performed`): response time, response code, ETag hit, retry.

Aggregates (nearest-rank P50/P90/P95, min/max/mean) are embedded in each result file and recomputed
cross-run by the driver.

### Known measurement caveats

- **Blob downloads are not in `http_events`**: they bypass `HTTPClient` (plain `HttpURLConnection`), so they
  emit no diagnostics events. Count blob requests/bytes at the network shaping layer.
- **Do not use the SDK's `get_offerings_result` diagnostics event as the source of truth for warm-cache
  runs**: on the cached path it is tracked *before* the workflows gate, so its `response_time_millis`
  underreports exactly the gate overhead this harness measures. The harness's own timestamps are the truth.
- **Billing is mocked**: a `StoreProduct` is fabricated for every product id the offerings response
  references, so Play Store latency/variance is excluded by design. For Play Store projects the Google
  offering parser matches packages by `(productId, basePlanId)` — pass the project's base plan ids via
  `PERF_TEST_BASE_PLAN_IDS` (e.g. `monthly,annual`) or packages will not resolve and `getOfferings` will
  error. Test Store projects match by product id alone and need no base plan ids.
- The kill-switch is **session-scoped and memory-only**. `cold` × `workflows_config_404` measures the
  first-call-of-session cost (the gate still waits for the 4xx); `warm_memory` × `workflows_config_404`
  measures steady state after the switch has tripped.

## Running

### One-off via Gradle (single cell)

```bash
./gradlew :purchases:connectedDefaultsDebugAndroidTest \
  -PPERF_TEST_API_KEY=goog_xxx \
  -PPERF_TEST_SCENARIO=workflows \
  -PPERF_TEST_CACHE_MODE=warm_offerings_cold_config \
  -PPERF_TEST_ITERATIONS=10 \
  -PPERF_TEST_NETWORK_LABEL=ideal \
  -PTEST_PACKAGE_FILTER=com.revenuecat.purchases.perftests
```

(Values can also live in `local.properties`; see `local.properties.example`.)

### Full matrix via the driver

```bash
python3 scripts/perf/run_offerings_startup_perf.py run \
  --api-key goog_xxx \
  --iterations 10 \
  --network-label ideal \
  --project-label large-loseit-copy \
  --base-plan-ids monthly,annual
```

Results are pulled to `perf-results/` and summarized as:

```text
scenario  cache  network  project  n  p50  p90  p95  max  errors  fallbacks
```

Run the same matrix once per network condition, changing `--network-label` each time.

## Network shaping

Shaping is applied **outside** the harness so that offerings, `/v1/config`, *and* blob/CDN traffic are all
shaped uniformly. Do not use `Purchases.proxyURL`-based shaping for this: it reroutes only API-base-URL
traffic, and blob downloads resolve their own source URLs from the config response, so the blob fan-out —
exactly the traffic under investigation — would escape the shaping.

Suggested setups, in order of fidelity:

1. **Linux host + `tc netem`** (shapes everything, supports loss):

   ```bash
   # LTE-ish: ~70ms RTT, 12/6 Mbps, then add loss per run.
   sudo tc qdisc add dev <iface> root netem delay 35ms 10ms rate 12mbit loss 10%
   # ...run the matrix...
   sudo tc qdisc del dev <iface> root
   ```

   With the Android emulator, apply this on the host's outbound interface (emulator traffic NATs through
   the host). Sweep loss at 10/20/30% per the test plan.

2. **Android emulator radio shaping** (latency/bandwidth only, no packet loss):

   ```bash
   emulator -avd <avd> -netdelay 100:200 -netspeed lte
   ```

3. **toxiproxy as a transparent TCP gateway** for finer control (per-connection latency/bandwidth/timeouts),
   if host-level `tc` is not available.

Record whatever you applied in `--network-label` (e.g. `lte_loss20`) — the harness treats it as an opaque
grouping key.

## Interpreting results — the decision checklist

Per the test plan, the questions each comparison should answer:

1. Is `get_offerings_ms` meaningfully worse in `workflows` vs `baseline`? On which cache states?
2. Is the regression concentrated in `warm_offerings_cold_config` (the migration cell) and cold starts?
3. Does packet loss amplify the regression (compare `ideal` vs `lte_loss*` labels)?
4. Does `workflows_config_404` return the gate to ~baseline after the first call of the session?
5. Does `workflows_config_unreachable` bound the gate at an acceptable worst case, or does the transport
   timeout strand startup for tens of seconds?
6. Are there `fallbacks` (> 0 `ui_config_is_default` / unresolved workflows)? A fast run with fallbacks is a
   degraded user experience, not a win.
7. Do `workflow_body_ms` / `ui_config_ms` shift as the backend's inline-blob threshold changes? Larger
   inline limits shift cost into the `/v1/config` response (visible in cold-config cells); smaller limits
   shift it into blob fan-out (visible in render readiness and under loss).
