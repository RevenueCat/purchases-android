# GIT-427 Offerings Cache OOM Debugging Design

## Objective

Determine the allocation mechanism and trigger for each GIT-427 crash signature, reproduce each path independently, and fix the SDK so large offerings responses do not cause fatal payload-sized cache allocations on Android devices with a 128 MB heap growth limit.

The work is SDK-only. The fallback service's response shape and HTTP compression remain a backend follow-up, although the investigation will preserve measurements that explain why fallback responses reach the SDK thresholds more often.

## Known Evidence

Three related but distinct memory paths exist:

1. The closed issue #3628 serialized an `HTTPResult` payload through nested JSON strings while writing the ETag cache. PR #3774 replaced that write with a fixed-buffer file encoder and small metadata entry.
2. GIT-427's `DeviceCache.cacheOfferingsResponse` signature serializes an already-parsed `JSONObject` into a new `String`. Android's default `JSONStringer` crosses a deterministic capacity boundary at 9,437,182 characters and requests a 37,748,744-byte backing array.
3. GIT-427's `ETagPayloadStore.read` signature occurs while reading a cached response after a `304 Not Modified`. The current implementation retains a file-sized `ByteArray`, allocates a UTF-16 `CharBuffer`, and then creates a `String`.

The existing `ETagManagerMemoryTest` measured the current behavior for a 5 MB offerings-like payload:

- ETag store: 511 KB allocated.
- ETag header lookup: 3 KB allocated.
- ETag 304 cache-hit read: 20,506 KB allocated.

The tester project comparison independently confirmed a response-shape difference:

- Both endpoints returned 149 offerings and 82 paywalls.
- All 82 paywalls from both endpoints had `components_localizations`.
- The main endpoint had zero `components_localizations_resolved` fields.
- The fallback endpoint had `components_localizations_resolved` in all 82 paywalls.
- 72 of those 82 fallback fields exactly matched `components_localizations`; the remaining 10 had the same locale count but some differing content.

The tester response is not large enough to reproduce the production OOM, so committed tests must use generated payloads rather than the tester project data.

## Root-Cause Hypotheses

### H1: Device cache serialization

The network response `String` and parsed `JSONObject` remain reachable when `OfferingsCache` calls `DeviceCache.cacheOfferingsResponse`. Re-serializing that tree creates a geometrically growing `StringBuilder`; an output slightly above 9,437,182 characters requires the observed 37,748,744-byte allocation.

Evidence required to confirm H1:

- A generated response immediately below the boundary does not request the next builder capacity.
- A generated response immediately above the boundary does.
- The high allocation is attributable to `JSONObject.toString()`, not `SharedPreferences.Editor.putString` or `apply`.

### H2: ETag 304 disk read

An ETag cache hit reads the entire file into a byte array, `CharsetDecoder.decode` allocates a file-sized UTF-16 character buffer, and `CharBuffer.toString` creates another payload copy. This makes a cache hit allocate roughly four times the ASCII-heavy payload size on the JVM and can require a single character array twice the payload byte size on Android.

Evidence required to confirm H2:

- A direct `ETagPayloadStore.read` and an `ETagManager` 304 cache hit have the same payload-proportional allocation slope.
- A 200 store and ETag header lookup remain bounded below 1 MB.
- The `read` path is entered for a 304 and not for a cold 200.

### H3: Offline cache rewrite

When both hosts fail, `OfferingsManager` parses the device-cached `JSONObject` with `loadedFromDiskCache = true`, then calls the same `OfferingsCache.cacheOfferings` method used for network responses. That method writes the response to disk again, reproducing H1 without a successful network request.

Evidence required to confirm H3:

- A disk-fallback success updates the in-memory offerings cache and invokes the success callback.
- The same flow currently calls the device response-cache write exactly once.
- After the fix, it performs no device response-cache write.

### H4: Fallback correlation

Fallback is a trigger rather than a source-specific SDK bug. Its separately generated project-level response may be larger than the main subscriber-scoped response, so it crosses H1 and H2 thresholds more often. Any response from either host can trigger the SDK bugs if it is sufficiently large.

Evidence required to confirm H4:

- Generated payload size, independent of `HTTPResponseOriginalSource`, predicts the allocation.
- Main and fallback response measurements are retained only as diagnostic context.

## Investigation Architecture

### Generated response fixture

Create a test-only `LargeOfferingsResponseGenerator` that produces valid, deterministic offerings JSON without project identifiers, API keys, real localization text, or captured server responses. It will accept a target compact length and generate repeated paywall components and locale maps until the response is:

- Just below the Android `StringBuilder` boundary.
- Just above the boundary.
- Approximately 10 MB.
- Approximately 20 MB.

The generator will expose both the raw `String` and parsed `JSONObject` so tests can deliberately retain the same live objects as the production network path.

### Allocation measurement

Extract the reflection-based `ThreadMXBean` measurement from `ETagManagerMemoryTest` into a test-only `ThreadAllocationMeter`. JVM tests will use exact per-thread allocation counts after warming class initialization.

Allocation tests will not assert timing or GC behavior. They will assert:

- Whether allocation grows with payload size.
- Whether a cache operation makes an avoidable payload-sized copy.
- Whether the stored `String` is the exact instance provided by the network path.

### Low-heap Android reproduction

Use a local instrumentation test or small test application configured with a 128 MB heap limit. Run three isolated scenarios:

1. Cold fallback-like 200 with no ETag or device cache.
2. Warm 304 with an ETag payload file already present.
3. Both network sources unavailable with a device-cached offerings response.

