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
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowManager
import com.revenuecat.purchases.common.workflows.WorkflowStep
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

/**
 * Resolves a checkpoint through the `checkpoint_rules` topic: the checkpoint's rules are read from remote
 * config, and the first one that resolves to a presentable workflow wins. Rules arrive ordered, and walking them
 * in order is the placeholder for the audience evaluation that will eventually pick the first rule whose
 * `audience_id` matches this customer — until then the order the dashboard published is the only signal.
 *
 * Each workflow body is inspected before presentation dependencies are loaded. A terminal offering step
 * returns its offering to the app, while a UI workflow keeps the existing presentation path. Unsupported or
 * unavailable rules are skipped so a later rule can still be served. Shared dependencies are loaded lazily and
 * at most once per checkpoint resolution.
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
        if (workflowManager == null || checkpointsConfigProvider == null) {
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

        val dependencies = CheckpointResolutionDependencies(
            identifier = identifier,
            workflowManager = workflowManager,
            uiConfigProvider = uiConfigProvider,
            getOfferings = getOfferings,
        )
        for (rule in checkpoint.rules) {
            resolveRule(workflowManager, dependencies, rule)?.let { return it }
        }
        return configurationUnavailable("No rule for checkpoint '$identifier' resolved to a servable workflow.")
    }

    private suspend fun resolveRule(
        workflowManager: WorkflowManager,
        dependencies: CheckpointResolutionDependencies,
        rule: CheckpointRule,
    ): CheckpointResolution? {
        val workflow = try {
            workflowManager.getPublishedWorkflow(rule.workflowId)
        } catch (e: PurchasesException) {
            logSkippedRule(rule, "it could not be loaded: ${e.error}")
            return null
        }
        val offeringSteps = workflow.steps.values.filter { it.type == OFFERING_STEP_TYPE }
        return if (offeringSteps.isEmpty()) {
            resolveUiRule(dependencies, rule, workflow)
        } else {
            resolveOfferingRule(dependencies, rule, workflow, offeringSteps)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun resolveOfferingRule(
        dependencies: CheckpointResolutionDependencies,
        rule: CheckpointRule,
        workflow: PublishedWorkflow,
        offeringSteps: List<WorkflowStep>,
    ): CheckpointResolution.Offering? {
        val step = offeringSteps.singleOrNull()
            ?: return unsupportedOfferingRule(rule, "it contains more than one offering step")
        if (workflow.steps.size != 1) {
            return unsupportedOfferingRule(rule, "an offering step cannot be mixed with other steps")
        }
        if (workflow.steps[workflow.initialStepId] != step) {
            return unsupportedOfferingRule(rule, "the offering step is not the initial step")
        }
        if (step.screenId != null) {
            return unsupportedOfferingRule(rule, "the offering step contains a screen")
        }
        if (step.triggers.isNotEmpty()) {
            return unsupportedOfferingRule(rule, "the offering step contains triggers")
        }
        if (step.triggerActions.isNotEmpty()) {
            return unsupportedOfferingRule(rule, "the offering step contains trigger actions")
        }
        val offeringIdentifier = (step.paramValues[OFFERING_IDENTIFIER_PARAM] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: return unsupportedOfferingRule(rule, "the offering step has no valid offering identifier")

        val offering = dependencies.offering(offeringIdentifier)
        if (offering == null) {
            logSkippedRule(rule, "offering '$offeringIdentifier' was not found in offerings")
            return null
        }
        debugLog {
            "Checkpoint resolved to offering '${offering.identifier}' from workflow '${rule.workflowId}'"
        }
        return CheckpointResolution.Offering(offering)
    }

    private fun unsupportedOfferingRule(rule: CheckpointRule, reason: String): CheckpointResolution.Offering? {
        logSkippedRule(rule, reason)
        return null
    }

    @Suppress("ReturnCount")
    private suspend fun resolveUiRule(
        dependencies: CheckpointResolutionDependencies,
        rule: CheckpointRule,
        workflow: PublishedWorkflow,
    ): CheckpointResolution.Workflow? {
        val offeringId = dependencies.offeringIdForWorkflow(rule.workflowId)
        if (offeringId == null) {
            logSkippedRule(rule, "no offering is mapped to it in the workflows topic")
            return null
        }
        val uiConfig = dependencies.uiConfig()
        if (uiConfig == null) {
            logSkippedRule(rule, "UI config is unavailable")
            return null
        }
        val offering = dependencies.offering(offeringId)
        if (offering == null) {
            logSkippedRule(rule, "offering '$offeringId' was not found in offerings")
            return null
        }
        debugLog {
            "Checkpoint resolved to workflow '${rule.workflowId}' (offering: ${offering.identifier})"
        }
        dependencies.prewarmWorkflowAssets(workflow, uiConfig)
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
        const val OFFERING_STEP_TYPE = "offering"
        const val OFFERING_IDENTIFIER_PARAM = "offering_identifier"
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
