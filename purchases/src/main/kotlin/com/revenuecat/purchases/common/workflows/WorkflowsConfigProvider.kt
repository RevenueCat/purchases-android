@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The topic-specific front door for workflows — the config-endpoint replacement for `WorkflowManager`'s read
 * path. It knows only the `workflows` topic, that an item's `offering_identifier` lives in its inline
 * content, and how to parse a [PublishedWorkflow]. Everything else — reading metadata, waiting on an in-flight
 * sync, resolving inline vs `blob_ref`, downloading and reading the body — it delegates to [RemoteConfigManager]
 * through `topic()` / `blobData()`. It never sees the blob store, the fetcher, or the disk cache.
 */
internal class WorkflowsConfigProvider(
    private val manager: RemoteConfigManager,
) {

    suspend fun workflowIdForOfferingId(offeringId: String): String? {
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
     * neither read nor downloaded, or the body fails to parse.
     */
    suspend fun getWorkflow(workflowId: String): PublishedWorkflow? {
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
     * Forces the `workflows` topic to be synced (or confirms it already is), discarding the result. Used by
     * [WorkflowManager.awaitWorkflowsReady] so `OfferingsManager` can gate its `onSuccess` callback on
     * workflow data being ready, the way it used to gate on the old `getWorkflowsList` fetch — cheap on a warm
     * cache since [RemoteConfigManager.topic] returns immediately once a topic is committed.
     */
    suspend fun awaitReady() {
        // Known gap, planned follow-up: this only waits for the topic metadata commit; the sync's blob
        // prefetch is best-effort and does not block it, so getOfferings can return before prefetch-marked
        // workflow bodies finish downloading (a render that arrives first waits on the in-flight download,
        // or fetches on demand; a failed download falls back at render time). The old getWorkflowsList gate
        // also waited for its prefetch batch. The agreed design: the fetcher's prefetch returns a Job for
        // the batch, RemoteConfigManager keeps the latest handle (awaitPrefetchSettled), and this joins it —
        // prefetch stays owned by the sync; this read path must not trigger fetches itself.
        manager.topic(RemoteConfigTopic.Workflows)
    }

    private companion object {
        private const val KEY_OFFERING_IDENTIFIER = "offering_identifier"

        private fun JsonObject.stringOrNull(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
