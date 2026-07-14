@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
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
 * On top of the on-demand suspend reads it keeps a small in-memory cache of **parsed** workflows so a paywall
 * render can read one without a disk + JSON hop: [getWorkflow] and [workflowIdForOfferingId] are **memory-first**
 * — a cache hit returns synchronously (the suspend fn never actually suspends, so it resumes on the caller's
 * thread with no dispatch), and only a miss touches [RemoteConfigManager]. Only workflows worth holding are
 * cached: an item flagged `prefetch`, or the one associated with the current offering (its `offering_identifier`
 * equals [currentOfferingIdProvider]'s value). The full `offering_identifier -> workflowId` map (metadata only)
 * is cached for every offering. Both are re-warmed on every config commit and dropped on identity change /
 * disable, guarded by [RemoteConfigManager.configGeneration] (store-if-newer) so a slower disk warm never
 * clobbers a fresher network commit.
 */
@Suppress("TooManyFunctions")
internal class WorkflowsConfigProvider(
    private val manager: RemoteConfigManager,
    private val currentOfferingIdProvider: () -> String? = { null },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private class Cached(
        val workflows: Map<String, PublishedWorkflow>,
        val offeringToWorkflowId: Map<String, String>,
    )

    private val cacheLock = Any()

    @Volatile
    private var cache: Cached? = null

    // Newest generation acted on (store or invalidation); an update applies only if its generation is >= this,
    // so an out-of-order low-generation warm can't repopulate stale data. -1 = nothing seen yet.
    private var lastGeneration: Int = -1

    /**
     * Whether the in-memory cache already holds what the current offering's paywall needs, so the offerings
     * readiness gate can deliver synchronously. True when the cache is populated and either there is no current
     * offering, the current offering has no workflow mapping (legacy/embedded paywall), or its workflow is parsed.
     */
    fun isWarmForCurrentOffering(): Boolean {
        val cached = cache ?: return false
        val workflowId = currentOfferingIdProvider()?.let { cached.offeringToWorkflowId[it] }
        return workflowId == null || cached.workflows.containsKey(workflowId)
    }

    suspend fun workflowIdForOfferingId(offeringId: String): String? {
        cache?.let { return it.offeringToWorkflowId[offeringId] }
        val topic = manager.topic(RemoteConfigTopic.Workflows)
        verboseLog { "workflows topic ${if (topic == null) "is absent" else "has ${topic.size} item(s)"}" }
        val matches = topic
            ?.entries
            ?.filter { (_, item) -> item.metadata.stringOrNull(KEY_OFFERING_IDENTIFIER) == offeringId }
            .orEmpty()
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
        return workflowId
    }

    /**
     * Resolves [workflowId] into a [PublishedWorkflow], or `null` when the item is unknown, its body can be
     * neither read nor downloaded, or the body fails to parse. Memory-first: a cached (parsed) workflow returns
     * synchronously; only a miss reads through [RemoteConfigManager] (disk/network + JSON parse).
     */
    suspend fun getWorkflow(workflowId: String): PublishedWorkflow? {
        cache?.workflows?.get(workflowId)?.let { return it }
        return resolveWorkflow(workflowId)
    }

    /** Reads + parses a workflow body straight from the config layer, bypassing the in-memory cache. */
    private suspend fun resolveWorkflow(workflowId: String): PublishedWorkflow? {
        val body = manager.blobData(RemoteConfigTopic.Workflows, workflowId) { it } ?: run {
            errorLog { "Workflow '$workflowId' is unavailable from remote config." }
            return null
        }
        return try {
            val workflow = WorkflowJsonParser.parsePublishedWorkflow(body.decodeToString())
            debugLog { "Parsed workflow '$workflowId' (${workflow.steps.size} step(s))" }
            workflow
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to parse workflow '$workflowId' body." }
            null
        }
    }

    /**
     * Forces the `workflows` topic to be synced (or confirms it already is) **and** waits for its
     * `prefetch`-marked workflow blobs to finish caching, discarding the result. Used by
     * [WorkflowManager.onPaywallConfigReady] so `OfferingsManager` can gate its `onSuccess` callback on
     * workflow data being ready, the way it used to gate on the old `getWorkflowsList` fetch — cheap on a warm
     * cache since [RemoteConfigManager.awaitTopicAndPrefetchBlobsReady] returns immediately once the topic is
     * committed and its prefetch blobs are cached.
     */
    suspend fun awaitReady() {
        manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)
    }

    /**
     * Best-effort populate of the in-memory caches from already-committed config, tagged with [generation]. No-op
     * (no `/v1/config` sync) when the `workflows` topic isn't committed yet, so a cold-disk init warm never
     * triggers a network config fetch. Caches only eligible workflows (prefetch or current offering); the
     * offering->workflowId map covers every offering.
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
        // Resolve from committed config (not the cache we're rebuilding).
        val workflows = coroutineScope {
            eligibleIds.distinct()
                .map { id -> async { id to resolveWorkflow(id) } }
                .awaitAll()
        }.mapNotNull { (id, workflow) -> workflow?.let { id to it } }.toMap()
        verboseLog {
            "Warmed workflows cache: ${workflows.size} eligible workflow(s), " +
                "${offeringToWorkflowId.size} offering mapping(s)."
        }
        store(generation, Cached(workflows, offeringToWorkflowId))
    }

    /** Warms at the current config generation; used by the offerings readiness gate. */
    suspend fun warm() = warm(manager.configGeneration)

    /** Fire-and-forget [warm] on this provider's own scope; used for the cold-start init warm. */
    fun warmAsync(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigCommitted(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigInvalidated(generation: Int) {
        synchronized(cacheLock) {
            if (generation >= lastGeneration) {
                lastGeneration = generation
                cache = null
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun isWarmAtOrAbove(generation: Int): Boolean = synchronized(cacheLock) {
        lastGeneration >= generation && isWarmForCurrentOffering()
    }

    private fun store(generation: Int, value: Cached) {
        synchronized(cacheLock) {
            if (generation >= lastGeneration) {
                lastGeneration = generation
                cache = value
            }
        }
    }

    private companion object {
        private const val KEY_OFFERING_IDENTIFIER = "offering_identifier"

        private fun JsonObject.stringOrNull(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
