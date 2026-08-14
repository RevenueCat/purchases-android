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
import com.revenuecat.purchases.common.checkpoints.CheckpointRule
import com.revenuecat.purchases.common.checkpoints.CheckpointRulesResolution
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.localrules.LocalRule
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.customVariableDimensions
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
internal class CheckpointWorkflowResolverImpl(
    private val workflowManager: WorkflowManager?,
    private val uiConfigProvider: UiConfigProvider?,
    private val checkpointsConfigProvider: CheckpointsConfigProvider?,
    private val localRulesEvaluator: LocalRulesEvaluator,
    private val getOfferings: suspend () -> Offerings,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private val audiencePredicate: String = PLACEHOLDER_AUDIENCE_PREDICATE,
) : CheckpointWorkflowResolver {

    override suspend fun resolve(identifier: String, customVariables: Map<String, Any>): CheckpointResolution {
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
        customVariables: Map<String, Any>,
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
        val matchResult = localRulesEvaluator.match(
            rules = checkpoint.rules.map { rule -> AudienceRule(rule, audiencePredicate) },
            customVariables = customVariableDimensions(customVariables),
        )
        // An audience the SDK failed to evaluate is not the same answer as an audience the customer is outside of,
        // so it can't report NO_MATCH.
        val rule = matchResult.getOrElse { error ->
            return configurationUnavailable(
                "The audiences for checkpoint '$identifier' could not be evaluated: ${error.message}",
            )
        }?.checkpointRule ?: return noMatch(identifier)

        val dependencies = CheckpointResolutionDependencies(
            identifier = identifier,
            workflowManager = workflowManager,
            uiConfigProvider = uiConfigProvider,
            getOfferings = getOfferings,
        )
        val uiConfig = dependencies.uiConfig()
            ?: return configurationUnavailable("UI config is unavailable for checkpoint '$identifier'.")
        return resolveRule(workflowManager, dependencies, rule, uiConfig)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveRule(
        workflowManager: WorkflowManager,
        dependencies: CheckpointResolutionDependencies,
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
                unsupportedOfferingRule(rule, "an offering step cannot be mixed with other steps")
            } else {
                resolveOfferingRule(dependencies, rule, initialStep)
            }
        } else if (workflow.steps.values.any { it.type == OFFERING_STEP_TYPE }) {
            unservableRule(rule, "a UI workflow cannot contain offering steps")
        } else {
            resolveUiRule(dependencies, rule, workflow, uiConfig)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun resolveOfferingRule(
        dependencies: CheckpointResolutionDependencies,
        rule: CheckpointRule,
        step: WorkflowStep,
    ): CheckpointResolution {
        val offeringIdentifier = (step.paramValues[OFFERING_IDENTIFIER_PARAM] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: return unsupportedOfferingRule(rule, "the offering step has no valid offering identifier")

        val offering = dependencies.offering(offeringIdentifier)
        if (offering == null) {
            return unservableRule(rule, "offering '$offeringIdentifier' was not found in offerings")
        }
        debugLog {
            "Checkpoint resolved to offering '${offering.identifier}' from workflow '${rule.workflowId}'"
        }
        return CheckpointResolution.MatchedOffering(offering)
    }

    private fun unsupportedOfferingRule(rule: CheckpointRule, reason: String): CheckpointResolution.NoAction =
        unservableRule(rule, reason)

    @Suppress("ReturnCount")
    private suspend fun resolveUiRule(
        dependencies: CheckpointResolutionDependencies,
        rule: CheckpointRule,
        workflow: PublishedWorkflow,
        uiConfig: UiConfig,
    ): CheckpointResolution {
        val offeringId = dependencies.offeringIdForWorkflow(rule.workflowId)
        if (offeringId == null) {
            return unservableRule(rule, "no offering is mapped to it in the workflows topic")
        }
        val offering = dependencies.offering(offeringId)
        if (offering == null) {
            return unservableRule(rule, "offering '$offeringId' was not found in offerings")
        }
        debugLog {
            "Checkpoint resolved to workflow '${rule.workflowId}' (offering: ${offering.identifier})"
        }
        dependencies.prewarmWorkflowAssets(workflow, uiConfig)
        return CheckpointResolution.MatchedWorkflow(workflow, uiConfig, offering)
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
    private class AudienceRule(
        val checkpointRule: CheckpointRule,
        override val predicate: String,
    ) : LocalRule

    private companion object {
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
        const val OFFERING_STEP_TYPE = "offering"
        const val OFFERING_IDENTIFIER_PARAM = "offering_identifier"
        const val PLACEHOLDER_AUDIENCE_PREDICATE = "true"
    }
}

private class CheckpointResolutionDependencies(
    private val identifier: String,
    private val workflowManager: WorkflowManager,
    private val uiConfigProvider: UiConfigProvider?,
    private val getOfferings: suspend () -> Offerings,
) {
    private var offeringsLoaded = false
    private var offerings: Offerings? = null
    private var workflowOfferingIndexLoaded = false
    private var workflowOfferingIndex: Map<String, String> = emptyMap()
    private var uiConfigLoaded = false
    private var resolvedUiConfig: UiConfig? = null

    suspend fun offering(identifier: String): Offering? = loadOfferings()?.all?.get(identifier)

    suspend fun offeringIdForWorkflow(workflowId: String): String? = loadWorkflowOfferingIndex()[workflowId]

    suspend fun uiConfig(): UiConfig? {
        if (!uiConfigLoaded) {
            uiConfigLoaded = true
            resolvedUiConfig = try {
                uiConfigProvider?.getUiConfig()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog(e) { "UI config could not be fetched for checkpoint '$identifier'." }
                null
            }
        }
        return resolvedUiConfig
    }

    fun prewarmWorkflowAssets(workflow: PublishedWorkflow, uiConfig: UiConfig) {
        workflowManager.prewarmWorkflowAssets(workflow, uiConfig)
    }

    private suspend fun loadOfferings(): Offerings? {
        if (!offeringsLoaded) {
            offeringsLoaded = true
            offerings = try {
                getOfferings()
            } catch (e: PurchasesException) {
                errorLog { "Offerings could not be fetched for checkpoint '$identifier': ${e.error}" }
                null
            }
        }
        return offerings
    }

    private suspend fun loadWorkflowOfferingIndex(): Map<String, String> {
        if (!workflowOfferingIndexLoaded) {
            workflowOfferingIndexLoaded = true
            workflowOfferingIndex = try {
                workflowManager.offeringIdByWorkflowId()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                errorLog(e) { "Workflow offering metadata could not be fetched for checkpoint '$identifier'." }
                emptyMap()
            }
        }
        return workflowOfferingIndex
    }
}
