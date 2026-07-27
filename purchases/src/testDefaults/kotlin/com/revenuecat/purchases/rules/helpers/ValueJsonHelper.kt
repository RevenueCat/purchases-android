package com.revenuecat.purchases.rules.helpers

import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.ValueJson

/**
 * Test-only convenience for converting a JSON literal into a [Value]. Lets
 * the tests express predicates the same way they appear in rule artifacts.
 * Delegates to the production [ValueJson] parser so there is a single source
 * of truth for JSON → [Value] conversion.
 */
internal object ValueJsonHelper {
    fun fromJsonString(input: String): Value = ValueJson.parse(input)
}
