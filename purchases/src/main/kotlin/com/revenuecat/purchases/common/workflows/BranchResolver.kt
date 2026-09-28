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

    /**
     * Resolves the branches [step] can exit through, when that step becomes current. Navigation stays
     * synchronous: until this lands, a branch takes its fallback.
     */
    suspend fun resolveBranches(
        step: WorkflowStep,
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
    ): Map<WorkflowTriggerAction.Branch, String> {
        val branches = step.triggerActions.values
            .filterIsInstance<WorkflowTriggerAction.Branch>()
            .toSet()

        return branches.associateWith { branch -> resolve(branch, customVariables) }
    }
}

/** Used when remote config is off, so there is nothing to evaluate audiences against. */
internal object DisabledBranchResolver : BranchResolver {
    override suspend fun resolve(
        branch: WorkflowTriggerAction.Branch,
        customVariables: Map<String, RulesDimensionValue>,
    ): String = branch.fallbackStepId
}

/**
 * Resolves audiences in order and returns the first match. Mirrors how checkpoint rules resolve theirs,
 * including the walk: a failure does not stop a later audience from winning.
 */
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
                // Not a failure: match ends the walk on a resolution failure, and an audience we cannot
                // read must not stop a later one from winning. Never matches instead.
                unreadable.add(route.audienceId)
                Result.success(NEVER_MATCHES)
            } else {
                Result.success(audience.rules)
            }
        }

        matched.getOrNull()?.let { return it.stepId }

        if (matched.isFailure) {
            errorLog { "Branch routed to its fallback step: ${matched.exceptionOrNull()}." }
        } else if (unreadable.isNotEmpty()) {
            errorLog { "Branch routed to its fallback step: could not read ${unreadable.joinToString()}." }
        }
        return branch.fallbackStepId
    }

    private companion object {
        /** Stands in for an audience we could not read, so the walk continues to the next one. */
        const val NEVER_MATCHES = """{"==": [1, 0]}"""
    }
}
