@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.checkpoints.CheckpointRule
import com.revenuecat.purchases.common.checkpoints.CheckpointRulesResolution
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.common.workflows.WorkflowManager

/**
 * Resolves a checkpoint through the `checkpoint_rules` topic: the checkpoint's rules are read from remote
 * config, and the first one that resolves to a presentable workflow wins. Rules arrive ordered, and walking them
 * in order is the placeholder for the audience evaluation that will eventually pick the first rule whose
 * `audience_id` matches this customer — until then the order the dashboard published is the only signal.
 *
 * A rule is skipped when its workflow can't be served (no offering configured for it, that offering absent from
 * the fetched offerings, or its body unavailable), mirroring the "unservable outcome → next rule" rule the
 * evaluator will keep. The shared reads a presentation needs — `ui_config` and offerings — happen once, outside
 * the walk, since a failure there is not specific to any rule.
 *
 * [SIMULATED_ERROR_CHECKPOINT_ID] is the one piece of PoC scaffolding left: it is the only way for the tester
 * apps to exercise the throw path, since nothing in the config-driven path throws.
 */
internal class CheckpointWorkflowResolverImpl(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val checkpointsConfigProvider: CheckpointsConfigProvider?,
    private val getOfferings: suspend () -> Offerings,
) : CheckpointWorkflowResolver {

    override suspend fun resolve(identifier: String, customProperties: Map<String, Any>): CheckpointResolution {
        if (identifier == SIMULATED_ERROR_CHECKPOINT_ID) {
            val error = PurchasesError(
                PurchasesErrorCode.ConfigurationError,
                "Simulated error: checkpoint workflow not presentable.",
            )
            errorLog(error)
            throw PurchasesException(error)
        }
        return resolveConfiguredWorkflow(identifier)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveConfiguredWorkflow(identifier: String): CheckpointResolution {
        if (workflowManager == null || uiConfigProvider == null || checkpointsConfigProvider == null) {
            return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.DISABLED)
        }
        val checkpoint = when (val resolution = checkpointsConfigProvider.resolveCheckpoint(identifier)) {
            is CheckpointRulesResolution.Found -> resolution.checkpoint
            CheckpointRulesResolution.NotConfigured -> return unknownCheckpoint(identifier)
            CheckpointRulesResolution.Disabled ->
                return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.DISABLED)
            CheckpointRulesResolution.Unavailable ->
                return configurationUnavailable("The rules for checkpoint '$identifier' could not be read.")
        }
        if (checkpoint.rules.isEmpty()) return noMatch(identifier)

        // A rule whose workflow has no offering can never be served, so filtering on the (memory-first) workflow
        // index first keeps an entirely unservable checkpoint from triggering an offerings fetch.
        val offeringIdByWorkflowId = workflowManager.offeringIdByWorkflowId()
        val candidates = checkpoint.rules.mapNotNull { rule ->
            val offeringId = offeringIdByWorkflowId[rule.workflowId]
            if (offeringId == null) {
                logSkippedRule(rule, "no offering ID is configured for it")
                null
            } else {
                rule to offeringId
            }
        }
        if (candidates.isEmpty()) {
            return configurationUnavailable("No rule for checkpoint '$identifier' points at a servable workflow.")
        }
        val uiConfig = uiConfigProvider.getUiConfig()
            ?: return configurationUnavailable("UI config is unavailable for checkpoint '$identifier'.")
        val offerings = try {
            getOfferings()
        } catch (e: PurchasesException) {
            return configurationUnavailable(
                "Offerings could not be fetched for checkpoint '$identifier': ${e.error}",
            )
        }
        return candidates.firstNotNullOfOrNull { (rule, offeringId) ->
            resolveRule(workflowManager, rule, offeringId, offerings, uiConfig)
        } ?: configurationUnavailable("No rule for checkpoint '$identifier' resolved to a presentable workflow.")
    }

    @Suppress("ReturnCount")
    private suspend fun resolveRule(
        workflowManager: WorkflowManager,
        rule: CheckpointRule,
        offeringId: String,
        offerings: Offerings,
        uiConfig: UiConfig,
    ): CheckpointResolution.Workflow? {
        val offering: Offering = offerings.all[offeringId]
            ?: run {
                logSkippedRule(rule, "offering '$offeringId' was not found in offerings")
                return null
            }
        val workflow = try {
            workflowManager.getWorkflow(rule.workflowId)
        } catch (e: PurchasesException) {
            logSkippedRule(rule, "it could not be loaded: ${e.error}")
            return null
        }
        debugLog {
            "Checkpoint resolved to workflow '${rule.workflowId}' (offering: ${offering.identifier})"
        }
        return CheckpointResolution.Workflow(workflow, uiConfig, offering)
    }

    private fun logSkippedRule(rule: CheckpointRule, reason: String) {
        warnLog { "Skipping checkpoint rule for workflow '${rule.workflowId}': $reason." }
    }

    private fun configurationUnavailable(message: String): CheckpointResolution.NoAction {
        errorLog { message }
        return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    private fun unknownCheckpoint(identifier: String): CheckpointResolution.NoAction {
        errorLog { "Checkpoint '$identifier' is not configured in the dashboard." }
        return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT)
    }

    private fun noMatch(identifier: String): CheckpointResolution.NoAction {
        debugLog { "Checkpoint '$identifier' is configured, but has no rules." }
        return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH)
    }

    private companion object {
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
    }
}
