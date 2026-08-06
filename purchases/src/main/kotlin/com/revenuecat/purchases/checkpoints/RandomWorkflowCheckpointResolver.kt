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
 * the workflows topic, except two hardcoded identifiers that simulate the unknown-checkpoint and error outcomes
 * the future checkpoints config topic will produce.
 */
internal class RandomWorkflowCheckpointResolver(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val getOfferings: suspend () -> Offerings,
) : CheckpointWorkflowResolver {

    override suspend fun resolve(identifier: String, customProperties: Map<String, Any>): CheckpointResolution =
        when (identifier) {
            SIMULATED_ERROR_CHECKPOINT_ID -> {
                val error = PurchasesError(
                    PurchasesErrorCode.ConfigurationError,
                    "Simulated error: checkpoint workflow not presentable.",
                )
                errorLog(error)
                throw PurchasesException(error)
            }
            SIMULATED_UNKNOWN_CHECKPOINT_ID ->
                CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT)
            else -> resolveRandomWorkflow(identifier)
        }

    @Suppress("ReturnCount")
    private suspend fun resolveRandomWorkflow(identifier: String): CheckpointResolution {
        val workflowManager = workflowManager
        val uiConfigProvider = uiConfigProvider
        if (workflowManager == null || uiConfigProvider == null) {
            return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.DISABLED)
        }
        // Only workflows tied to an offering are presentable for now: the presentation requires one.
        val (workflowId, offeringId) = workflowManager.offeringIdByWorkflowId().entries
            .filter { it.value != null }
            .randomOrNull()
            ?: return configurationUnavailable("No presentable workflows available for checkpoint '$identifier'.")
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
            "Checkpoint '$identifier' resolved to random workflow '$workflowId' " +
                "(offering: ${offering.identifier})"
        }
        return CheckpointResolution.Workflow(workflow, uiConfig, offering)
    }

    private fun configurationUnavailable(message: String): CheckpointResolution.NoAction {
        errorLog { message }
        return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    private companion object {
        const val SIMULATED_UNKNOWN_CHECKPOINT_ID = "unknown_checkpoint"
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
    }
}
