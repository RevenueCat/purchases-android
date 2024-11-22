package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ShadowTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: Shadow,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "everything zero - ints as string",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": "0",
                          "x": "0",
                          "y": "0"
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 0.0,
                        x = 0.0,
                        y = 0.0,
                    )
                )
            ),
            arrayOf(
                "everything positive - ints as string",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": "10",
                          "x": "20",
                          "y": "30"
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.0,
                        x = 20.0,
                        y = 30.0,
                    )
                )
            ),
            arrayOf(
                "offsets negative - ints as string",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": "10",
                          "x": "-21",
                          "y": "-32"
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.0,
                        x = -21.0,
                        y = -32.0,
                    )
                )
            ),
            arrayOf(
                "everything zero - floats as string",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": "0.0",
                          "x": "0.0",
                          "y": "0.0"
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 0.0,
                        x = 0.0,
                        y = 0.0,
                    )
                )
            ),
            arrayOf(
                "everything positive - floats as string",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": "10.4",
                          "x": "20.3",
                          "y": "30.2"
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.4,
                        x = 20.3,
                        y = 30.2,
                    )
                )
            ),
            arrayOf(
                "offsets negative - floats as string",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": "10.1",
                          "x": "-21.2",
                          "y": "-32.3"
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.1,
                        x = -21.2,
                        y = -32.3,
                    )
                )
            ),
            arrayOf(
                "everything zero - ints ",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": 0,
                          "x": 0,
                          "y": 0
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 0.0,
                        x = 0.0,
                        y = 0.0,
                    )
                )
            ),
            arrayOf(
                "everything positive - ints ",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": 10,
                          "x": 20,
                          "y": 30
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.0,
                        x = 20.0,
                        y = 30.0,
                    )
                )
            ),
            arrayOf(
                "offsets negative - ints ",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": 10,
                          "x": -21,
                          "y": -32
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.0,
                        x = -21.0,
                        y = -32.0,
                    )
                )
            ),
            arrayOf(
                "everything zero - floats ",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": 0.0,
                          "x": 0.0,
                          "y": 0.0
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 0.0,
                        x = 0.0,
                        y = 0.0,
                    )
                )
            ),
            arrayOf(
                "everything positive - floats ",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": 10.4,
                          "x": 20.3,
                          "y": 30.2
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.4,
                        x = 20.3,
                        y = 30.2,
                    )
                )
            ),
            arrayOf(
                "offsets negative - floats ",
                Args(
                    json = """
                        {
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "radius": 10.1,
                          "x": -21.2,
                          "y": -32.3
                        }
                        """.trimIndent(),
                    expected = Shadow(
                        color = ColorScheme(
                            light = ColorInfo.Alias("primary")
                        ),
                        radius = 10.1,
                        x = -21.2,
                        y = -32.3,
                    )
                )
            ),
        )
    }

    @Test
    fun `Should properly deserialize Shadow`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<Shadow>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
