package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * In-memory cache for all workflow data: the resolved per-workflow [WorkflowDataResult]s and the
 * workflows list (plus its derived offeringId → workflowId map). It is the single owner of this
 * state so that clearing it on identity transitions wipes everything at once, mirroring how
 * [com.revenuecat.purchases.common.offerings.OfferingsCache] owns the in-memory offerings cache.
 *
 * Why in-memory, like offerings: this layer sits on top of the durable copy that lives on disk in
 * [DeviceCache], for the same reason the offerings cache does — to serve already-fetched and
 * prefetched data synchronously within a session, so opening a paywall reuses a resolved workflow
 * instead of paying another backend/CDN round-trip. Time-based staleness
 * ([isWorkflowsListCacheStale] / [isWorkflowCacheStale]) then decides when a refetch is due.
 *
 * It also owns the disk copy of the workflows list, mirroring how
 * [com.revenuecat.purchases.common.offerings.OfferingsCache] owns the offerings response on disk:
 * [cacheWorkflowsList] persists it, [cachedWorkflowsListResponseFromDisk] restores it on backend
 * failure, and [clearCache] wipes it on identity transitions.
 *
 * It additionally persists per-workflow detail envelopes to disk: [cacheWorkflowDetailEnvelope]
 * writes a single envelope (merging with any already-stored ones),
 * [cachedWorkflowDetailEnvelopesFromDisk] restores the full map after a backend failure, and
 * [clearCache] wipes the envelope store on identity transitions (alongside the list disk cache).
 */
