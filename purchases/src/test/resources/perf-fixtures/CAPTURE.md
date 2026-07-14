# Capturing Perf Test Fixtures

This directory contains recorded HTTP response bodies for the RevenueCat SDK's `getOfferings()` request sequence in a performance test scenario. These fixtures are minimal-but-valid baselines and may be regenerated when the backend contract changes.

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
   - The SDK makes three requests (documented in the Task 1 spike analysis):
     - `GET /rcbilling/v1/subscribers/{id}/products?id=…` → save as `products.json`
     - `GET /v1/subscribers/{id}/offerings` → save as `offerings.json`
     - `POST /v1/config/app` → save as `config.json`
     - Fallback: `GET /v1/subscribers/{id}` → save as `subscribers.json` (not always observed on happy path)

5. **Sanitize response bodies:**
   - Rewrite any absolute URLs pointing to blob/asset hosts to the placeholder host: `http://PERF_MOCK_HOST/`
   - This allows the MockWebServer used in tests to serve the fixtures without external dependencies

6. **Save each response body:**
   - Write the JSON response to the corresponding file (`products.json`, `offerings.json`, `config.json`, `subscribers.json`)
   - Validate each file is well-formed JSON (see validation below)

7. **Update `manifest.json`:**
   - Document the request paths and response file locations in `manifest.json`
   - **Order is significant**: The list is matched by first `path.contains(match)`, so `/products` must precede `/v1/subscribers` (the products path also contains `v1/subscribers`)

### Validation

Verify all fixture files are well-formed JSON:

```bash
cd purchases/src/test/resources/perf-fixtures
for f in manifest.json offerings.json products.json config.json subscribers.json; do
  python3 -m json.tool "$f" > /dev/null && echo "OK $f" || echo "BAD $f"
done
```

All five files should report `OK`.

## Refreshing Fixtures

When the RevenueCat backend contract changes (e.g., new fields in offerings, product structure changes), regenerate these fixtures by repeating the capture steps above. The committed baseline is minimal but valid; a real capture can replace it with more comprehensive data.
