package com.revenuecat.purchases.common.localrules

/**
 * A rule that can be evaluated against locally collected dimensions.
 *
 * Consumers implement this on their own type, and [LocalRulesEvaluator.match] hands that same instance back when
 * the rule wins, so whatever the rule needs to carry rides along without this package knowing about it.
 */
internal interface LocalRule {

    /** The rule's JSON Logic predicate, evaluated against a [RulesDimensionSnapshot]. */
    val predicate: String
}