@Suppress("TooManyFunctions")
@OptIn(InternalRevenueCatAPI::class)
internal class WorkflowsCache(
    private val deviceCache: DeviceCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {
    private val cachedWorkflows = mutableMapOf<String, InMemoryCachedObject<WorkflowDataResult>>()
    private val workflowsListCachedObject = InMemoryCachedObject<WorkflowsListResponse>(dateProvider = dateProvider)

    @Volatile
    private var offeringIdToWorkflowIdMap: Map<String, String> = emptyMap()

    // Bumped on every clearCache() (identity transitions). A fetch captures the generation at its
    // start and passes it back on write; a write whose captured generation no longer matches is
    // dropped, so a fetch that began before an identity transition can't repopulate the cleared cache.
    private var cacheGeneration: Int = 0

    @Synchronized
    fun currentGeneration(): Int = cacheGeneration

    // region Workflow detail cache

    @Synchronized
    fun cachedWorkflow(workflowId: String): WorkflowDataResult? =
        cachedWorkflows[workflowId]?.cachedInstance

    @Synchronized
    fun isWorkflowCacheStale(workflowId: String, appInBackground: Boolean): Boolean =
        cachedWorkflows[workflowId]?.lastUpdatedAt?.isCacheStale(appInBackground, dateProvider) ?: true

    @Synchronized
    fun cacheWorkflow(workflowId: String, result: WorkflowDataResult, expectedGeneration: Int) {
        if (expectedGeneration != cacheGeneration) return
        val cached = cachedWorkflows.getOrPut(workflowId) { InMemoryCachedObject(dateProvider = dateProvider) }
        cached.cacheInstance(result)
    }

    /**
     * Clears all resolved workflow detail values from the in-memory cache. Used when a
     * force-refresh is triggered (pull-to-refresh via offerings) so the subsequent prefetch is a
     * guaranteed cache miss and always fetches fresh data from the backend, rather than serving
     * a still-within-TTL cached value. The offeringId map is unaffected.
     */
    @Synchronized
    fun clearWorkflowDetailCaches() {
        cachedWorkflows.clear()
    }

    // endregion Workflow detail cache

    // region Workflows list cache

    @Synchronized
    fun isWorkflowsListCacheStale(appInBackground: Boolean): Boolean =
        workflowsListCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider)

    @Synchronized
    fun invalidateWorkflowsListTimestamp() {
        workflowsListCachedObject.clearCacheTimestamp()
    }

    /**
     * Caches the workflows list in memory and persists it to disk, the same way
     * [com.revenuecat.purchases.common.offerings.OfferingsCache.cacheOfferings] caches offerings.
     *
     * Guarded by [expectedGeneration]: a fetch captures [currentGeneration] at its start and passes
     * it back here. If an identity transition called [clearCache] in between (bumping the generation),
     * the captured value no longer matches and the write is dropped, so an in-flight fetch from the
     * previous user can't repopulate the cleared cache. The guard and the [clearCache] increment share
     * this object's intrinsic lock, so the check-then-write is atomic.
     * [com.revenuecat.purchases.common.offerings.OfferingsCache.cacheOfferings] carries the same guard
     * so both caches stay consistent across an identity change.
     */
    @Synchronized
    fun cacheWorkflowsList(
        response: WorkflowsListResponse,
        offeringIdMap: Map<String, String>,
        expectedGeneration: Int,
    ) {
        if (expectedGeneration != cacheGeneration) return
        // Re-checked under the same lock by cacheWorkflowsListInMemory; redundant on this path (the
        // lock is held throughout) but keeps the guard uniform for its direct callers.
        cacheWorkflowsListInMemory(response, offeringIdMap, expectedGeneration)
        deviceCache.cacheWorkflowsListResponse(
            JsonTools.json.encodeToString(WorkflowsListResponse.serializer(), response),
        )
        pruneWorkflowDetailEnvelopesToList(response.workflows.map { it.id }.toSet())
    }

    /**
     * Populates only the in-memory list cache and offeringId map, leaving disk untouched. Used to
     * restore in-memory state from the disk copy after a backend failure, where the disk already
     * holds this exact payload so rewriting it would be wasted work.
     */
    @Synchronized
    fun cacheWorkflowsListInMemory(
        response: WorkflowsListResponse,
        offeringIdMap: Map<String, String>,
        expectedGeneration: Int,
    ) {
        if (expectedGeneration != cacheGeneration) return
        workflowsListCachedObject.cacheInstance(response)
        offeringIdToWorkflowIdMap = offeringIdMap
    }

    /**
     * Reads the last workflows list persisted by [cacheWorkflowsList], or null when nothing is
     * cached or the payload can't be parsed (the parse failure is logged). Used to recover the
     * list after a backend failure, mirroring
     * [com.revenuecat.purchases.common.offerings.OfferingsCache.cachedOfferingsResponse].
     */
    fun cachedWorkflowsListResponseFromDisk(): WorkflowsListResponse? =
        deviceCache.getWorkflowsListResponseCache()?.let { cached ->
            runCatching { WorkflowJsonParser.parseWorkflowsListResponse(cached) }
                .onFailure { errorLog(it) { "Failed to restore workflows list from disk cache" } }
                .getOrNull()
        }

    fun workflowIdForOfferingId(offeringId: String): String? =
        offeringIdToWorkflowIdMap[offeringId]

    // endregion Workflows list cache

    private companion object {
        private val envelopesSerializer = MapSerializer(String.serializer(), WorkflowDetailResponse.serializer())
    }

    // region Workflow detail envelopes disk cache

    /**
     * Persists [envelope] under [workflowId] in the on-disk envelope map, merging with whatever is
     * already there. Called only from the prefetch path after a successful resolve, so a persisted
     * envelope is always one we could render offline. Mirrors how [cacheWorkflowsList] writes the
     * list to disk.
     *
     * Reads, merges, and rewrites the whole on-disk map; the prefetch set is small so this is cheap.
     * If the existing payload can't be parsed it is treated as empty, so an unreadable map is
     * replaced rather than preserved.
     */
    @Synchronized
    fun cacheWorkflowDetailEnvelope(workflowId: String, envelope: WorkflowDetailResponse, expectedGeneration: Int) {
        if (expectedGeneration != cacheGeneration) return
        val current = cachedWorkflowDetailEnvelopesFromDisk().orEmpty().toMutableMap()
        current[workflowId] = envelope
        persistWorkflowDetailEnvelopes(current)
    }

    /**
     * Reads the persisted envelope map, or null when nothing is cached or the payload can't be
     * parsed (the parse failure is logged). Used to recover envelopes after a backend failure,
     * mirroring [cachedWorkflowsListResponseFromDisk].
     */
    fun cachedWorkflowDetailEnvelopesFromDisk(): Map<String, WorkflowDetailResponse>? =
        deviceCache.getWorkflowDetailEnvelopesCache()?.let { cached ->
            runCatching { WorkflowJsonParser.parseWorkflowDetailEnvelopes(cached) }
                .onFailure { errorLog(it) { "Failed to restore workflow detail envelopes from disk cache" } }
                .getOrNull()
        }

    /**
     * Drops persisted envelopes whose workflowId is no longer in the latest list. Because only
     * successfully-prefetched workflows are ever written here (see [cacheWorkflowDetailEnvelope]),
     * this prune removes stale prefetch envelopes for workflows the backend has stopped sending.
     * It is the keyed-map equivalent of how [com.revenuecat.purchases.common.offerings.OfferingsCache]
     * stays bounded by wholesale-replacing its single response blob.
     */
    @Synchronized
    private fun pruneWorkflowDetailEnvelopesToList(workflowIds: Set<String>) {
        val current = cachedWorkflowDetailEnvelopesFromDisk() ?: return
        val pruned = current.filterKeys { it in workflowIds }
        if (pruned.size != current.size) {
            persistWorkflowDetailEnvelopes(pruned)
        }
    }

    private fun persistWorkflowDetailEnvelopes(envelopes: Map<String, WorkflowDetailResponse>) {
        deviceCache.cacheWorkflowDetailEnvelopes(
            JsonTools.json.encodeToString(envelopesSerializer, envelopes),
        )
    }

    // endregion Workflow detail envelopes disk cache

    @Synchronized
    fun clearCache() {
        cacheGeneration++
        cachedWorkflows.clear()
        workflowsListCachedObject.clearCache()
        offeringIdToWorkflowIdMap = emptyMap()
        deviceCache.clearWorkflowsListResponseCache()
        deviceCache.clearWorkflowDetailEnvelopesCache()
    }
}
