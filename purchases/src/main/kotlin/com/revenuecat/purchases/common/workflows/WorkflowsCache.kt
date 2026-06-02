package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.errorLog

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
 */
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

    // endregion Workflow detail cache

    // region Workflows list cache

    @Synchronized
    fun isWorkflowsListCacheStale(appInBackground: Boolean): Boolean =
        workflowsListCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider)

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
        workflowsListCachedObject.cacheInstance(response)
        offeringIdToWorkflowIdMap = offeringIdMap
        deviceCache.cacheWorkflowsListResponse(
            JsonTools.json.encodeToString(WorkflowsListResponse.serializer(), response),
        )
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

    @Synchronized
    fun clearCache() {
        cachedWorkflows.clear()
        workflowsListCachedObject.clearCache()
        offeringIdToWorkflowIdMap = emptyMap()
        deviceCache.clearWorkflowsListResponseCache()
    }
}
