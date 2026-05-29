package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale

/**
 * In-memory cache for all workflow data: the resolved per-workflow [WorkflowDataResult]s and the
 * workflows list (plus its derived offeringId → workflowId map). It is the single owner of this
 * state so that clearing it on identity transitions wipes everything at once, mirroring how
 * [com.revenuecat.purchases.common.offerings.OfferingsCache] owns the in-memory offerings cache.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class WorkflowsCache(
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
     * Caches the workflows list unconditionally, the same way
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
    }

    fun workflowIdForOfferingId(offeringId: String): String? =
        offeringIdToWorkflowIdMap[offeringId]

    // endregion Workflows list cache

    @Synchronized
    fun clearCache() {
        cachedWorkflows.clear()
        workflowsListCachedObject.clearCache()
        offeringIdToWorkflowIdMap = emptyMap()
    }
}
