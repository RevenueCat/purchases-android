package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BadgeStyleTests(private val serialized: String, private val expected: Badge.Style) {

    companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = Badge.Style.values().map { expected ->
            val serialized = when (expected) {
                Badge.Style.Overlay -> "\"overlay\""
                Badge.Style.EdgeToEdge -> "\"edge_to_edge\""
                Badge.Style.Nested -> "\"nested\""
            }
            arrayOf(serialized, expected)
        }
    }

    @Test
    fun `Should properly deserialize BadgeStyle`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<Badge.Style>(serialized)

        // Assert
        assert(actual == expected)
    }

}
