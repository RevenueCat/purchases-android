@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.workflows.WorkflowManager

/**
 * PoC [CheckpointWorkflowResolver]: every checkpoint matches and resolves to a workflow picked at random from
 * the workflows topic, except two hardcoded identifiers that simulate the no-match and error outcomes the
 * future checkpoints config topic will produce.
 */
internal class RandomWorkflowCheckpointResolver(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val getOfferings: suspend () -> Offerings,
) : CheckpointWorkflowResolver {

    override suspend fun resolve(checkpoint: CheckpointInfo): CheckpointWorkflowResolution =
        when (checkpoint.identifier) {
            SIMULATED_ERROR_CHECKPOINT_ID -> CheckpointWorkflowResolution.Failed(
                PurchasesError(
                    PurchasesErrorCode.ConfigurationError,
                    "Simulated error: checkpoint workflow not presentable.",
                ),
            )
            SIMULATED_NO_MATCH_CHECKPOINT_ID ->
                CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.NO_MATCH)
            else -> resolveRandomWorkflow(checkpoint)
        }

    @Suppress("ReturnCount")
    private suspend fun resolveRandomWorkflow(checkpoint: CheckpointInfo): CheckpointWorkflowResolution {
        val workflowManager = workflowManager
        val uiConfigProvider = uiConfigProvider
        if (workflowManager == null || uiConfigProvider == null) {
            return CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.DISABLED)
        }
        // Only workflows tied to an offering are presentable for now: the presentation requires one.
        val (workflowId, offeringId) = workflowManager.offeringIdByWorkflowId().entries
            .filter { it.value != null }
            .randomOrNull()
            ?: return configurationUnavailable(
                "No presentable workflows available for checkpoint '${checkpoint.identifier}'.",
            )
        val workflow = try {
            workflowManager.getWorkflow(workflowId)
        } catch (e: PurchasesException) {
            return configurationUnavailable("Workflow '$workflowId' could not be loaded: ${e.error}")
        }
        val uiConfig = uiConfigProvider.getUiConfig()
            ?: return configurationUnavailable("UI config is unavailable for workflow '$workflowId'.")
        val offerings = try {
            getOfferings()
        } catch (e: PurchasesException) {
            return configurationUnavailable("Offerings could not be fetched for workflow '$workflowId': ${e.error}")
        }
        val offering = offerings.all[offeringId]
            ?: return configurationUnavailable(
                "Offering '$offeringId' referenced by workflow '$workflowId' was not found in offerings.",
            )
        debugLog {
            "Checkpoint '${checkpoint.identifier}' resolved to random workflow '$workflowId' " +
                "(offering: ${offering.identifier})"
        }
        return CheckpointWorkflowResolution.Matched(
            CheckpointWorkflowPresentation(checkpoint, workflow, uiConfig, offering),
        )
    }

    private fun configurationUnavailable(message: String): CheckpointWorkflowResolution.NoMatch {
        errorLog { message }
        return CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    private companion object {
        const val SIMULATED_NO_MATCH_CHECKPOINT_ID = "unknown_checkpoint"
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
    }
}
