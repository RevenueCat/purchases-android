@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.checkpoints.CheckpointRule
import com.revenuecat.purchases.common.checkpoints.CheckpointRulesResolution
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.localrules.LocalRule
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowManager
import com.revenuecat.purchases.common.workflows.WorkflowStep
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

/**
 * Resolves a checkpoint through the `checkpoint_rules` topic: the checkpoint's rules are read from remote config
 * and evaluated in order against locally collected dimensions, and the first rule whose audience matches wins.
 *
 * Audience predicates are not served yet, so every rule currently carries [PLACEHOLDER_AUDIENCE_PREDICATE] and the
 * published order remains the effective signal; see [AudienceRule].
 *
 * [SIMULATED_ERROR_CHECKPOINT_ID] is the one piece of PoC scaffolding left: it is the only way for the tester
 * apps to exercise the throw path, since nothing in the config-driven path throws.
 */
// TooManyFunctions/LongParameterList: both are the hardcoded test-rule scaffolding, not the real shape.
@Suppress("LongParameterList", "TooManyFunctions")
internal class CheckpointWorkflowResolverImpl(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val checkpointsConfigProvider: CheckpointsConfigProvider?,
    private val localRulesEvaluator: LocalRulesEvaluator,
    private val getOfferings: suspend () -> Offerings,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private val audiencePredicate: String = PLACEHOLDER_AUDIENCE_PREDICATE,
    // TEMPORARY, not for main: see [TEST_RULES]. Tests opt out to exercise the published-rule walk.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private val useHardcodedTestRules: Boolean = true,
) : CheckpointWorkflowResolver {

    override suspend fun resolve(
        identifier: String,
        customVariables: Map<String, RulesDimensionValue>,
    ): CheckpointResolution {
        if (identifier == SIMULATED_ERROR_CHECKPOINT_ID) {
            val error = PurchasesError(
                PurchasesErrorCode.ConfigurationError,
                "Simulated error: checkpoint workflow not presentable.",
            )
            errorLog(error)
            throw PurchasesException(error)
        }
        return resolveConfiguredWorkflow(identifier, customVariables)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveConfiguredWorkflow(
        identifier: String,
        customVariables: Map<String, RulesDimensionValue>,
    ): CheckpointResolution {
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
        val rules = if (useHardcodedTestRules) {
            hardcodedTestRules(workflowManager)
        } else {
            checkpoint.rules.map { rule -> AudienceRule(rule, audiencePredicate) }
        }
        val matchResult = localRulesEvaluator.match(
            rules = rules,
            customVariables = CustomVariableKeyValidator.validateAndFilter(customVariables),
        )
        // An audience the SDK failed to evaluate is not the same answer as an audience the customer is outside of,
        // so it can't report NO_MATCH.
        val rule = matchResult.getOrElse { error ->
            return configurationUnavailable(
                "The audiences for checkpoint '$identifier' could not be evaluated: ${error.message}",
            )
        }?.checkpointRule ?: return noMatch(identifier)

        val uiConfig = try {
            uiConfigProvider.getUiConfig()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "UI config could not be fetched for checkpoint '$identifier'." }
            null
        } ?: return configurationUnavailable("UI config is unavailable for checkpoint '$identifier'.")
        return resolveRule(identifier, workflowManager, rule, uiConfig)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveRule(
        checkpointIdentifier: String,
        workflowManager: WorkflowManager,
        rule: CheckpointRule,
        uiConfig: UiConfig,
    ): CheckpointResolution {
        val workflow = try {
            workflowManager.getWorkflowBody(rule.workflowId)
        } catch (e: PurchasesException) {
            return unservableRule(rule, "it could not be loaded: ${e.error}")
        }
        val initialStep = workflow.steps[workflow.initialStepId]
            ?: return unservableRule(rule, "its initial step was not found")
        return if (initialStep.type == OFFERING_STEP_TYPE) {
            if (workflow.steps.size != 1) {
                unservableRule(rule, "an offering step cannot be mixed with other steps")
            } else {
                resolveOfferingRule(checkpointIdentifier, rule, initialStep)
            }
        } else if (workflow.steps.values.any { it.type == OFFERING_STEP_TYPE }) {
            unservableRule(rule, "a UI workflow cannot contain offering steps")
        } else {
            resolveUiRule(checkpointIdentifier, workflowManager, rule, workflow, uiConfig)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun resolveOfferingRule(
        checkpointIdentifier: String,
        rule: CheckpointRule,
        step: WorkflowStep,
    ): CheckpointResolution {
        val offeringIdentifier = (step.paramValues[OFFERING_IDENTIFIER_PARAM] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: return unservableRule(rule, "the offering step has no valid offering identifier")

        val offering = loadOffering(checkpointIdentifier, offeringIdentifier)
        if (offering == null) {
            return unservableRule(rule, "offering '$offeringIdentifier' was not found in offerings")
        }
        debugLog {
            "Checkpoint resolved to offering '${offering.identifier}' from workflow '${rule.workflowId}'"
        }
        return CheckpointResolution.MatchedOffering(offering)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveUiRule(
        checkpointIdentifier: String,
        workflowManager: WorkflowManager,
        rule: CheckpointRule,
        workflow: PublishedWorkflow,
        uiConfig: UiConfig,
    ): CheckpointResolution {
        val offeringId = try {
            workflowManager.offeringIdByWorkflowId()[rule.workflowId]
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Workflow offering metadata could not be fetched for checkpoint '$checkpointIdentifier'." }
            null
        }
        if (offeringId == null) {
            return unservableRule(rule, "no offering is mapped to it in the workflows topic")
        }
        val offering = loadOffering(checkpointIdentifier, offeringId)
        if (offering == null) {
            return unservableRule(rule, "offering '$offeringId' was not found in offerings")
        }
        debugLog {
            "Checkpoint resolved to workflow '${rule.workflowId}' (offering: ${offering.identifier})"
        }
        workflowManager.prewarmWorkflowAssets(workflow, uiConfig)
        return CheckpointResolution.MatchedWorkflow(workflow, uiConfig, offering)
    }

    private suspend fun loadOffering(checkpointIdentifier: String, offeringIdentifier: String): Offering? =
        try {
            getOfferings().all[offeringIdentifier]
        } catch (e: PurchasesException) {
            errorLog { "Offerings could not be fetched for checkpoint '$checkpointIdentifier': ${e.error}" }
            null
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
     * TEMPORARY, not for main: builds [TEST_RULES] into rules the evaluator can walk.
     *
     * The targets are named by offering rather than by workflow because workflow ids are generated per project,
     * while the offering ids are the ones a tester recognises, so the workflow that renders each target offering is
     * looked up by inverting [WorkflowManager.offeringIdByWorkflowId].
     *
     * A rule whose offering no workflow renders is skipped rather than kept, unlike the real walk where it would
     * win and then fail: skipping keeps the remaining rules testable on a project that lacks one of the offerings.
     */
    private suspend fun hardcodedTestRules(workflowManager: WorkflowManager): List<AudienceRule> {
        val workflowIdByOfferingId = workflowManager.offeringIdByWorkflowId()
            .entries
            .associate { (workflowId, offeringId) -> offeringId to workflowId }
        return TEST_RULES.mapNotNull { (predicate, offeringId) ->
            val workflowId = workflowIdByOfferingId[offeringId]
            if (workflowId == null) {
                warnLog { "Skipping hardcoded test rule for offering '$offeringId': no workflow renders it." }
                null
            } else {
                AudienceRule(
                    checkpointRule = CheckpointRule(
                        id = "hardcoded_$offeringId",
                        audienceId = "hardcoded_$offeringId",
                        workflowId = workflowId,
                    ),
                    predicate = predicate,
                )
            }
        }.also { rules ->
            debugLog { "Evaluating ${rules.size} hardcoded test rules instead of the published ones." }
        }
    }

    /**
     * Pairs a checkpoint rule with the predicate its audience stands for. Audience predicates will arrive in their
     * own remote-config topic, whose shape is not settled, so until then every audience matches and the rules are
     * effectively still walked in published order.
     */
    private class AudienceRule(
        val checkpointRule: CheckpointRule,
        override val predicate: String,
    ) : LocalRule

    private companion object {
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
        const val OFFERING_STEP_TYPE = "offering"
        const val OFFERING_IDENTIFIER_PARAM = "offering_identifier"
        const val PLACEHOLDER_AUDIENCE_PREDICATE = "true"

        /**
         * TEMPORARY, not for main: predicate to target offering, in priority order. Stands in for the audiences
         * topic so rule selection can be exercised by hand before that topic exists.
         */
        val TEST_RULES = listOf(
            """{"and": [{"==": [{"var": "custom.name"}, "Antonio"]}, """ +
                """{"==": [{"var": "device.locale"}, "en_us"]}]}""" to "default",
            """{"==": [{"var": "custom.name"}, "Antonio"]}""" to "rick",
            "true" to "no_name",
        )
    }
}
