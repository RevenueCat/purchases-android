package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.JsonTools
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class AlignmentTests {

    @RunWith(Parameterized::class)
    public class HorizontalAlignmentTests(private val serialized: String, private val expected: HorizontalAlignment) {

        public companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = HorizontalAlignment.values().map { expected ->
                val serialized = when (expected) {
                    HorizontalAlignment.LEADING -> "\"leading\""
                    HorizontalAlignment.CENTER -> "\"center\""
                    HorizontalAlignment.TRAILING -> "\"trailing\""
                }
                arrayOf(serialized, expected)
            } + listOf(
                arrayOf("\"some_future_unknown_horizontal_alignment\"", HorizontalAlignment.LEADING)
            )
        }

        @Test
        fun `Should properly deserialize HorizontalAlignment`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<HorizontalAlignment>(serialized)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    public class VerticalAlignmentTests(private val serialized: String, private val expected: VerticalAlignment) {

        public companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = VerticalAlignment.values().map { expected ->
                val serialized = when (expected) {
                    VerticalAlignment.TOP -> "\"top\""
                    VerticalAlignment.CENTER -> "\"center\""
                    VerticalAlignment.BOTTOM -> "\"bottom\""
                }
                arrayOf(serialized, expected)
            } + listOf(
                arrayOf("\"some_future_unknown_vertical_alignment\"", VerticalAlignment.TOP)
            )
        }

        @Test
        fun `Should properly deserialize VerticalAlignment`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<VerticalAlignment>(serialized)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    public class TwoDimensionalAlignmentTests(private val serialized: String, private val expected: TwoDimensionalAlignment) {

        public companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = TwoDimensionalAlignment.values().map { expected ->
                val serialized = when (expected) {
                    TwoDimensionalAlignment.CENTER -> "\"center\""
                    TwoDimensionalAlignment.LEADING -> "\"leading\""
                    TwoDimensionalAlignment.TRAILING -> "\"trailing\""
                    TwoDimensionalAlignment.TOP -> "\"top\""
                    TwoDimensionalAlignment.BOTTOM -> "\"bottom\""
                    TwoDimensionalAlignment.TOP_LEADING -> "\"top_leading\""
                    TwoDimensionalAlignment.TOP_TRAILING -> "\"top_trailing\""
                    TwoDimensionalAlignment.BOTTOM_LEADING -> "\"bottom_leading\""
                    TwoDimensionalAlignment.BOTTOM_TRAILING -> "\"bottom_trailing\""
                }
                arrayOf(serialized, expected)
            } + listOf(
                arrayOf("\"some_future_unknown_two_dimensional_alignment\"", TwoDimensionalAlignment.TOP)
            )
        }

        @Test
        fun `Should properly deserialize TwoDimensionalAlignment`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<TwoDimensionalAlignment>(serialized)

            // Assert
            assert(actual == expected)
        }
    }
}
