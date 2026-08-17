package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

/**
 * A value exposed to the rules engine.
 *
 * Keeps the sources of dimensions independent from the engine's own value representation, so a source says what a
 * dimension *is* and this package decides how JSON Logic sees it. Objects are deliberately not expressible: a
 * dimension has to be something an operator can compare or search.
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
     * Reaches the engine as an array, which is what `in` and `some` search. A single-value dimension is not the
     * same thing: `in` over a string matches substrings, so `pro` would match `pro_annual`.
     */
    public data class StringListValue(val value: List<String>) : RulesDimensionValue()
}
