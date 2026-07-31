# GIT-427 Offerings Cache OOM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reproduce each GIT-427 allocation path independently, preserve the raw offerings response through the cache pipeline, stop offline cache rewrites, and make ETag cache reads safe on Android devices with a 128 MB heap.

**Architecture:** Add deterministic offerings-sized fixtures and exact allocation measurement first. Carry a structured `OfferingsResponse` containing both parsed and raw response forms, persist the raw string plus source metadata without JSON reserialization, and separate memory-only from disk cache updates. Replace the ETag decoder's full UTF-16 `CharBuffer` with checksum-validated direct byte-to-string construction, retaining an 8 MiB ETag bypass as a measured fallback if the direct implementation fails the low-heap gate.

**Tech Stack:** Kotlin 2.0.21, Android/Robolectric, JUnit 4, MockK, AssertJ, `org.json`, `SharedPreferences`, Java `ThreadMXBean`, SHA-256, Gradle.

## Global Constraints

- Work only in the Android SDK repository; fallback snapshot generation and HTTP compression are backend follow-ups.
- Never store an API key, captured project response, offering identifier, or real localization content in source, fixtures, logs, commits, or pull-request text.
- Preserve missing/corrupt ETag payload behavior as a self-healing cache miss.
- Preserve both `defaults` and `customEntitlementComputation` flavors.
- Make no public SDK API changes.
- Use imports rather than inline fully-qualified names.
- Do not introduce a `CompositionLocal`.
- A disk-loaded offerings response must refresh memory state without writing the response to disk again.
- Permanent allocation tests must assert allocation counts, not timing or GC behavior.
- The pull request must receive the `pr:fix` label.

---

## File Map

### New files

- `purchases/src/test/java/com/revenuecat/purchases/common/ThreadAllocationMeter.kt`
  - Shared exact per-thread allocation measurement for JVM memory regressions.
- `purchases/src/test/java/com/revenuecat/purchases/common/LargeOfferingsResponseGenerator.kt`
  - Synthetic, deterministic offerings-shaped JSON with no real project data.
- `purchases/src/test/java/com/revenuecat/purchases/common/LargeOfferingsResponseGeneratorTest.kt`
  - Fixture validity and size guarantees.
- `purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheMemoryTest.kt`
  - Regression gate for payload-independent offerings cache writes.
- `purchases/src/main/kotlin/com/revenuecat/purchases/common/OfferingsResponse.kt`
  - Internal boundary object carrying raw text, parsed JSON, and response source.

### Modified production files

- `purchases/src/main/kotlin/com/revenuecat/purchases/common/Backend.kt`
  - Return `OfferingsResponse` from the offerings callback.
- `purchases/src/main/kotlin/com/revenuecat/purchases/common/caching/DeviceCache.kt`
  - Store the exact raw response and source in separate preference entries.
- `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsCache.kt`
  - Read legacy/new source formats and expose separate memory-only/persisting operations.
- `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt`
  - Carry raw network text and avoid persisting disk-loaded responses.
- `purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagManager.kt`
  - Persist payload checksum metadata and reject legacy metadata before unsafe reads.
- `purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagPayloadStore.kt`
  - Return size/checksum write metadata and use checksum-validated direct UTF-8 construction.

### Modified test files

- `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerMemoryTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagPayloadStoreTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/common/backend/BackendTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsCacheTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/backend_integration_tests/FallbackURLBackendIntegrationTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/backend_integration_tests/LoadShedderUSEast1BackendIntegrationTest.kt`
- `purchases/src/test/java/com/revenuecat/purchases/backend_integration_tests/ProductionBackendIntegrationTest.kt`

---

### Task 1: Build reusable allocation and payload test infrastructure

**Files:**

- Create: `purchases/src/test/java/com/revenuecat/purchases/common/ThreadAllocationMeter.kt`
- Create: `purchases/src/test/java/com/revenuecat/purchases/common/LargeOfferingsResponseGenerator.kt`
- Create: `purchases/src/test/java/com/revenuecat/purchases/common/LargeOfferingsResponseGeneratorTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerMemoryTest.kt`

**Interfaces:**

