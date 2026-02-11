package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.JsonTools
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FlexDistributionTests(private val serialized: String, private val expected: FlexDistribution) {

    public companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = FlexDistribution.values().map { expected ->
            val serialized = when (expected) {
                FlexDistribution.START -> "\"start\""
                FlexDistribution.END -> "\"end\""
                FlexDistribution.CENTER -> "\"center\""
                FlexDistribution.SPACE_BETWEEN -> "\"space_between\""
                FlexDistribution.SPACE_AROUND -> "\"space_around\""
                FlexDistribution.SPACE_EVENLY -> "\"space_evenly\""
            }
            arrayOf(serialized, expected)
        } + listOf(
            arrayOf("\"some_future_unknown_flex_distribution\"", FlexDistribution.START)
        )
    }

    @Test
    fun `Should properly deserialize FlexDistribution`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<FlexDistribution>(serialized)

        // Assert
        assert(actual == expected)
    }

}