For each scenario, record only:

- Response source and status.
- Raw payload byte and character counts.
- ETag payload file size.
- Whether `loadedFromDiskCache` is true.
- Java heap used before and after each cache boundary.

Do not log response bodies, API keys, offering identifiers, or localization content.

The JVM allocation gates are permanent CI tests. The 128 MB Android run is a required manual verification recorded in the pull request because heap and GC behavior are runtime-specific and unsuitable as a stable CI assertion.

## Fix Design

### Offerings device cache

Carry the original response text alongside its parsed `JSONObject` and `HTTPResponseOriginalSource` from `Backend` to `OfferingsManager`. Cache the exact original `String` instead of calling `JSONObject.toString`.

Persist the response source in a separate `SharedPreferences` entry. Continue reading legacy cached JSON that contains `rc_original_source`; prefer the separate source entry for newly written responses.

Split the offerings cache operation into:

- Updating the in-memory `Offerings`, timestamp, and locale metadata.
- Persisting a new network response.

A disk-fallback parse performs only the first operation. A successful network response performs both.

### ETag payload read decision gate

A `Reader` feeding a `StringBuilder` is not automatically acceptable: it can replace the decoder's full `CharBuffer` with an equally large builder backing array. Benchmark these candidates against the same 5 MB, 10 MB, and 20 MB fixtures:

1. **Validated byte-to-string read:** preserve file integrity with size plus checksum metadata, then construct the `String` directly from verified UTF-8 bytes without the general-purpose decoder's full `CharBuffer`.
2. **Oversized ETag bypass:** do not persist ETag metadata for payloads that fail a conservative low-heap safety limit, so the next request receives a 200 instead of attempting a dangerous 304 disk read.
3. **File-backed or stream-parsed cached body:** keep the payload file-backed through `HTTPResult` and parse JSON without materializing an intermediate payload `String`.

Selection rules:

- Choose candidate 1 only if a 20 MB cached payload successfully reads and parses with a 128 MB heap, no single temporary allocation exceeds the payload byte size, total read allocations are no more than three times payload bytes, corrupt files remain cache misses, and the ETag store/header paths remain below 1 MB.
- Otherwise choose candidate 2 as the bounded, low-risk fix and document the extra network request trade-off.
- Candidate 3 requires a separate design review because it changes `HTTPResult.Payload` and multiple backend consumers; do not include it in GIT-427 unless candidates 1 and 2 both fail required behavior.

The oversized bypass threshold, if needed, will be fixed at 8 MiB. This is below the observed 9.44-million-character `StringBuilder` boundary and limits an ASCII UTF-16 decode to roughly 16 MiB before other live objects. Existing entries larger than the threshold will be treated as cache misses and removed.

## Error Handling and Compatibility

- Missing, truncated, checksum-mismatched, or invalid cached ETag payloads remain cache misses and use the existing refresh retry.
- ETag metadata written by an older SDK without a checksum is treated as a one-time cache miss and replaced by a fresh response; it is never decoded through the unsafe legacy read path.
- An ETag payload write must complete before its metadata is committed.
- Clearing ETag caches removes payload and integrity metadata.
- Invalid separately stored offerings source values log the existing error and default to `HTTPResponseOriginalSource.MAIN`.
- Clearing the offerings response cache removes both the response and separate source entry.
- An SDK downgrade can still parse the raw offerings JSON but will not understand the separate source entry; it will default cached response source to main.
- No public SDK API changes are allowed.

## Test and Verification Matrix

### Permanent JVM tests

- Generator produces valid JSON at requested size boundaries.
- Device response-cache write stores the same `String` instance and allocates less than 1 MB beyond fixture setup.
- Legacy embedded offerings source and new separate source both read correctly.
- Disk fallback updates memory and callbacks without persisting the response again.
- ETag 200 store and header lookup remain below 1 MB.
- ETag 304 read allocation is measured at 5 MB, 10 MB, and 20 MB.
- Multibyte UTF-8 spanning internal buffers round-trips.
- Invalid UTF-8, truncation, and integrity mismatches are cache misses.
- The selected ETag behavior passes its decision-rule allocation gates.

### Focused suites

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.networking.*ETag*Test" \
  --tests "com.revenuecat.purchases.common.DeviceCacheTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsCacheTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsManagerTest" \
  --tests "com.revenuecat.purchases.common.backend.BackendTest"
```

Then run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest
./gradlew :purchases:testCustomEntitlementComputationDebugUnitTest
./gradlew detektAll
```

Run `./scripts/api-check.sh` if any internal signature is visible to Metalava.

### Manual Android verification

On Android 10 and Android 15 runtimes with a 128 MB heap:

- Run cold 200, warm 304, and offline disk-fallback scenarios.
- Confirm no OOM and no repeated offline disk write.
- Confirm offerings content, original source, cache freshness, and callbacks remain correct.
- Attach aggregate allocation and heap observations to the pull request without payload content.

## Completion Criteria

GIT-427 is complete when:

- H1, H2, and H3 are reproduced independently and the evidence is recorded in tests or the pull request.
- The device offerings cache never serializes a network `JSONObject`.
- A disk-loaded offerings response is not persisted again.
- The selected ETag behavior passes the 128 MB Android verification and permanent allocation gates.
- Cache corruption continues to self-heal as a miss.
- Main and fallback responses produce equivalent SDK offerings behavior.
- Focused tests, both purchases flavor unit-test suites, Detekt, and applicable API checks pass.