- Produces: `ThreadAllocationMeter.measure(block: () -> Unit): Long`
- Produces: `LargeOfferingsResponseGenerator.generateAtLeast(targetChars: Int): GeneratedOfferingsResponse`
- Produces: `GeneratedOfferingsResponse(text: String, json: JSONObject)`
- Consumes: Existing `ETagManagerMemoryTest` warm-up logic.

- [ ] **Step 1: Add a failing fixture test**

```kotlin
package com.revenuecat.purchases.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LargeOfferingsResponseGeneratorTest {
    @Test
    fun `generates valid offerings-shaped JSON at or above the target size`() {
        val generated = LargeOfferingsResponseGenerator.generateAtLeast(64 * 1024)

        assertThat(generated.text.length).isGreaterThanOrEqualTo(64 * 1024)
        assertThat(generated.json.getJSONArray("offerings").length()).isEqualTo(1)
        assertThat(
            generated.json
                .getJSONArray("offerings")
                .getJSONObject(0)
                .getJSONObject("paywall_components")
                .getJSONObject("components_localizations")
                .getJSONObject("en_US")
                .getString("copy"),
        ).isNotEmpty()
    }
}
```

- [ ] **Step 2: Run the fixture test and verify RED**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.LargeOfferingsResponseGeneratorTest"
```

Expected: compilation fails because `LargeOfferingsResponseGenerator` does not exist.

- [ ] **Step 3: Implement the deterministic generator**

```kotlin
package com.revenuecat.purchases.common

import org.json.JSONObject

internal data class GeneratedOfferingsResponse(
    val text: String,
    val json: JSONObject,
)

internal object LargeOfferingsResponseGenerator {
    const val STRING_BUILDER_PREVIOUS_CAPACITY_CHARS = 9_437_182

    fun generateAtLeast(targetChars: Int): GeneratedOfferingsResponse {
        require(targetChars > 0)
        val prefix = """
            {"current_offering_id":"synthetic","offerings":[{"identifier":"synthetic","packages":[],"paywall_components":{"template_name":"synthetic","asset_base_url":"https://example.invalid/","components_config":{},"components_localizations":{"en_US":{"copy":"
        """.trimIndent()
        val suffix = "\"}},\"default_locale\":\"en_US\"}}]}"
        val valueLength = maxOf(1, targetChars - prefix.length - suffix.length)
        val text = prefix + "x".repeat(valueLength) + suffix
        return GeneratedOfferingsResponse(text, JSONObject(text))
    }
}
```

- [ ] **Step 4: Implement the shared allocation meter**

```kotlin
package com.revenuecat.purchases.common

import java.lang.reflect.Method

internal object ThreadAllocationMeter {
    private val managementFactoryGetThreadMXBean: Method =
        Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
    private val getThreadAllocatedBytesMethod: Method =
        Class.forName("com.sun.management.ThreadMXBean")
            .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)

    fun measure(block: () -> Unit): Long {
        val threadId = Thread.currentThread().id
        val before = allocatedBytes(threadId)
        check(before >= 0) { "Thread allocation tracking is unavailable." }
        block()
        return allocatedBytes(threadId) - before
    }

    private fun allocatedBytes(threadId: Long): Long {
        val bean = managementFactoryGetThreadMXBean.invoke(null)
        return getThreadAllocatedBytesMethod.invoke(bean, threadId) as Long
    }
}
```

- [ ] **Step 5: Refactor `ETagManagerMemoryTest` to consume the meter**

Remove its reflection fields and `measureAllocatedBytes`; replace calls with:

```kotlin
val storeBytes = ThreadAllocationMeter.measure {
    underTest.storeBackendResultIfNoError(URL, result, eTagInResponse = "etag")
}
```

Keep the existing warm-up pass and allocation thresholds unchanged.

- [ ] **Step 6: Run GREEN tests**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.LargeOfferingsResponseGeneratorTest" \
  --tests "com.revenuecat.purchases.common.networking.ETagManagerMemoryTest"
```

Expected: both pass; the existing profile remains approximately 0.5 MB store, a few KB header lookup, and 20 MB 304 read for a 5 MB payload.

- [ ] **Step 7: Commit**

