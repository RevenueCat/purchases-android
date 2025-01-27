package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class StackComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeStackComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: StackComponent,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all values present",
                    Args(
                        json = """
                        {
                          "background_color": {
                            "light": {
                              "type": "alias",
                              "value": "secondary"
                            }
                          },
                          "border": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            },
                            "width": 123
                          },
                          "components": [
                            {
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "primary"
                                }
                              },
                              "components": [],
                              "id": "xmpgCrN9Rb",
                              "name": "Text",
                              "text_lid": "7bkohQjzIE",
                              "type": "text"
                            }
                          ],
                          "corner_radiuses": {
                            "bottom_leading": 8,
                            "bottom_trailing": 6,
                            "top_leading": 4,
                            "top_trailing": 2
                          },
                          "size": {
                            "width": {
                              "type": "fill",
                              "value": null
                            },
                            "height": {
                              "type": "fit",
                              "value": null
                            }
                          },
                          "dimension": {
                            "alignment": "leading",
                            "distribution": "space_between",
                            "type": "vertical"
                          },
                          "id": "WLbwQoNUKF",
                          "margin": {
                            "bottom": 10,
                            "leading": 12,
                            "top": 14,
                            "trailing": 16
                          },
                          "name": "Stack",
                          "padding": {
                            "bottom": 16,
                            "leading": 14,
                            "top": 12,
                            "trailing": 10
                          },
                          "shape": {
                            "type": "pill"
                          },
                          "spacing": 8,
                          "type": "stack",
                          "shadow": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "tertiary"
                              }
                            },
                            "radius": 20.1,
                            "x": 23.6,
                            "y": 45.2
                          }
                        }
                        """.trimIndent(),
                        expected = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("7bkohQjzIE"),
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                )
                            ),
                            dimension = Dimension.Vertical(
                                alignment = HorizontalAlignment.LEADING,
                                distribution = FlexDistribution.SPACE_BETWEEN
                            ),
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            spacing = 8f,
                            backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("secondary"))),
                            padding = Padding(top = 12.0, leading = 14.0, bottom = 16.0, trailing = 10.0),
                            margin = Padding(top = 14.0, leading = 12.0, bottom = 10.0, trailing = 16.0),
                            shape = Shape.Pill,
                            border = Border(color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))), width = 123.0),
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                radius = 20.1,
                                x = 23.6,
                                y = 45.2
                            ),
                        )
                    ),
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "components": [
                            {
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "primary"
                                }
                              },
                              "components": [],
                              "id": "xmpgCrN9Rb",
                              "name": "Text",
                              "text_lid": "7bkohQjzIE",
                              "type": "text"
                            }
                          ],
                          "id": "WLbwQoNUKF",
                          "name": "Stack",
                          "type": "stack"
                        }
                        """.trimIndent(),
                        expected = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("7bkohQjzIE"),
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                )
                            ),
                        )
                    ),
                ),
                arrayOf(
                    "empty margin and padding",
                    Args(
                        json = """
                        {
                          "components": [
                            {
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "primary"
                                }
                              },
                              "components": [],
                              "id": "xmpgCrN9Rb",
                              "name": "Text",
                              "text_lid": "7bkohQjzIE",
                              "type": "text"
                            }
                          ],
                          "margin": {},
                          "name": "Stack",
                          "padding": {},
                          "type": "stack"
                        }
                        """.trimIndent(),
                        expected = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("7bkohQjzIE"),
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                )
                            ),
                            margin = Padding.zero,
                            padding = Padding.zero,
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize StackComponent as StackComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<StackComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize StackComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialStackComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialStackComponent,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all values present",
                    Args(
                        json = """
                        {
                          "visible": false,
                          "dimension": {
                            "alignment": "leading",
                            "distribution": "space_between",
                            "type": "vertical"
                          },
                          "size": {
                            "width": {
                              "type": "fill",
                              "value": null
                            },
                            "height": {
                              "type": "fit",
                              "value": null
                            }
                          },
                          "spacing": 8.0,
                          "background_color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "padding": {
                            "bottom": 2,
                            "leading": 3,
                            "top": 1,
                            "trailing": 4
                          },
                          "margin": {
                            "bottom": 3,
                            "leading": 2,
                            "top": 4,
                            "trailing": 1
                          },
                          "shape": {
                            "type": "pill"
                          },
                          "border": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "secondary"
                              }
                            },
                            "width": 23
                          },
                          "shadow": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "tertiary"
                              }
                            },
                            "radius": 20.1,
                            "x": 23.6,
                            "y": 45.2
                          }
                        }
                        """.trimIndent(),
                        expected = PartialStackComponent(
                            visible = false,
                            dimension = Dimension.Vertical(
                                alignment = HorizontalAlignment.LEADING,
                                distribution = FlexDistribution.SPACE_BETWEEN
                            ),
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            spacing = 8f,
                            backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                            padding = Padding(top = 1.0, bottom = 2.0, leading = 3.0, trailing = 4.0),
                            margin = Padding(top = 4.0, bottom = 3.0, leading = 2.0, trailing = 1.0),
                            shape = Shape.Pill,
                            border = Border(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("secondary"))),
                                width = 23.0
                            ),
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                radius = 20.1,
                                x = 23.6,
                                y = 45.2
                            )
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialStackComponent()
                    )
                ),
                arrayOf(
                    "empty padding and margin",
                    Args(
                        json = """
                        {
                          "padding": {},
                          "margin": {}
                        }
                        """.trimIndent(),
                        expected = PartialStackComponent(
                            padding = Padding.zero,
                            margin = Padding.zero,
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialStackComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialStackComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
