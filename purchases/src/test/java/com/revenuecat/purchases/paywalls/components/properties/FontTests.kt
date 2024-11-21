package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class FontTests {

    @RunWith(Parameterized::class)
    class FontWeightTests(private val serialized: String, private val expected: FontWeight) {

        companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = FontWeight.values().map { expected ->
                val serialized = when (expected) {
                    FontWeight.EXTRA_LIGHT -> "\"extra_light\""
                    FontWeight.THIN -> "\"thin\""
                    FontWeight.LIGHT -> "\"light\""
                    FontWeight.REGULAR -> "\"regular\""
                    FontWeight.MEDIUM -> "\"medium\""
                    FontWeight.SEMI_BOLD -> "\"semi_bold\""
                    FontWeight.BOLD -> "\"bold\""
                    FontWeight.EXTRA_BOLD -> "\"extra_bold\""
                    FontWeight.BLACK -> "\"black\""
                }
                arrayOf(serialized, expected)
            }
        }

        @Test
        fun `Should properly deserialize FontWeight`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<FontWeight>(serialized)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    class FontSizeTests(private val serialized: String, private val expected: FontSize) {

        companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = FontSize.values().map { expected ->
                val serialized = when (expected) {
                    FontSize.HEADING_XXL -> "\"heading_xxl\""
                    FontSize.HEADING_XL -> "\"heading_xl\""
                    FontSize.HEADING_L -> "\"heading_l\""
                    FontSize.HEADING_M -> "\"heading_m\""
                    FontSize.HEADING_S -> "\"heading_s\""
                    FontSize.HEADING_XS -> "\"heading_xs\""
                    FontSize.BODY_XL -> "\"body_xl\""
                    FontSize.BODY_L -> "\"body_l\""
                    FontSize.BODY_M -> "\"body_m\""
                    FontSize.BODY_S -> "\"body_s\""
                }
                arrayOf(serialized, expected)
            }
        }

        @Test
        fun `Should properly deserialize FontSize`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<FontSize>(serialized)

            // Assert
            assert(actual == expected)
        }
    }

}