```bash
git add \
  purchases/src/test/java/com/revenuecat/purchases/common/ThreadAllocationMeter.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/LargeOfferingsResponseGenerator.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/LargeOfferingsResponseGeneratorTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerMemoryTest.kt
git commit -m "test: add large offerings allocation fixtures"
```

---

### Task 2: Capture both current allocation signatures and add the DeviceCache regression gate

**Files:**

- Create: `purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheMemoryTest.kt`
- Record locally: `.context/GIT-427-memory-baseline.md`

**Interfaces:**

- Consumes: `GeneratedOfferingsResponse`
- Consumes: `ThreadAllocationMeter.measure`
- Produces: a failing requirement that `DeviceCache.cacheOfferingsResponse(String, HTTPResponseOriginalSource)` allocate less than 1 MiB.

- [ ] **Step 1: Temporarily measure `JSONObject.toString` immediately below and above the Android capacity boundary**

Start `DeviceCacheMemoryTest` with this temporary characterization:

```kotlin
@Test
fun `characterize JSONObject serialization around the Android builder boundary`() {
    listOf(
        LargeOfferingsResponseGenerator.STRING_BUILDER_PREVIOUS_CAPACITY_CHARS - 1024,
        LargeOfferingsResponseGenerator.STRING_BUILDER_PREVIOUS_CAPACITY_CHARS + 1024,
    ).forEach { target ->
        val generated = LargeOfferingsResponseGenerator.generateAtLeast(target)
        var serialized: String? = null
        val allocated = ThreadAllocationMeter.measure {
            serialized = generated.json.toString()
        }
        assertThat(serialized).isNotNull
        println("target=$target actual=${serialized!!.length} allocated=$allocated")
    }
}
```

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.DeviceCacheMemoryTest.characterize JSONObject serialization around the Android builder boundary" \
  --info
```

Copy only target length, actual compact length, and allocated bytes into `.context/GIT-427-memory-baseline.md`. Do not commit the temporary characterization test.

- [ ] **Step 2: Extend the ETag profile locally to 10 MB and 20 MB**

Run the existing ETag measurement logic with payload target constants of 10 MiB and 20 MiB, one size per invocation to limit peak test-worker memory. Record store, header, and 304-read allocation totals in `.context/GIT-427-memory-baseline.md`, then restore the permanent 5 MiB constant.

Expected: store/header remain bounded while 304-read allocation scales approximately linearly.

- [ ] **Step 3: Replace the temporary characterization with the desired failing regression**

```kotlin
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DeviceCacheMemoryTest {
    @Test
    fun `caching a large raw offerings response allocates less than one MiB`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("device_cache_memory_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val cache = DeviceCache(prefs, "test_api_key")
        val response = LargeOfferingsResponseGenerator.generateAtLeast(10 * 1024 * 1024)

        cache.cacheOfferingsResponse("{}", HTTPResponseOriginalSource.MAIN)
        val allocated = ThreadAllocationMeter.measure {
            cache.cacheOfferingsResponse(response.text, HTTPResponseOriginalSource.FALLBACK)
        }

        assertThat(allocated).isLessThan(1024L * 1024L)
        assertThat(
            prefs.getString(
                "com.revenuecat.purchases.test_api_key.offeringsResponse",
                null,
            ),
        ).isSameAs(response.text)
    }
}
```

- [ ] **Step 4: Run the DeviceCache regression and verify RED**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.DeviceCacheMemoryTest"
```

Expected: compilation fails because the raw-string/source overload does not exist.

Do not commit yet; Task 4 supplies the implementation that makes this gate green.

---

### Task 3: Preserve the raw response at the Backend boundary

**Files:**

- Create: `purchases/src/main/kotlin/com/revenuecat/purchases/common/OfferingsResponse.kt`
- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/Backend.kt`
- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/backend/BackendTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt`
- Modify: the three backend integration tests listed in the file map.

**Interfaces:**

- Produces:

```kotlin
internal data class OfferingsResponse(
    val body: JSONObject,
    val bodyString: String,
    val originalDataSource: HTTPResponseOriginalSource,
)
```

