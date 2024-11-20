package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ColorInfoTests {

    @RunWith(Parameterized::class)
    class DeserializeColorInfoTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ColorInfo,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "hex",
                    Args(
                        json = """
                            {
                              "type": "hex",
                              "value": "#ff00ffff"
                            }
                        """.trimIndent(),
                        expected = ColorInfo.Hex(
                            value = "#ff00ffff"
                        )
                    )
                ),
                arrayOf(
                    "alias",
                    Args(
                        json = """
                            {
                              "type": "alias",
                              "value": "primary"
                            }
                        """.trimIndent(),
                        expected = ColorInfo.Alias(
                            value = "primary"
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ColorInfo`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ColorInfo>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializeColorSchemeTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ColorScheme,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "light and dark",
                    Args(
                        json = """
                            {
                              "light": {
                                "type": "hex",
                                "value": "#ff00ffff"
                              },
                              "dark": {
                                "type": "alias",
                                "value": "primary"
                              }
                            }
                        """.trimIndent(),
                        expected = ColorScheme(
                            light = ColorInfo.Hex(value = "#ff00ffff"),
                            dark = ColorInfo.Alias(value = "primary"),
                        )
                    )
                ),
                arrayOf(
                    "light only",
                    Args(
                        json = """
                            {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            }
                        """.trimIndent(),
                        expected = ColorScheme(
                            light = ColorInfo.Alias(value = "primary"),
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ColorScheme`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ColorScheme>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
