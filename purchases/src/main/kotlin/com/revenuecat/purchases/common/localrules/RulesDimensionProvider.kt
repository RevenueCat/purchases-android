@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

/**
 * Supplies part of the root scope of on-device dimensions.
 *
 * Implementations may observe or persist state internally, but values are pulled only when an evaluation asks for
 * a new snapshot, since eligibility reflects the customer's state at the moment the rule is evaluated.
 */
internal interface RulesDimensionProvider {

    /** The name this provider is reported under in diagnostics. It is not part of the evaluated scope. */
    val name: String

    /**
     * The complete current set of values, keyed by root-level snake_case names (`app_version`);
     * [RulesDimensionResolver] merges every provider into a single root scope.
     *
     * The root names are part of the contract predicates are authored against, so a provider must not claim one
     * that is not its own: the resolver fails the whole snapshot when two sources supply the same name, or when a
     * provider supplies one of the roots the resolver itself owns (`evaluated_at`, `custom`, `backend`).
     *
     * A value that is unavailable is omitted rather than guessed: a predicate that reads an absent key fails as
     * an unresolved variable, rather than being answered from a value we invented. A name no predicate could
     * read is dropped by [RulesDimensionResolver], which applies that rule to every provider. Throwing is
     * reserved for a systemic failure to produce this provider's values.
     *
     * [date] is the common reference instant for the evaluation. It does not indicate when the underlying values
     * were observed.
     */
    suspend fun dimensions(date: Date): Map<String, RulesDimensionValue>
}
