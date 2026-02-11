package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.JsonTools
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
public class BadgeStyleTests(private val serialized: String, private val expected: Badge.Style) {

    public companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = Badge.Style.values().map { expected ->
            val serialized = when (expected) {
                Badge.Style.Overlay -> "\"overlay\""
                Badge.Style.EdgeToEdge -> "\"edge_to_edge\""
                Badge.Style.Nested -> "\"nested\""
            }
            arrayOf(serialized, expected)
        } + listOf(
            arrayOf("\"some_future_unknown_style\"", Badge.Style.Overlay)
        )
    }

    @Test
    fun `Should properly deserialize BadgeStyle`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<Badge.Style>(serialized)

        // Assert
        assert(actual == expected)
    }

}
