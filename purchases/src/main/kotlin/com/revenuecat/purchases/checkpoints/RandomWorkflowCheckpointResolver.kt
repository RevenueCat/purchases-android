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
 * PoC [CheckpointWorkflowResolver]: a hardcoded allowlist stands in for the future checkpoints config topic, and
 * a matched checkpoint resolves to a workflow picked at random from the workflows topic.
 */
internal class RandomWorkflowCheckpointResolver(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val cachedOfferingsProvider: () -> Offerings?,
) : CheckpointWorkflowResolver {

    override suspend fun resolve(checkpoint: CheckpointInfo): CheckpointWorkflowResolution =
        when (checkpoint.identifier) {
            SIMULATED_ERROR_CHECKPOINT_ID -> CheckpointWorkflowResolution.Failed(
                PurchasesError(
                    PurchasesErrorCode.ConfigurationError,
                    "Simulated error: checkpoint workflow not presentable.",
                ),
            )
            in WORKFLOW_CHECKPOINT_ALLOWLIST -> resolveRandomWorkflow(checkpoint)
            else -> CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.NO_MATCH)
        }

    @Suppress("ReturnCount")
    private suspend fun resolveRandomWorkflow(checkpoint: CheckpointInfo): CheckpointWorkflowResolution {
        val workflowManager = workflowManager
        val uiConfigProvider = uiConfigProvider
        if (workflowManager == null || uiConfigProvider == null) {
            return CheckpointWorkflowResolution.NoMatch(CheckpointResult.NoAction.Reason.DISABLED)
        }
        val (workflowId, offeringId) = workflowManager.availableWorkflows().entries.randomOrNull()
            ?: return configurationUnavailable("No workflows available for checkpoint '${checkpoint.identifier}'.")
        val workflow = try {
            workflowManager.getWorkflow(workflowId)
        } catch (e: PurchasesException) {
            return configurationUnavailable("Workflow '$workflowId' could not be loaded: ${e.error}")
        }
        val uiConfig = uiConfigProvider.getUiConfig()
            ?: return configurationUnavailable("UI config is unavailable for workflow '$workflowId'.")
        val offering = offeringId?.let { cachedOfferingsProvider()?.all?.get(it) }
        debugLog {
            "Checkpoint '${checkpoint.identifier}' resolved to random workflow '$workflowId' " +
                "(offering: ${offering?.identifier})"
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
        val WORKFLOW_CHECKPOINT_ALLOWLIST = setOf("test_checkpoint", "finished_onboarding")
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
    }
}
