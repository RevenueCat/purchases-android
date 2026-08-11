# Audiences Remote-Config Ingestion Design

## Goal

Add Android support for the app-level `audiences` remote-config topic. Prefetched audience payloads must be decoded into schema-agnostic `JsonObject` values and retained in memory for future rule evaluation, which remains out of scope.

## Reference and scope

The design follows the approach in RevenueCat purchases-ios PR #7390: the provider exposes the complete audience payload without defining a concrete audience schema. Android additionally keeps prefetched decoded payloads in a generation-guarded memory cache, as required by WFL-451.

This change does not evaluate audience rules, interpret payload fields, or introduce public SDK API.

## Architecture

### Topic registration

Add `Audiences("audiences")` to `RemoteConfigTopic`. The generic remote-config manager continues to own topic persistence, blob prefetching, integrity checks, and inline-versus-remote blob resolution.

### Provider

Add an internal `AudiencesConfigProvider` in `common/audiences`. Its external shape is:

```kotlin
internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {
    suspend fun getAudience(identifier: String): JsonObject?
    suspend fun warm(generation: Int)
    fun warmAsync(generation: Int)
    fun close()
}
```

The provider treats each audience body as an opaque `JsonObject`. It does not validate `id`, inspect `rules`, or discard unknown keys. Consequently, future schema additions remain available without ingestion changes.

`warm(generation)` reads the already-committed `audiences` topic without triggering a config sync, selects entries marked `prefetch`, and resolves their bodies concurrently through `RemoteConfigManager.blobData<JsonObject>`. Every item is resolved independently. Successfully decoded objects are stored in a `GenerationGuardedCache<Map<String, JsonObject>>`, keyed by the topic item key; missing blobs, malformed JSON, and non-object JSON are omitted without affecting valid siblings. An empty result is a valid cached snapshot.

`getAudience(identifier)` first returns a matching cached object. On a miss, it delegates to `RemoteConfigManager.blobData<JsonObject>` so a cold or non-prefetched lookup still follows the standard self-priming remote-config path. A generation snapshot prevents an object resolved across an identity invalidation from being served. Direct fallback reads do not replace the complete prefetched snapshot with a partial map; subsequent commit warming populates the memory cache.

The provider invalidates its cache on `onConfigInvalidated`, asynchronously re-warms on `onConfigCommitted`, and cancels its owned scope in `close()`.

### Lifecycle wiring

`PurchasesFactory` creates one provider beside the UI-config, workflows, and checkpoints providers. It registers the provider as a remote-config listener and starts a non-networking warm from the initially committed generation.

`PurchasesOrchestrator` owns the provider so its coroutine scope can be closed with the other remote-config providers. No rule evaluator receives it yet.

## Error and concurrency behavior

- A missing `audiences` topic produces an empty in-memory snapshot and never crashes initialization.
- A missing item body is skipped.
- Malformed JSON or a valid non-object JSON body is skipped.
- One bad item cannot invalidate or suppress valid sibling items.
- Lower-generation asynchronous work cannot overwrite a newer cache snapshot or invalidation.
- A lookup whose body resolves while the generation changes must return the matching newer cached value, or `null` when none exists, rather than data from the previous identity.

## Testing

Add focused provider tests that verify:

- `RemoteConfigTopic.Audiences.wireName` is `audiences`.
- A complete opaque audience object is decoded with all keys preserved.
- Warming resolves and caches only `prefetch` entries.
- Warmed lookups return from memory without another manager read.
- Missing, malformed, and non-object items are omitted while valid siblings remain available.
- Empty or absent topics produce an empty snapshot.
- Invalidation clears cached values.
- Generation ordering prevents stale warm or lookup results from being served.

Add a remote-config manager integration test analogous to the iOS PR to show that an `audiences` topic can contain valid and missing prefetched entries, that the valid blob is prefetched, and that either item can be queried without the missing item invalidating the topic.

Run the focused audiences and remote-config test classes, followed by the complete `:purchases:testDefaultsDebugUnitTest` suite and Detekt for the changed module.
