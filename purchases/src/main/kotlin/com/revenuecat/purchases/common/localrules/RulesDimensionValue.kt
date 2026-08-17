package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

/**
 * A value exposed to the rules engine.
 *
 * Keeps the sources of dimensions independent from the engine's own value representation, so a source says what a
 * dimension *is* and this package decides how JSON Logic sees it. A dimension has to be something an operator can
 * compare or search, so an object is expressible only as an element of a collection — which is what the iteration
 * operators search — and never on its own.
 *
 * Integers and doubles are separate so a source can say which it means — an API level is a whole number, a ratio is
 * not — even though the engine collapses both into the single number JSON Logic models, comparing and rendering
 * `34` and `34.0` identically.
 *
 * Public only so callers outside `:purchases` can supply the values a rule is evaluated against.
 */
@InternalRevenueCatAPI
public sealed class RulesDimensionValue {
    public data class StringValue(val value: String) : RulesDimensionValue()
    public data class BoolValue(val value: Boolean) : RulesDimensionValue()
    public data class IntValue(val value: Long) : RulesDimensionValue()
    public data class DoubleValue(val value: Double) : RulesDimensionValue()

    /**
     * Reaches the engine as epoch milliseconds, so a predicate orders dates with the ordinary numeric operators
     * and there is no date type for one platform to spell differently than another.
     */
    public data class DateValue(val value: Date) : RulesDimensionValue()

    /**
     * A collection of records, for a dimension the customer has any number of rather than one of — their
     * purchases, their entitlements.
     *
     * Reaches the engine as an array of objects, which `some`, `all`, `none` and `filter` walk one record at a
     * time. A record's own values are dimensions in their own right, so the same rules apply to them: a value the
     * record has none of is left out, and an absent key resolves to null in the engine.
     *
     * A predicate inside an iteration operator sees only the record it is looking at, with no access to the scope
     * around it, so a record has to carry every value a predicate about it might need — including the evaluation
     * instant it is compared against.
     */
    public data class ObjectListValue(val value: List<Map<String, RulesDimensionValue>>) : RulesDimensionValue()
}