- Changes `Backend.getOfferings` success callback from `(JSONObject, HTTPResponseOriginalSource) -> Unit` to `(OfferingsResponse) -> Unit`.
- Extends `OfferingsManager.createAndCacheOfferings` with `offeringsResponseText: String?`; null means the input came from the device cache.

- [ ] **Step 1: Add a failing Backend identity test**

Configure `mockClient.performRequest` to return an `HTTPResult` containing:

```kotlin
val rawBody = """{"offerings":[],"current_offering_id":null}"""
```

Capture the success value and assert:

```kotlin
assertThat(received.bodyString).isSameAs(rawBody)
assertThat(received.body.getJSONArray("offerings").length()).isZero()
assertThat(received.originalDataSource).isEqualTo(HTTPResponseOriginalSource.MAIN)
```

- [ ] **Step 2: Run the Backend test and verify RED**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.backend.BackendTest"
```

Expected: compilation fails because `OfferingsResponse` and the single-argument callback do not exist.

- [ ] **Step 3: Add `OfferingsResponse` and update Backend**

In `Backend.onCompletion`, replace:

```kotlin
onSuccess(result.body, result.originalDataSource)
```

with:

```kotlin
onSuccess(
    OfferingsResponse(
        body = result.body,
        bodyString = result.payloadText,
        originalDataSource = result.originalDataSource,
    ),
)
```

Update `OfferingsCallback`, `getOfferings`, and every test lambda to accept one `OfferingsResponse`.

- [ ] **Step 4: Carry response text into OfferingsManager without changing cache behavior yet**

For network success:

```kotlin
{ response ->
    createAndCacheOfferings(
        offeringsJSON = response.body,
        offeringsResponseText = response.bodyString,
        originalDataSource = response.originalDataSource,
        loadedFromDiskCache = false,
        fetchGeneration = fetchGeneration,
        onError,
        onSuccess,
    )
}
```

For device-cache fallback, pass `offeringsResponseText = null`. Pass the same nullable value through the cache-generation retry.

- [ ] **Step 5: Run focused compile/tests**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.backend.BackendTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsManagerTest"
```

Expected: pass; cache behavior is unchanged, but raw text identity reaches `OfferingsManager`.

- [ ] **Step 6: Commit**

```bash
git add \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/OfferingsResponse.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/Backend.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/backend/BackendTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/backend_integration_tests/FallbackURLBackendIntegrationTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/backend_integration_tests/LoadShedderUSEast1BackendIntegrationTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/backend_integration_tests/ProductionBackendIntegrationTest.kt
git commit -m "refactor: preserve raw offerings responses"
```

---

### Task 4: Store the exact offerings string and separate source metadata

**Files:**

- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/caching/DeviceCache.kt`
- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsCache.kt`
- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheMemoryTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsCacheTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt`

**Interfaces:**

- Produces `DeviceCache.cacheOfferingsResponse(response: String, source: HTTPResponseOriginalSource)`.
- Produces `DeviceCache.getOfferingsResponseSource(): String?`.
- Produces `CachedOfferingsResponse(response: JSONObject, originalSource: HTTPResponseOriginalSource)`.
- Changes `OfferingsCache.cacheOfferings(offerings: Offerings, offeringsResponse: String)` to persist a network response.
- Temporarily retains the `JSONObject` overload for disk fallback; Task 5 removes its persistence behavior after proving the offline rewrite with a failing test.

- [ ] **Step 1: Add failing DeviceCache behavior tests**

Add tests that verify:

```kotlin
cache.cacheOfferingsResponse(rawResponse, HTTPResponseOriginalSource.FALLBACK)

verify {
    mockEditor.putString(offeringsResponseCacheKey, refEq(rawResponse))
    mockEditor.putString(offeringsResponseSourceCacheKey, HTTPResponseOriginalSource.FALLBACK.name)
    mockEditor.apply()
}
```

Also assert `clearOfferingsResponseCache` removes both keys in one editor and `getOfferingsResponseSource` returns the stored name.

- [ ] **Step 2: Run DeviceCache tests and verify RED**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.DeviceCacheTest" \
  --tests "com.revenuecat.purchases.common.DeviceCacheMemoryTest"
```

Expected: compilation fails for the new overload/source key.

