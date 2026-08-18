package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * A scalar exposed to the rules engine.
 *
 * Keeps the sources of dimensions independent from the engine's own value representation, and encodes that a
 * dimension is a scalar: arrays and objects are deliberately not expressible yet.
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
}
