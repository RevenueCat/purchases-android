# Capturing Perf Test Fixtures

This directory contains recorded HTTP response bodies for the RevenueCat SDK's `getOfferings()` request sequence in a performance test scenario. These fixtures are minimal-but-valid baselines and may be regenerated when the backend contract changes.

**`/v1/config` is not captured here.** It is RC Container Format (binary), not JSON, so it can't be
sanitized and stored as a plain JSON fixture the way the other endpoints are. Instead it's built
programmatically by `PerfConfigFixture.kt` (next to the test sources), which assembles a valid RC
Container v1 blob — `workflows` and `ui_config` topics, every referenced blob inlined — via the
same internal test helper (`RCContainerTestData.buildContainer`) that backs `RCContainerTest`. A
bare `{}` body would only exercise the SDK's parse-failure/retry path, not the paywall-config
readiness gate this perf suite measures, so if the RC Container wire format changes, update
`PerfConfigFixture.kt` directly rather than looking for a `config.json` here.

## One-Time Capture Procedure

The perftester app on a separate development branch can regenerate these fixtures. Alternatively, to record them from scratch:

### Prerequisites
- Android emulator or device running an app configured with a RevenueCat SDK `test_` API key
- The app must have workflows and UI config provisioned in the backend
- A local capturing proxy (e.g., Proxyman, Charles, mitmproxy) installed and configured to intercept HTTPS

### Capture Steps

1. **Start the capturing proxy** on your machine and configure it to intercept HTTPS traffic.
   - For mitmproxy: `mitmproxy -p 8080`
   - For Proxyman: Open the app and enable recording

2. **Configure the Android app to route through the proxy:**
   - In emulator settings or device Wi-Fi proxy settings, point HTTPS traffic to your proxy's address and port (typically `localhost:8080`)

3. **Trigger a cold `getOfferings()` call:**
   - Start the app fresh (or clear its cache)
   - Call `Purchases.sharedInstance.getOfferings()` in your integration
   - Observe the network trace in the proxy

4. **Extract the recorded request sequence:**
   - A cold `getOfferings()` on the default (remote-config-on) path makes three requests, confirmed
     by the perf suite itself (`GetOfferingsPerfTest.defaultPathMakesExactlyTheExpectedRoundTrips`
     logs the observed sequence):
     - `GET /v1/subscribers/{id}/offerings` → save as `offerings.json`
     - `GET /rcbilling/v1/subscribers/{id}/products?id=…` → save as `products.json`
     - `GET /v1/config/app` → **do not save as JSON**; see the RC Container note above instead
     - Fallback: `GET /v1/subscribers/{id}` → save as `subscribers.json` (not always observed on happy path)

5. **Sanitize response bodies:**
   - Rewrite any absolute URLs pointing to blob/asset hosts to the placeholder host: `http://PERF_MOCK_HOST/`
   - This allows the MockWebServer used in tests to serve the fixtures without external dependencies

6. **Save each response body:**
   - Write the JSON response to the corresponding file (`products.json`, `offerings.json`, `subscribers.json`)
   - Validate each file is well-formed JSON (see validation below)

7. **Update `manifest.json`:**
   - Document the request paths and response file locations in `manifest.json`
   - **Order is significant**: The list is matched by first `path.contains(match)`, so `/products` must precede `/v1/subscribers` (the products path also contains `v1/subscribers`)
   - `/config` is intentionally absent from `manifest.json`: `PerfFixtures.dispatcher` special-cases it and serves `PerfConfigFixture`'s bytes directly, ahead of the manifest lookup.

### Validation

Verify all fixture files are well-formed JSON:

```bash
cd purchases/src/test/resources/perf-fixtures
for f in manifest.json offerings.json products.json subscribers.json; do
  python3 -m json.tool "$f" > /dev/null && echo "OK $f" || echo "BAD $f"
done
```

All four files should report `OK`.

## Refreshing Fixtures

When the RevenueCat backend contract changes (e.g., new fields in offerings, product structure changes), regenerate these fixtures by repeating the capture steps above. The committed baseline is minimal but valid; a real capture can replace it with more comprehensive data.
