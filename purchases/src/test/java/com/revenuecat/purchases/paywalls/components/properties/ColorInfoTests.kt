package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.colorInt
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo.Gradient.Point
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
                    "hex - rgba",
                    Args(
                        json = """
                            {
                              "type": "hex",
                              "value": "#ff331122"
                            }
                        """.trimIndent(),
                        expected = ColorInfo.Hex(
                            value = colorInt(alpha = 0x22, red = 0xff, green = 0x33, blue = 0x11)
                        )
                    )
                ),
                arrayOf(
                    "hex - rgb",
                    Args(
                        json = """
                            {
                              "type": "hex",
                              "value": "#332211"
                            }
                        """.trimIndent(),
                        expected = ColorInfo.Hex(
                            value = colorInt(alpha = 0xff, red = 0x33, green = 0x22, blue = 0x11)
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
                            ColorAlias(
                                value = "primary"
                            )
                        )
                    )
                ),
                arrayOf(
                    "gradient - linear",
                    Args(
                        json = """
                            {
                              "type": "linear",
                              "degrees": 45,
                              "points": [
                                {
                                  "color": "#032400ff",
                                  "percent": 0
                                },
                                {
                                  "color": "#090979ff",
                                  "percent": 35
                                },
                                {
                                  "color": "#216c32ff",
                                  "percent": 100
                                }
                              ]
                            }
                        """.trimIndent(),
                        expected = ColorInfo.Gradient.Linear(
                            degrees = 45f,
                            points = listOf(
                                Point(
                                    color = colorInt(alpha = 0xff, red = 0x03, green = 0x24, blue = 0x00),
                                    percent = 0f,
                                ),
                                Point(
                                    color = colorInt(alpha = 0xff, red = 0x09, green = 0x09, blue = 0x79),
                                    percent = 35f,
                                ),
                                Point(
                                    color = colorInt(alpha = 0xff, red = 0x21, green = 0x6c, blue = 0x32),
                                    percent = 100f,
                                )
                            )
                        )
                    )
                ),
                arrayOf(
                    "gradient - radial",
                    Args(
                        json = """
                            {
                              "type": "radial",
                              "points": [
                                {
                                  "color": "#032400ff",
                                  "percent": 0
                                },
                                {
                                  "color": "#090979ff",
                                  "percent": 35
                                },
                                {
                                  "color": "#216c32ff",
                                  "percent": 100
                                }
                              ]
                            }
                        """.trimIndent(),
                        expected = ColorInfo.Gradient.Radial(
                            points = listOf(
                                Point(
                                    color = colorInt(alpha = 0xff, red = 0x03, green = 0x24, blue = 0x00),
                                    percent = 0f,
                                ),
                                Point(
                                    color = colorInt(alpha = 0xff, red = 0x09, green = 0x09, blue = 0x79),
                                    percent = 35f,
                                ),
                                Point(
                                    color = colorInt(alpha = 0xff, red = 0x21, green = 0x6c, blue = 0x32),
                                    percent = 100f,
                                )
                            )
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ColorInfo`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ColorInfo>(args.json)

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
                                "value": "#f403a1de"
                              },
                              "dark": {
                                "type": "alias",
                                "value": "primary"
                              }
                            }
                        """.trimIndent(),
                        expected = ColorScheme(
                            light = ColorInfo.Hex(
                                value = colorInt(alpha = 0xde, red = 0xf4, green = 0x03, blue = 0xa1)
                            ),
                            dark = ColorInfo.Alias(ColorAlias(value = "primary")),
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
                            light = ColorInfo.Alias(ColorAlias(value = "primary")),
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ColorScheme`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ColorScheme>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
