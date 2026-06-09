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

    // region Workflow detail cache

    @Synchronized
    fun cachedWorkflow(workflowId: String): WorkflowDataResult? =
        cachedWorkflows[workflowId]?.cachedInstance

    @Synchronized
    fun isWorkflowCacheStale(workflowId: String, appInBackground: Boolean): Boolean =
        cachedWorkflows[workflowId]?.lastUpdatedAt?.isCacheStale(appInBackground, dateProvider) ?: true

    @Synchronized
    fun cacheWorkflow(workflowId: String, result: WorkflowDataResult) {
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
     * This means workflows share the same accepted appUserID limitation as offerings: a fetch that
     * is in flight during an identity transition can complete *after* [clearCache] and repopulate
     * the cleared cache with the previous user's list (last-write-wins). It is not guarded here, so
     * it self-heals on the next fetch, exactly as the offerings cache does. If we ever decide to
     * close that window, it should be done consistently for both caches rather than only here.
     */
    @Synchronized
    fun cacheWorkflowsList(response: WorkflowsListResponse, offeringIdMap: Map<String, String>) {
        cacheWorkflowsListInMemory(response, offeringIdMap)
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
    fun cacheWorkflowsListInMemory(response: WorkflowsListResponse, offeringIdMap: Map<String, String>) {
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
    fun cacheWorkflowDetailEnvelope(workflowId: String, envelope: WorkflowDetailResponse) {
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
     * Returns the persisted envelope for [workflowId], or null when the key is absent or
     * the on-disk map can't be read. Convenience wrapper over [cachedWorkflowDetailEnvelopesFromDisk].
     */
    fun cachedWorkflowDetailEnvelopeFromDisk(workflowId: String): WorkflowDetailResponse? =
        cachedWorkflowDetailEnvelopesFromDisk()?.get(workflowId)

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
        cachedWorkflows.clear()
        workflowsListCachedObject.clearCache()
        offeringIdToWorkflowIdMap = emptyMap()
        deviceCache.clearWorkflowsListResponseCache()
        deviceCache.clearWorkflowDetailEnvelopesCache()
    }
}
