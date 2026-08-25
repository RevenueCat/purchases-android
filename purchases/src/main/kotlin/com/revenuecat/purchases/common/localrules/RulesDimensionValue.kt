package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

/**
 * A value exposed to the rules engine.
 *
 * Keeps the sources of dimensions independent from the engine's own value representation, so a source says what a
 * dimension *is* and this package decides how JSON Logic sees it. A dimension has to be something an operator can
 * compare, read through, or search: a scalar is compared, an object is read through by name, and only a collection
 * is searched.
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
     * record has none of is left out, and a predicate that reads an absent key fails as an unresolved variable.
     *
     * A predicate inside an iteration operator sees only the record it is looking at. `var` cannot reach the
     * scope around it, so a record has to carry every value a predicate about it might need — including the
     * evaluation instant it is compared against — unless the predicate reads the snapshot with `rc.rootVar`.
     */
    public data class ObjectListValue(val value: List<Map<String, RulesDimensionValue>>) : RulesDimensionValue()

    /**
     * A named group of values, for a dimension that is one thing described several ways — a subscriber attribute
     * and when it was set.
     *
     * Reaches the engine as a nested object, which `var` walks by dot-path: `subscriberAttributes.goal.value`
     * resolves *through* `goal`. Unlike a record inside [ObjectListValue], a predicate reading one of these still
     * sees the whole scope around it, because no iteration operator is involved.
     *
     * An empty group is truthy in JSON Logic, so a source with nothing to say about a name leaves the name out
     * rather than supplying one of these with no values in it.
     */
    public data class ObjectValue(val value: Map<String, RulesDimensionValue>) : RulesDimensionValue()
}
