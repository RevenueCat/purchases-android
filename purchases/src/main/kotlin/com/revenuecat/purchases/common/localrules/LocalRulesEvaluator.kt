@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.rules.RulesEngine

internal sealed class LocalRulesEvaluationException(message: String) : Exception(message) {

    /** The predicate at [ruleIndex] could not be evaluated; [error] is a `RulesEngine.EvaluationException`. */
    internal data class PredicateEvaluation(
        val ruleIndex: Int,
        val error: Throwable,
    ) : LocalRulesEvaluationException("rule at index $ruleIndex could not be evaluated: ${error.message}")

    /** [reason] is a [RulesDimensionResolutionException]. */
    internal data class DimensionResolution(
        val reason: Throwable,
    ) : LocalRulesEvaluationException("dimensions could not be collected: ${reason.message}")
}

/**
 * Evaluates rules against freshly collected dimensions.
 *
 * One snapshot serves a whole call, so every rule in it is judged against the same view of the customer.
 */
internal class LocalRulesEvaluator(
    providers: List<RulesDimensionProvider>,
    currentAppUserId: () -> String,
    dateProvider: DateProvider = DefaultDateProvider(),
) {

    private val dimensionResolver = RulesDimensionResolver(providers, currentAppUserId, dateProvider)

    /**
     * The first rule that matches, or `null` when none does.
     *
     * A predicate the engine cannot evaluate (malformed JSON, an operator this SDK version does not implement, a
     * dimension this SDK version does not supply) is not enough to fail the call on its own: a later rule may
     * still match definitively, and it wins. Only when nothing matched does the first such failure surface,
     * because then "no match" cannot be told apart from "we failed to ask".
     *
     * When a predicate must be resolved before it can be evaluated, a resolution failure fails the call immediately.
     * [customVariables] are the caller's own values for this evaluation, readable under `custom.*`.
     */
    suspend fun <Rule : LocalRule> match(
        rules: List<Rule>,
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
    ): Result<Rule?> = match(rules, customVariables) { rule -> Result.success(rule.predicate) }

    @Suppress("ReturnCount")
    suspend fun <Rule> match(
        rules: List<Rule>,
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
        predicateFor: suspend (Rule) -> Result<String>,
    ): Result<Rule?> {
        if (rules.isEmpty()) return Result.success(null)

        val snapshot = dimensionResolver.snapshot(customVariables).fold(
            onSuccess = { snapshot -> snapshot },
            onFailure = { error ->
                return Result.failure(LocalRulesEvaluationException.DimensionResolution(error))
            },
        )

        var firstFailure: LocalRulesEvaluationException.PredicateEvaluation? = null
        for ((index, rule) in rules.withIndex()) {
            val predicate = predicateFor(rule).getOrElse { error -> return Result.failure(error) }
            val result = RulesEngine.evaluate(predicate, snapshot.values)
            val matches = result.getOrElse { error ->
                if (firstFailure == null) {
                    firstFailure = LocalRulesEvaluationException.PredicateEvaluation(index, error)
                }
                false
            }
            if (matches) return Result.success(rule)
        }

        return firstFailure?.let { failure -> Result.failure(failure) } ?: Result.success(null)
    }
}
