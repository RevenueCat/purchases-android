package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Value
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringArrayOperatorsTest {

    // ---- merge ----

    // Kept as a Kotlin test (not migrated to a JSON fixture): `merge` returns
    // an array, and this engine's loose `==` string-coerces `[[1], 2]` and
    // `[1, 2]` to the same `"1,2"`, so no predicate can verify that nested
    // arrays are NOT recursively flattened. Only a structural `Value`
    // comparison can. The remaining `in` / `cat` / `substr` / `merge` cases
    // now live in `predicate-fixtures/{in,cat,substr,merge}.json`.
    @Test
    fun `merge does not recurse on nested arrays`() {
        // Only one level of flattening — inner arrays remain.
        val out = StringArrayOperators.opMerge(
            arr(arr(arr(Value.IntValue(1)), Value.IntValue(2))),
            Value.Null,
        )
        assertThat(out).isEqualTo(arr(arr(Value.IntValue(1)), Value.IntValue(2)))
    }

    private fun arr(vararg items: Value): Value = Value.ArrayValue(items.toList())
}