- [ ] **Step 3: Implement exact raw storage**

Add:

```kotlin
private val offeringsResponseSourceCacheKey: String by lazy {
    "$apiKeyPrefix.offeringsResponseSource"
}

@Synchronized
internal fun getOfferingsResponseSource(): String? =
    preferences.getString(offeringsResponseSourceCacheKey, null)

@Synchronized
internal fun cacheOfferingsResponse(
    response: String,
    source: HTTPResponseOriginalSource,
) {
    preferences.edit()
        .putString(offeringsResponseCacheKey, response)
        .putString(offeringsResponseSourceCacheKey, source.name)
        .apply()
}
```

Remove the `JSONObject` overload. Clear both keys in `clearOfferingsResponseCache`.

- [ ] **Step 4: Make OfferingsCache resolve new and legacy source formats**

Add:

```kotlin
internal data class CachedOfferingsResponse(
    val response: JSONObject,
    val originalSource: HTTPResponseOriginalSource,
)
```

Resolve source in this order:

1. `deviceCache.getOfferingsResponseSource()`.
2. Legacy `response.optNullableString(ORIGINAL_SOURCE_KEY)`.
3. `HTTPResponseOriginalSource.MAIN`.

Catch `IllegalArgumentException`, log `"Invalid original data source for cached offerings"`, and default to main.

Change the network cache write to:

```kotlin
deviceCache.cacheOfferingsResponse(offeringsResponse, offerings.originalSource)
```

Keep the existing `JSONObject` overload for the disk path during this task, but route its final serialized string and source through the new `DeviceCache` method:

```kotlin
@Synchronized
fun cacheOfferings(offerings: Offerings, offeringsResponse: JSONObject) {
    updateInMemoryCache(offerings)
    deviceCache.cacheOfferingsResponse(offeringsResponse.toString(), offerings.originalSource)
}
```

Both overloads call a private `updateInMemoryCache(offerings: Offerings)` containing the instance, timestamp, and locale updates.

- [ ] **Step 5: Use the raw network string in OfferingsManager**

Branch without changing the disk behavior yet:

```kotlin
if (offeringsResponseText == null) {
    offeringsCache.cacheOfferings(offeringsResultData.offerings, offeringsJSON)
} else {
    offeringsCache.cacheOfferings(offeringsResultData.offerings, offeringsResponseText)
}
```

The first branch intentionally reproduces the existing offline rewrite until Task 5.

- [ ] **Step 6: Run GREEN tests**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.DeviceCacheTest" \
  --tests "com.revenuecat.purchases.common.DeviceCacheMemoryTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsCacheTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsManagerTest"
```

Expected: all pass; the 10 MiB device-cache write allocates less than 1 MiB and stores the same string instance.

- [ ] **Step 7: Commit Tasks 2 and 4**

```bash
git add \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/caching/DeviceCache.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsCache.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/DeviceCacheMemoryTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsCacheTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt
git commit -m "fix: cache raw offerings responses"
```

---

### Task 5: Stop rewriting disk-loaded offerings

**Files:**

- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsCache.kt`
- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsCacheTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt`

**Interfaces:**

- Produces `OfferingsCache.cacheOfferingsInMemory(offerings: Offerings)`.
- Keeps `OfferingsCache.cacheOfferings(offerings: Offerings, offeringsResponse: String)` for network responses.

- [ ] **Step 1: Change the existing disk-fallback test to require no persistence**

After factory success for `loadedFromDiskCache = true`, assert:

```kotlin
verify(exactly = 1) { cache.cacheOfferingsInMemory(testOfferings) }
verify(exactly = 0) { cache.cacheOfferings(any(), any()) }
```

Also retain assertions that the success callback receives `testOfferings` and the factory receives `loadedFromDiskCache = true`.

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsManagerTest.get offerings success is called if behavior should fallback and cached response"
```

Expected: compilation fails because `cacheOfferingsInMemory` does not exist.

- [ ] **Step 3: Extract the common in-memory update**

