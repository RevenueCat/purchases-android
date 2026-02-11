package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.JsonTools
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FitModeTests(private val serialized: String, private val expected: FitMode) {

    public companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = FitMode.values().map { expected ->
            val serialized = when (expected) {
                FitMode.FIT -> "\"fit\""
                FitMode.FILL -> "\"fill\""
            }
            arrayOf(serialized, expected)
        } + listOf(
            arrayOf("\"some_future_unknown_fit_mode\"", FitMode.FIT)
        )
    }

    @Test
    fun `Should properly deserialize FitMode`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<FitMode>(serialized)

        // Assert
        assert(actual == expected)
    }

}
