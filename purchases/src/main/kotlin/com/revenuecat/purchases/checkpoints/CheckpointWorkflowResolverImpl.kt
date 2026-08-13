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
import com.revenuecat.purchases.common.localrules.LocalRule
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.common.workflows.WorkflowManager

/**
 * Resolves a checkpoint through the `checkpoint_rules` topic: the checkpoint's rules are read from remote config
 * and evaluated in order against locally collected dimensions, and the first rule whose audience matches wins.
 *
 * The winner is final. If its workflow turns out to be unservable — no offering configured for it, that offering
 * absent from the fetched offerings, or its body unavailable — the checkpoint resolves to
 * [CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE] rather than falling through to a rule the
 * customer was not the first choice for.
 *
 * Audience predicates are not served yet, so every rule currently carries [PLACEHOLDER_AUDIENCE_PREDICATE] and the
 * published order remains the effective signal; see [AudienceRule].
 *
 * [SIMULATED_ERROR_CHECKPOINT_ID] is the one piece of PoC scaffolding left: it is the only way for the tester
 * apps to exercise the throw path, since nothing in the config-driven path throws.
 */
internal class CheckpointWorkflowResolverImpl(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val checkpointsConfigProvider: CheckpointsConfigProvider?,
    private val localRulesEvaluator: LocalRulesEvaluator,
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
        val matchResult = localRulesEvaluator.match(checkpoint.rules.map { rule -> AudienceRule(rule) })
        // An audience the SDK failed to evaluate is not the same answer as an audience the customer is outside of,
        // so it can't report NO_MATCH.
        val rule = matchResult.getOrElse { error ->
            return configurationUnavailable(
                "The audiences for checkpoint '$identifier' could not be evaluated: ${error.message}",
            )
        }?.checkpointRule ?: return noMatch(identifier)

        // Resolved before ui_config and offerings so an unservable winner doesn't also trigger an offerings fetch.
        val offeringId = workflowManager.offeringIdByWorkflowId()[rule.workflowId]
            ?: return unservableRule(rule, "no offering is mapped to it in the workflows topic")
        val uiConfig = uiConfigProvider.getUiConfig()
            ?: return configurationUnavailable("UI config is unavailable for checkpoint '$identifier'.")
        val offerings = try {
            getOfferings()
        } catch (e: PurchasesException) {
            return configurationUnavailable(
                "Offerings could not be fetched for checkpoint '$identifier': ${e.error}",
            )
        }
        return resolveRule(workflowManager, rule, offeringId, offerings, uiConfig)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveRule(
        workflowManager: WorkflowManager,
        rule: CheckpointRule,
        offeringId: String,
        offerings: Offerings,
        uiConfig: UiConfig,
    ): CheckpointResolution {
        val offering: Offering = offerings.all[offeringId]
            ?: return unservableRule(rule, "offering '$offeringId' was not found in offerings")
        val workflow = try {
            workflowManager.getWorkflow(rule.workflowId)
        } catch (e: PurchasesException) {
            return unservableRule(rule, "it could not be loaded: ${e.error}")
        }
        debugLog {
            "Checkpoint resolved to workflow '${rule.workflowId}' (offering: ${offering.identifier})"
        }
        return CheckpointResolution.Workflow(workflow, uiConfig, offering)
    }

    private fun unservableRule(rule: CheckpointRule, reason: String): CheckpointResolution.NoAction {
        warnLog { "The matched checkpoint rule for workflow '${rule.workflowId}' can't be served: $reason." }
        return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
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
        debugLog { "No rule for checkpoint '$identifier' matched." }
        return CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH)
    }

    /**
     * Pairs a checkpoint rule with the predicate its audience stands for. Audience predicates will arrive in their
     * own remote-config topic, whose shape is not settled, so until then every audience matches and the rules are
     * effectively still walked in published order.
     */
    private class AudienceRule(val checkpointRule: CheckpointRule) : LocalRule {
        override val predicate: String = PLACEHOLDER_AUDIENCE_PREDICATE
    }

    private companion object {
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
        const val PLACEHOLDER_AUDIENCE_PREDICATE = "true"
    }
}