```kotlin
@Synchronized
fun cacheOfferingsInMemory(offerings: Offerings) = updateInMemoryCache(offerings)

private fun updateInMemoryCache(offerings: Offerings) {
    offeringsCachedObject.cacheInstance(offerings)
    offeringsCachedObject.updateCacheTimestamp(dateProvider.now)
    cachedLanguageTags = String(localeProvider.currentLocalesLanguageTags.toCharArray())
}
```

Keep the `String` overload calling `updateInMemoryCache` plus `deviceCache.cacheOfferingsResponse`. Delete the `JSONObject` overload after `OfferingsManager` uses `cacheOfferingsInMemory` for the disk path.

- [ ] **Step 4: Branch Manager cache behavior by response origin**

```kotlin
if (offeringsResponseText == null) {
    offeringsCache.cacheOfferingsInMemory(offeringsResultData.offerings)
} else {
    offeringsCache.cacheOfferings(offeringsResultData.offerings, offeringsResponseText)
}
```

The nullable text is null only for device-cache fallback; keep `loadedFromDiskCache` passed independently to `OfferingsFactory`.

- [ ] **Step 5: Run GREEN tests**

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsCacheTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsManagerTest"
```

Expected: pass; disk fallback updates memory/timestamp/locales and performs no device response write.

- [ ] **Step 6: Commit**

```bash
git add \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsCache.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/offerings/OfferingsManager.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsCacheTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/offerings/OfferingsManagerTest.kt
git commit -m "fix: avoid rewriting disk-cached offerings"
```

---

### Task 6: Add checksum metadata and remove the ETag decoder CharBuffer

**Files:**

- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagPayloadStore.kt`
- Modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagManager.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagPayloadStoreTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerTest.kt`
- Modify: `purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerMemoryTest.kt`

**Interfaces:**

- Produces:

```kotlin
internal data class ETagPayloadInfo(
    val sizeBytes: Long,
    val checksum: Checksum,
)
```

- Changes `ETagPayloadStore.write(urlString, payload)` to return `ETagPayloadInfo?`.
- Changes `ETagPayloadStore.read(urlString, expectedInfo)` to require size and SHA-256 integrity metadata.
- Adds `ETagCacheMetadata.payloadChecksum: Checksum?`.

- [ ] **Step 1: Add failing store integrity tests**

Cover:

```kotlin
val info = underTest.write(url, payload)!!
assertThat(info.sizeBytes).isEqualTo(payload.toByteArray().size.toLong())
assertThat(info.checksum).isEqualTo(
    Checksum.generate(payload.toByteArray(), Checksum.Algorithm.SHA256),
)
assertThat(underTest.read(url, info)).isEqualTo(payload)
```

After mutating one byte without changing file length:

```kotlin
assertThat(underTest.read(url, info)).isNull()
```

Continue covering multibyte content, empty payloads, truncation, missing files, failed writes, and atomic replacement.

- [ ] **Step 2: Add failing metadata compatibility tests**

Assert:

- Serialized new metadata round-trips `payloadChecksum`.
- Metadata without `payloadChecksum` produces empty ETag headers and no payload read.
- A 304 with metadata lacking checksum returns null so the existing refresh retry can fetch a 200.

- [ ] **Step 3: Add the failing allocation gate**

For 5 MiB, 10 MiB, and 20 MiB generated ASCII-heavy payloads:

```kotlin
val readBytes = ThreadAllocationMeter.measure {
    cachedResult = manager.getHTTPResultFromCacheOrBackend(/* 304 arguments */)
}
assertThat(readBytes).isLessThan(3L * payload.toByteArray().size)
```

Warm each size against a small separate URL before measuring. Run one large size per test method so references from earlier sizes cannot inflate the live set.

- [ ] **Step 4: Run ETag tests and verify RED**

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.networking.ETagPayloadStoreTest" \
  --tests "com.revenuecat.purchases.common.networking.ETagManagerTest" \
  --tests "com.revenuecat.purchases.common.networking.ETagManagerMemoryTest"
```

Expected: compilation failures for `ETagPayloadInfo` and checksum metadata, followed by an allocation failure until the decoder is replaced.

- [ ] **Step 5: Compute SHA-256 while streaming the write**

Wrap the file output in `DigestOutputStream`:

