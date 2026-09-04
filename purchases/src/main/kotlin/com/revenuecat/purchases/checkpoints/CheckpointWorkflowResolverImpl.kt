@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.checkpoints.CheckpointWorkflowResolverImpl.Companion.SIMULATED_ERROR_CHECKPOINT_ID
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.audiences.AudiencesConfigProvider
import com.revenuecat.purchases.common.checkpoints.CheckpointRule
import com.revenuecat.purchases.common.checkpoints.CheckpointRulesResolution
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowManager
import com.revenuecat.purchases.common.workflows.WorkflowStep
import kotlinx.coroutines.CancellationException

/**
 * Resolves a checkpoint through the `checkpoint_rules` topic: the checkpoint's rules are read from remote config
 * and evaluated in order against locally collected dimensions, and the first rule whose audience matches wins.
 *
 * The winner is final. If its workflow turns out to be unservable — no offering identifier on its initial step or
 * that step's screen, that offering absent from the fetched offerings, or its body unavailable — the checkpoint
 * resolves to
 * [CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE] rather than falling through to a rule the
 * customer was not the first choice for.
 *
 * Resolution reads config across several suspension points, so a commit (or an identity change) can land halfway
 * through and leave the rules the winner was picked from stale. That answer is discarded and resolution starts
 * over against the new state, once — a second stale attempt reports
 * [CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE] rather than spinning on a burst of commits.
 *
 * [SIMULATED_ERROR_CHECKPOINT_ID] is the one piece of PoC scaffolding left: it is the only way for the tester
 * apps to exercise the throw path, since nothing in the config-driven path throws.
 */
@Suppress("TooManyFunctions")
internal class CheckpointWorkflowResolverImpl(
    private val workflowManager: WorkflowManager,
    private val uiConfigProvider: UiConfigProvider,
    private val checkpointsConfigProvider: CheckpointsConfigProvider,
    private val audiencesConfigProvider: AudiencesConfigProvider,
    private val localRulesEvaluator: LocalRulesEvaluator,
    private val getOfferings: suspend () -> Offerings,
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
        attemptResolve(identifier, customVariables)?.let { return it }
        verboseLog { "Remote config changed while resolving checkpoint '$identifier'; resolving it again." }
        return attemptResolve(identifier, customVariables)
            ?: configurationUnavailable("Remote config kept changing while resolving checkpoint '$identifier'.")
    }

    /**
     * One resolution attempt against a single config generation, or `null` if the generation moved under it — the
     * rules the winning rule came from are no longer the committed ones, so the answer can't be reported as if it
     * were. [resolve] retries such an attempt exactly once, so resolution never runs more than twice.
     */
    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    private suspend fun attemptResolve(
        identifier: String,
        customVariables: Map<String, RulesDimensionValue>,
    ): CheckpointResolution? {
        val rulesResolution = when (val resolution = checkpointsConfigProvider.resolveCheckpoint(identifier)) {
            is CheckpointRulesResolution.Found -> resolution
            CheckpointRulesResolution.NotConfigured -> return unknownCheckpoint(identifier)
            CheckpointRulesResolution.Unavailable ->
                return configurationUnavailable("The rules for checkpoint '$identifier' could not be read.")
        }
        val matchResult = matchRule(audiencesConfigProvider, rulesResolution.checkpoint.rules, customVariables)
        // Checked before the result is unwrapped: a match that failed against a generation that moved mid-read
        // (audiences read from a later commit than the rules) is stale rather than authoritative, and deserves
        // the retry as much as a stale success does.
        if (!checkpointsConfigProvider.isCurrent(rulesResolution)) return null
        // An audience the SDK failed to evaluate (unreadable audiences, a predicate the engine cannot run) is not
        // the same answer as an audience the customer is outside of, so it can't report NO_MATCH. A predicate on a
        // dimension this SDK does not supply is the latter: the evaluator already counts it as a non-match.
        val rule = matchResult.getOrElse { error ->
            return configurationUnavailable(
                "The audiences for checkpoint '$identifier' could not be evaluated: ${error.message}",
            )
        }
        if (rule == null) return noMatch(identifier)
        val uiConfig = try {
            uiConfigProvider.getUiConfig()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "UI config could not be fetched for checkpoint '$identifier'." }
            null
        } ?: return configurationUnavailable("UI config is unavailable for checkpoint '$identifier'.")
        val result = resolveRule(identifier, workflowManager, rule, uiConfig)
        return result.takeIf { checkpointsConfigProvider.isCurrent(rulesResolution) }
    }

    /**
     * The whole audience dictionary is read once, so every rule is matched against audiences from the same
     * committed config, and matching itself never re-reads config.
     */
    @Suppress("ReturnCount")
    private suspend fun matchRule(
        audiencesConfigProvider: AudiencesConfigProvider,
        rules: List<CheckpointRule>,
        customVariables: Map<String, RulesDimensionValue>,
    ): Result<CheckpointRule?> {
        if (rules.isEmpty()) return Result.success(null)
        val audiences = audiencesConfigProvider.getAudiences()
            ?: return Result.failure(AudiencesUnavailableException())
        return localRulesEvaluator.match(
            rules = rules,
            customVariables = CustomVariableKeyValidator.validateAndFilter(customVariables),
        ) { rule ->
            audiences[rule.audienceId]
                ?.let { audience -> Result.success(audience.rules) }
                ?: Result.failure(AudienceUnavailableException(rule.audienceId))
        }
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
            resolveUiRule(checkpointIdentifier, rule, workflow, uiConfig, initialStep)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun resolveOfferingRule(
        checkpointIdentifier: String,
        rule: CheckpointRule,
        step: WorkflowStep,
    ): CheckpointResolution {
        val offeringIdentifier = step.offeringIdentifier
            ?: return unservableRule(rule, "the offering step has no valid offering identifier")

        val offering = loadOffering(checkpointIdentifier, offeringIdentifier)
            ?: return unservableRule(rule, "offering '$offeringIdentifier' was not found in offerings")
        debugLog {
            "Checkpoint resolved to offering '${offering.identifier}' from workflow '${rule.workflowId}'"
        }
        return CheckpointResolution.MatchedOffering(offering)
    }

    @Suppress("ReturnCount")
    private suspend fun resolveUiRule(
        checkpointIdentifier: String,
        rule: CheckpointRule,
        workflow: PublishedWorkflow,
        uiConfig: UiConfig,
        initialStep: WorkflowStep,
    ): CheckpointResolution {
        val offeringId = workflow.offeringIdentifierFor(initialStep)
            ?: return unservableRule(rule, "its initial step has no valid offering identifier")
        val offering = loadOffering(checkpointIdentifier, offeringId)
            ?: return unservableRule(rule, "offering '$offeringId' was not found in offerings")
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

    private class AudienceUnavailableException(identifier: String) :
        Exception("audience '$identifier' could not be read")

    private class AudiencesUnavailableException :
        Exception("the audiences configuration could not be read")

    private companion object {
        const val SIMULATED_ERROR_CHECKPOINT_ID = "error_checkpoint"
        const val OFFERING_STEP_TYPE = "offering"
    }
}
