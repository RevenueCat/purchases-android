@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.CustomVariableKeyValidator
import com.revenuecat.purchases.common.audiences.AudiencesConfigProvider
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue

/**
 * Decides where a `branch` trigger action sends someone.
 *
 * The return is not nullable. There is always a `fallbackStepId`, so navigation is never blocked.
 */
internal interface BranchResolver {

    suspend fun resolve(
        branch: WorkflowTriggerAction.Branch,
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
    ): String

    /** The branches [step] can exit through, keyed by action id. */
    suspend fun resolveBranches(
        step: WorkflowStep,
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
    ): Map<String, String> = step.triggerActions
        .mapNotNull { (actionId, action) ->
            (action as? WorkflowTriggerAction.Branch)?.let { actionId to resolve(it, customVariables) }
        }
        .toMap()
}

/** Every branch takes its fallback. Goes away with [AppConfig.branchingEnabled] once branching ships. */
internal object DisabledBranchResolver : BranchResolver {
    override suspend fun resolve(
        branch: WorkflowTriggerAction.Branch,
        customVariables: Map<String, RulesDimensionValue>,
    ): String = branch.fallbackStepId
}

/** Resolves audiences in order and returns the first match. */
internal class BranchResolverImpl(
    private val audiencesConfigProvider: AudiencesConfigProvider,
    private val localRulesEvaluator: LocalRulesEvaluator,
) : BranchResolver {

    @Suppress("ReturnCount")
    override suspend fun resolve(
        branch: WorkflowTriggerAction.Branch,
        customVariables: Map<String, RulesDimensionValue>,
    ): String {
        if (branch.branches.isEmpty()) return branch.fallbackStepId

        // One snapshot for the whole walk, so a config swap midway cannot mix two generations.
        val audiences = audiencesConfigProvider.getAudiences()
        if (audiences == null) {
            errorLog { "Branch routed to its fallback step: no audience configuration." }
            return branch.fallbackStepId
        }

        val unreadable = mutableListOf<String>()
        val matched = localRulesEvaluator.match(
            rules = branch.branches,
            customVariables = CustomVariableKeyValidator.validateAndFilter(customVariables),
            logPrefix = "[Workflow branch] ",
        ) { route ->
            val audience = audiences[route.audienceId]
            if (audience == null) {
                unreadable.add(route.audienceId)
                Result.success(NEVER_MATCHES)
            } else {
                Result.success(audience.rules)
            }
        }

        // Reported even when a later audience matched, otherwise a route that silently stopped firing
        // leaves no trace at all.
        if (unreadable.isNotEmpty()) {
            errorLog { "Workflow branch could not read ${unreadable.joinToString()}." }
        }

        matched.getOrNull()?.let { return it.stepId }

        if (matched.isFailure) {
            errorLog { "Branch routed to its fallback step: ${matched.exceptionOrNull()}." }
        }
        return branch.fallbackStepId
    }

    private companion object {
        /**
         * Stands in for an audience we could not read. A failed resolution would end the walk, and one
         * unreadable audience must not stop a later one from winning, so it never matches instead.
         */
        const val NEVER_MATCHES = """{"==": [1, 0]}"""
    }
}