```kotlin
val digest = MessageDigest.getInstance(Checksum.Algorithm.SHA256.algorithmName)
FileOutputStream(tempFile).use { fileOut ->
    DigestOutputStream(fileOut, digest).use { digestOut ->
        encodeTo(digestOut, payload)
    }
}
val info = ETagPayloadInfo(
    sizeBytes = tempFile.length(),
    checksum = Checksum(
        Checksum.Algorithm.SHA256,
        digest.digest().toHexString(),
    ),
)
```

Return `info` only after `renameTo(file)` succeeds. Keep the fixed encoder buffers and strict write-side surrogate validation.

- [ ] **Step 6: Verify bytes before direct UTF-8 construction**

Replace the full decoder with:

```kotlin
val bytes = ByteArray(size)
DataInputStream(input).readFully(bytes)
val actualChecksum = Checksum.generate(bytes, Checksum.Algorithm.SHA256)
if (actualChecksum != expectedInfo.checksum) return null
String(bytes, Charsets.UTF_8)
```

The checksum proves the bytes are identical to the strictly encoded write, so the read does not need a second general-purpose strict decode pass.

- [ ] **Step 7: Persist checksum metadata and reject legacy entries before reading**

Add `payloadChecksum` serialization as the SHA-256 hex value. In `getStoredMetadata`, return null when checksum or payload size is missing. `getETagHeaders` must therefore omit the ETag for pre-fix entries, causing a direct 200 refresh instead of a 304 followed by unsafe decode.

Build `ETagPayloadInfo` from metadata and pass it to `payloadStore.read`.

Update `ETagManagerTest.mockCachedHTTPResult` to call `payloadStore.write`, copy both returned `sizeBytes` and `checksum` into `ETagCacheMetadata`, and only then serialize the metadata returned by mocked preferences.

- [ ] **Step 8: Run GREEN ETag tests**

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.networking.ETagPayloadStoreTest" \
  --tests "com.revenuecat.purchases.common.networking.ETagManagerTest" \
  --tests "com.revenuecat.purchases.common.networking.ETagManagerMemoryTest"
```

Expected:

- Store/header lookup each allocate less than 1 MiB.
- Reads allocate less than three times payload bytes at 5, 10, and 20 MiB.
- Corruption and pre-checksum entries are misses.

- [ ] **Step 9: Commit**

```bash
git add \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagPayloadStore.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagManager.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagPayloadStoreTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerMemoryTest.kt
git commit -m "fix: reduce ETag payload read allocations"
```

---

### Task 7: Run the 128 MB decision gate and add the 8 MiB bypass only if required

**Files:**

- Record locally: `.context/GIT-427-android-memory-results.md`
- Conditional modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagManager.kt`
- Conditional modify: `purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagPayloadStore.kt`
- Conditional modify: corresponding ETag tests.

**Interfaces:**

- Consumes: checksum-validated direct read from Task 6.
- Conditional produces: `MAX_ETAG_PAYLOAD_BYTES = 8L * 1024L * 1024L`
- Conditional produces: `ETagPayloadStore.remove(urlString: String)`.

- [ ] **Step 1: Verify the runtime heap**

On Android 10 and Android 15 test devices/emulators:

```bash
adb shell getprop dalvik.vm.heapgrowthlimit
adb shell dumpsys meminfo com.revenuecat.purchases.debug
```

Use only runtimes reporting a 128 MB growth limit for the decision result.

- [ ] **Step 2: Run isolated generated-payload scenarios**

Exercise:

1. A 10 MiB and 20 MiB ETag file read through a 304.
2. A 10 MiB raw offerings response persisted through `DeviceCache`.
3. A device-cache fallback with both network sources disabled.

Record payload size, source/status, heap before/after, success/failure, and whether a disk response write occurred. Do not record payload content.

- [ ] **Step 3: Apply the decision rule**

Keep Task 6 unchanged only if the 20 MiB 304 read and JSON parse succeed on both Android versions without OOM and the JVM allocation gate remains below three times payload bytes.

If either Android run fails, continue with Steps 4–8. If both pass, skip Steps 4–8 and record: `Direct checksum-validated decoding passed; oversized bypass not required.`

- [ ] **Step 4: Add failing oversized-entry tests**

