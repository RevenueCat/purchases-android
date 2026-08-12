package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomVariableDimensionsTest {

    @Test
    fun `each supported type maps to its dimension value`() {
        val dimensions = customVariableDimensions(
            mapOf(
                "text" to "value",
                "flag" to true,
                "count" to 3,
                "big_count" to 3L,
                "ratio" to 1.5f,
                "precise_ratio" to 1.5,
            ),
        )

        assertThat(dimensions).isEqualTo(
            mapOf(
                "text" to RulesDimensionValue.StringValue("value"),
                "flag" to RulesDimensionValue.BoolValue(true),
                "count" to RulesDimensionValue.IntValue(3),
                "big_count" to RulesDimensionValue.IntValue(3),
                "ratio" to RulesDimensionValue.DoubleValue(1.5),
                "precise_ratio" to RulesDimensionValue.DoubleValue(1.5),
            ),
        )
    }

    @Test
    fun `values with no scalar representation are dropped`() {
        val dimensions = customVariableDimensions(
            mapOf(
                "kept" to "value",
                "list" to listOf(1, 2),
                "nested" to mapOf("a" to 1),
                "date" to Date(0),
            ),
        )

        assertThat(dimensions).containsOnlyKeys("kept")
    }

    @Test
    fun `keys that cannot be addressed from a predicate are dropped`() {
        val dimensions = customVariableDimensions(
            mapOf(
                "my_property" to "kept",
                "my.property" to "dropped",
                "2fast" to "dropped",
                "has space" to "dropped",
                "my-property" to "dropped",
                "" to "dropped",
            ),
        )

        assertThat(dimensions).isEqualTo(mapOf("my_property" to RulesDimensionValue.StringValue("kept")))
    }

    @Test
    fun `no custom variables yields no dimensions`() {
        assertThat(customVariableDimensions(emptyMap())).isEmpty()
    }
}
