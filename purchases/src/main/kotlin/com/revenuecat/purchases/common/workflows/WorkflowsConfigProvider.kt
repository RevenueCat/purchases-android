@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.UiConfigProvider
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
    private val uiConfigProvider: UiConfigProvider = UiConfigProvider(manager),
) {

    suspend fun workflowIdForOfferingId(offeringId: String): String? {
        val topic = manager.topic(RemoteConfigTopic.Workflows)
        debugLog { "workflows topic ${if (topic == null) "is absent" else "has ${topic.size} item(s)"}" }
        val workflowId = topic
            ?.entries
            ?.firstOrNull { (_, item) -> item.metadata.stringOrNull(KEY_OFFERING_IDENTIFIER) == offeringId }
            ?.key
        debugLog {
            if (workflowId != null) {
                "Resolved offering '$offeringId' to workflow '$workflowId'"
            } else {
                "No workflow found for offering '$offeringId'"
            }
        }
        return workflowId
    }

    /**
     * Resolves [workflowId] into a [WorkflowDataResult], or `null` when the item is unknown, its body can be
     * neither read nor downloaded, or the body fails to parse.
     */
    suspend fun getWorkflow(workflowId: String): WorkflowDataResult? {
        val body = manager.blobData(RemoteConfigTopic.Workflows, workflowId) { it } ?: run {
            errorLog { "Workflow '$workflowId' is unavailable from remote config." }
            return null
        }
        return try {
            // ui_config is its own topic (WFL-374), no longer embedded in the workflow body; the parsed
            // uiConfig defaults to an empty UiConfig() until it's resolved and swapped in here.
            val workflow = WorkflowJsonParser.parsePublishedWorkflow(body.decodeToString())
                .copy(uiConfig = uiConfigProvider.getUiConfig())
            debugLog { "Parsed workflow '$workflowId' (${workflow.steps.size} step(s))" }
            // enrolled_variants is out of scope for this spike; it does not fit the topic-dedup model and is
            // being designed separately.
            WorkflowDataResult(workflow = workflow, enrolledVariants = null)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to parse workflow '$workflowId' body." }
            null
        }
    }

    private companion object {
        private const val KEY_OFFERING_IDENTIFIER = "offering_identifier"

        private fun JsonObject.stringOrNull(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