Assert that:

- A backend payload of exactly 8 MiB remains ETag-cacheable.
- A payload of 8 MiB plus one byte leaves no metadata and no payload file.
- Existing metadata whose `payloadSizeBytes` exceeds 8 MiB produces empty ETag headers and no payload read.

- [ ] **Step 5: Run the conditional tests and verify RED**

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.networking.ETagManagerTest" \
  --tests "com.revenuecat.purchases.common.networking.ETagPayloadStoreTest"
```

Expected: oversized entries are currently retained.

- [ ] **Step 6: Implement the bounded bypass**

After a streamed write returns `ETagPayloadInfo`, compare `sizeBytes` with:

```kotlin
private const val MAX_ETAG_PAYLOAD_BYTES = 8L * 1024L * 1024L
```

For larger payloads:

```kotlin
payloadStore.remove(urlString)
prefs.value.edit().remove(urlString).apply()
return
```

Reject existing oversized metadata in `getStoredMetadata` before sending an ETag. `remove` must unlink only `fileFor(urlString)`.

- [ ] **Step 7: Run GREEN and repeat the 128 MB scenarios**

Confirm oversized payloads receive/refetch a 200 and never enter `ETagPayloadStore.read`; raw offerings device caching and offline fallback still succeed.

- [ ] **Step 8: Commit only when the bypass branch was required**

```bash
git add \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagManager.kt \
  purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/ETagPayloadStore.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagManagerTest.kt \
  purchases/src/test/java/com/revenuecat/purchases/common/networking/ETagPayloadStoreTest.kt
git commit -m "fix: bypass oversized ETag payloads"
```

---

### Task 8: Final compatibility and verification pass

**Files:**

- Modify only files whose tests expose a regression.
- Review: `docs/superpowers/specs/2026-07-31-git-427-offerings-cache-oom-debugging-design.md`
- Review: `.context/GIT-427-memory-baseline.md`
- Review: `.context/GIT-427-android-memory-results.md`

**Interfaces:**

- Consumes all production behavior and regression gates from Tasks 1–7.
- Produces the final evidence summary for the pull request.

- [ ] **Step 1: Run focused tests**

```bash
./gradlew :purchases:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.common.networking.*ETag*Test" \
  --tests "com.revenuecat.purchases.common.DeviceCacheTest" \
  --tests "com.revenuecat.purchases.common.DeviceCacheMemoryTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsCacheTest" \
  --tests "com.revenuecat.purchases.common.offerings.OfferingsManagerTest" \
  --tests "com.revenuecat.purchases.common.backend.BackendTest"
```

Expected: pass with clean test output.

- [ ] **Step 2: Run both purchases flavors**

```bash
./gradlew :purchases:testDefaultsDebugUnitTest
./gradlew :purchases:testCustomEntitlementComputationDebugUnitTest
```

Expected: both pass.

- [ ] **Step 3: Run static analysis**

```bash
./gradlew detektAll
```

Expected: pass.

- [ ] **Step 4: Check API compatibility**

```bash
./scripts/api-check.sh
```

Expected: pass with no public API changes.

- [ ] **Step 5: Audit the repository for prohibited data**

```bash
git diff origin/main... -- . ':!docs/superpowers/**' | \
  rg 'goog_|api-production\\.8-lives-cat\\.io.*Authorization|components_localizations_resolved.*[A-Za-z]{20}' && exit 1 || true
git status --short
```

Expected: no API key, captured response, or real localization content; only intended source/test/plan changes.

- [ ] **Step 6: Review final allocation evidence**

Confirm:

- Device offerings cache write: less than 1 MiB beyond fixture setup.
- Offline device-cache fallback: zero response persistence calls.
- ETag store/header lookup: less than 1 MiB each.
- Selected ETag 304 behavior: passes the 128 MB Android gate.
- Corruption and legacy metadata: self-healing misses.

- [ ] **Step 7: Request code review**

Use `superpowers:requesting-code-review` and address findings before publishing.

- [ ] **Step 8: Prepare the pull request**

Follow `~/.claude/voice.md`, summarize the three independently reproduced paths, include aggregate allocation results without payload content, and add the `pr:fix` label.
