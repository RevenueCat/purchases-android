@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The topic-specific front door for workflows — the config-endpoint replacement for `WorkflowManager`'s read
 * path. It knows only the `workflows` topic, that an item's `offering_identifier` lives in its inline
 * content, and how to parse a [PublishedWorkflow]. Everything else — reading metadata, waiting on an in-flight
 * sync, resolving inline vs `blob_ref`, downloading and reading the body — it delegates to [RemoteConfigManager]
 * through `topic()` / `blobData()`. It never sees the blob store, the fetcher, or the disk cache.
 *
 * On top of the on-demand suspend reads it keeps a small in-memory cache so a paywall render can read a workflow
 * without a disk + JSON hop: [getWorkflow] and [workflowIdForOfferingId] are **memory-first** — a cache hit
 * returns synchronously (the suspend fn never actually suspends, so it resumes on the caller's thread with no
 * dispatch), and only a miss touches [RemoteConfigManager]. To keep memory low the cache holds the **raw workflow
 * body bytes**, not the decoded [PublishedWorkflow] graph (which retains an entire paywall component tree +
 * localizations per screen); the decode is deferred behind a [Lazy] and runs on first [getWorkflow] access, on
 * the caller's (main) thread, and is retained thereafter — mirroring `Offering.PaywallComponents`. Only workflows
 * worth holding are cached: an item flagged `prefetch`, or the one associated with the current offering (its
 * `offering_identifier` equals [currentOfferingIdProvider]'s value). The full `offering_identifier -> workflowId`
 * map (metadata only) is cached for every offering. Both are re-warmed on every config commit and dropped on
 * identity change / disable, guarded by [RemoteConfigManager.configGeneration] (store-if-newer) so a slower disk
 * warm never clobbers a fresher network commit.
 */
@Suppress("TooManyFunctions")
internal class WorkflowsConfigProvider(
    private val manager: RemoteConfigManager,
    private val currentOfferingIdProvider: () -> String? = { null },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private class Cached(
        // Raw body bytes per workflow, decoded lazily (on first access, on the caller's thread) and retained.
        val workflows: Map<String, Lazy<PublishedWorkflow?>>,
        val offeringToWorkflowId: Map<String, String>,
    )

    private val cache = GenerationGuardedCache<Cached>()

    /**
     * Whether the in-memory cache already holds what the current offering's paywall needs, so the offerings
     * readiness gate can deliver synchronously. True when the cache is populated and either there is no current
     * offering, the current offering has no workflow mapping (legacy/embedded paywall), or its body is in memory
     * (ready to decode synchronously on read).
     */
    fun isWarmForCurrentOffering(): Boolean {
        val cached = cache.cached ?: return false
        val workflowId = currentOfferingIdProvider()?.let { cached.offeringToWorkflowId[it] }
        // "Has the body in memory" — the decode is deferred, so this never forces a parse.
        return workflowId == null || cached.workflows.containsKey(workflowId)
    }

    /**
     * Resolves an offering to its workflow through the `/v1/config` workflows topic, distinguishing a genuinely
     * workflowless offering ([WorkflowResolution.NoWorkflow]) from a topic that could not be read at all
     * ([WorkflowResolution.Unresolved], i.e. the endpoint is disabled or a sync failed transiently). Memory-first:
     * a warm cache resolves synchronously; only a miss reads the topic (which may trigger a sync).
     */
    suspend fun resolveWorkflow(offeringId: String): WorkflowResolution {
        cache.cached?.let { cached ->
            return cached.offeringToWorkflowId[offeringId]?.let { WorkflowResolution.Found(it) }
                ?: WorkflowResolution.NoWorkflow
        }
        val generation = manager.configGeneration
        val topic = manager.topic(RemoteConfigTopic.Workflows)
        verboseLog { "workflows topic ${if (topic == null) "is absent" else "has ${topic.size} item(s)"}" }
        // A null topic means it could not be read: the /v1/config endpoint is disabled (4xx kill switch) or a
        // sync failed transiently. Whether this offering has a workflow is unknown, so leave the recovery
        // decision to the caller instead of pretending it is workflowless.
        return if (topic == null) {
            verboseLog { "Workflows topic unavailable while resolving offering '$offeringId'" }
            WorkflowResolution.Unresolved
        } else {
            val matches = topic.entries
                .filter { (_, item) -> item.metadata.stringOrNull(KEY_OFFERING_IDENTIFIER) == offeringId }
            if (matches.size > 1) {
                warnLog { "Duplicate offering_identifier '$offeringId' in workflows topic: ${matches.map { it.key }}" }
            }
            // Last entry wins on duplicates.
            val workflowId = matches.lastOrNull()?.key
            verboseLog {
                if (workflowId != null) {
                    "Resolved offering '$offeringId' to workflow '$workflowId'"
                } else {
                    "No workflow found for offering '$offeringId'"
                }
            }
            // If an identity-change invalidation advanced the generation while the topic was read, the resolved
            // id may belong to the previous user; prefer whatever the (now newer) cache holds instead.
            val resolvedId = if (cache.isCurrent(generation)) {
                workflowId
            } else {
                cache.cached?.offeringToWorkflowId?.get(offeringId)
            }
            resolvedId?.let { WorkflowResolution.Found(it) } ?: WorkflowResolution.NoWorkflow
        }
    }

    /** The resolved workflow id for [offeringId], or `null` when none is mapped or the topic is unavailable. */
    suspend fun workflowIdForOfferingId(offeringId: String): String? =
        (resolveWorkflow(offeringId) as? WorkflowResolution.Found)?.workflowId

    /**
     * Resolves [workflowId] into a [PublishedWorkflow], or `null` when the item is unknown, its body can be
     * neither read nor downloaded, or the body fails to parse. Memory-first: a cached workflow returns
     * synchronously — the body bytes are already in memory, so the [Lazy] decode runs on this (caller) thread
     * without suspending. Only a miss reads through [RemoteConfigManager] (disk/network + JSON parse).
     */
    suspend fun getWorkflow(workflowId: String): PublishedWorkflow? {
        cache.cached?.workflows?.get(workflowId)?.let { return it.value }
        val generation = manager.configGeneration
        val workflow = resolveWorkflowBody(workflowId)
        // If an identity-change invalidation advanced the generation while the body was resolved, it may belong
        // to the previous user; prefer whatever the (now newer) cache holds instead of serving it.
        return when {
            workflow == null -> null
            cache.isCurrent(generation) -> workflow
            else -> cache.cached?.workflows?.get(workflowId)?.value
        }
    }

    /** Reads + parses a workflow body straight from the config layer, bypassing the in-memory cache. */
    private suspend fun resolveWorkflowBody(workflowId: String): PublishedWorkflow? {
        val body = resolveWorkflowBytes(workflowId) ?: return null
        return decodeWorkflow(workflowId, body)
    }

    /** Reads a workflow body's raw bytes from the config layer (disk/network); no decode. */
    private suspend fun resolveWorkflowBytes(workflowId: String): ByteArray? =
        manager.blobData(RemoteConfigTopic.Workflows, workflowId) { it }
            ?: run {
                errorLog { "Workflow '$workflowId' is unavailable from remote config." }
                null
            }

    /** Parses raw workflow body bytes into a [PublishedWorkflow], or `null` on a parse failure. */
    private fun decodeWorkflow(workflowId: String, body: ByteArray): PublishedWorkflow? =
        try {
            val workflow = WorkflowJsonParser.parsePublishedWorkflow(body.decodeToString())
            debugLog { "Parsed workflow '$workflowId' (${workflow.steps.size} step(s))" }
            workflow
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to parse workflow '$workflowId' body." }
            null
        }

    /**
     * Forces the `workflows` topic to be synced (or confirms it already is) **and** waits for its
     * `prefetch`-marked workflow blobs to finish caching, discarding the result. Called by the no-arg [warm]
     * (the offerings readiness gate path) so `OfferingsManager` can gate its `onSuccess` callback on workflow
     * data being ready, the way it used to gate on the old `getWorkflowsList` fetch — cheap on a warm cache
     * since [RemoteConfigManager.awaitTopicAndPrefetchBlobsReady] returns immediately once the topic is
     * committed and its prefetch blobs are cached. Unlike [warm]`(generation)`, this **can** trigger a
     * `/v1/config` sync on a miss, which is exactly why the gate needs it.
     */
    suspend fun awaitReady() {
        manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)
    }

    /**
     * Best-effort populate of the in-memory caches from already-committed config, tagged with [generation]. No-op
     * (no `/v1/config` sync) when the `workflows` topic isn't committed yet, so a cold-disk init warm never
     * triggers a network config fetch. Caches only eligible workflows (prefetch or current offering); the
     * offering->workflowId map covers every offering.
     *
     * Only the **raw body bytes** are pre-loaded here (the expensive IO/network hop, on this warm's dispatcher);
     * the [PublishedWorkflow] decode is deferred behind a [Lazy] and runs on first [getWorkflow] access, so
     * warming never retains a decoded workflow graph. An eligible workflow is cached iff its bytes resolve;
     * whether those bytes later parse is decided lazily on read (a bad body reads back as `null`, not a warm-time
     * exclusion).
     */
    suspend fun warm(generation: Int) {
        // Already warm for this (or a newer) generation and the current offering — nothing to do.
        if (isWarmAtOrAbove(generation)) return
        val topic = manager.committedTopicOrNull(RemoteConfigTopic.Workflows) ?: return
        val offeringToWorkflowId = LinkedHashMap<String, String>()
        val currentOfferingId = currentOfferingIdProvider()
        val eligibleIds = mutableListOf<String>()
        topic.entries.forEach { (workflowId, item) ->
            val offeringId = item.metadata.stringOrNull(KEY_OFFERING_IDENTIFIER)
            if (offeringId != null) {
                // Last entry wins on duplicates, matching workflowIdForOfferingId.
                offeringToWorkflowId[offeringId] = workflowId
            }
            if (item.prefetch || (offeringId != null && offeringId == currentOfferingId)) {
                eligibleIds += workflowId
            }
        }
        // Pre-load bytes from committed config (not the cache we're rebuilding); defer the decode.
        val workflows = coroutineScope {
            eligibleIds.distinct()
                .map { id -> async { id to resolveWorkflowBytes(id) } }
                .awaitAll()
        }.mapNotNull { (id, bytes) ->
            bytes?.let { id to lazy(LazyThreadSafetyMode.SYNCHRONIZED) { decodeWorkflow(id, it) } }
        }.toMap()
        verboseLog {
            "Warmed workflows cache: ${workflows.size} eligible workflow(s), " +
                "${offeringToWorkflowId.size} offering mapping(s)."
        }
        cache.store(generation, Cached(workflows, offeringToWorkflowId))
    }

    /** Warms at the current config generation; used by the offerings readiness gate. */
    suspend fun warm() {
        // Gate-only: unlike warm(generation) (commit/init callers, which must never fetch), the gate must be
        // able to trigger/await a /v1/config sync when the workflows topic isn't committed yet — the
        // ui_config-first priming in onPaywallConfigReady short-circuits on a warm ui_config cache and never
        // syncs. No-op/fast on an already-committed topic; best-effort (a failed/absent sync leaves the warm
        // below a no-op). configGeneration is read after the sync so it reflects any fresh commit.
        awaitReady()
        warm(manager.configGeneration)
    }

    /** Fire-and-forget [warm] on this provider's own scope; used for the cold-start init warm. */
    fun warmAsync(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigCommitted(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigInvalidated(generation: Int) {
        cache.invalidate(generation)
    }

    fun close() {
        scope.cancel()
    }

    private fun isWarmAtOrAbove(generation: Int): Boolean =
        cache.isAtOrAbove(generation) && isWarmForCurrentOffering()

    private companion object {
        private const val KEY_OFFERING_IDENTIFIER = "offering_identifier"

        private fun JsonObject.stringOrNull(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
