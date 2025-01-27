package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class IconComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeIconComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: IconComponent,
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
                          "base_url": "https://assets.pawwalls.com",
                          "icon_name": "Test icon name",
                          "formats": {
                            "webp": "test.webp"
                          },
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "padding": {
                            "bottom": 16,
                            "leading": 14,
                            "top": 12,
                            "trailing": 10
                          },
                          "margin": {
                            "bottom": 10,
                            "leading": 12,
                            "top": 14,
                            "trailing": 16
                          },
                          "size": {
                            "height": {
                              "type": "fixed",
                              "value": 200
                            },
                            "width": {
                              "type": "fixed",
                              "value": 200
                            }
                          },
                          "icon_background": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "secondary"
                              }
                            },
                            "shape": {
                              "corners": {
                                "top_leading": 3,
                                "top_trailing": 4,
                                "bottom_leading": 1,
                                "bottom_trailing": 2
                              },
                              "type": "rectangle"
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
                            },
                            "border": {
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "secondary"
                                }
                              },
                              "width": 123
                            }
                          },
                          "type": "icon"
                        }
                        """.trimIndent(),
                        expected = IconComponent(
                            baseUrl = "https://assets.pawwalls.com",
                            iconName = "Test icon name",
                            formats = IconComponent.Formats(
                                webp = "test.webp"
                            ),
                            size = Size(
                                height = SizeConstraint.Fixed(200u),
                                width = SizeConstraint.Fixed(200u),
                            ),
                            color = ColorScheme(
                                light = ColorInfo.Alias(ColorAlias("primary"))
                            ),
                            padding = Padding(
                                bottom = 16.0,
                                leading = 14.0,
                                top = 12.0,
                                trailing = 10.0,
                            ),
                            margin = Padding(
                                bottom = 10.0,
                                leading = 12.0,
                                top = 14.0,
                                trailing = 16.0,
                            ),
                            iconBackground = IconComponent.IconBackground(
                                color = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("secondary"))
                                ),
                                shape = MaskShape.Rectangle(
                                    corners = CornerRadiuses.Dp(
                                        topLeading = 3.0,
                                        topTrailing = 4.0,
                                        bottomLeading = 1.0,
                                        bottomTrailing = 2.0,
                                    )
                                ),
                                shadow = Shadow(
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("tertiary"))
                                    ),
                                    radius = 20.1,
                                    x = 23.6,
                                    y = 45.2,
                                ),
                                border = Border(
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("secondary"))
                                    ),
                                    width = 123.0,
                                ),
                            ),
                        )
                    ),
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "base_url": "https://assets.pawwalls.com",
                          "icon_name": "Test icon name",
                          "formats": {
                            "webp": "test.webp"
                          },
                          "type": "icon"
                        }
                        """.trimIndent(),
                        expected = IconComponent(
                            baseUrl = "https://assets.pawwalls.com",
                            iconName = "Test icon name",
                            formats = IconComponent.Formats(
                                webp = "test.webp"
                            ),
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize IconComponent as IconComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<IconComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize IconComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialIconComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialIconComponent,
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
                          "visible": true,
                          "base_url": "https://assets.pawwalls.com",
                          "icon_name": "Test icon name",
                          "formats": {
                            "webp": "test.webp"
                          },
                          "color": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "padding": {
                            "bottom": 16,
                            "leading": 14,
                            "top": 12,
                            "trailing": 10
                          },
                          "margin": {
                            "bottom": 10,
                            "leading": 12,
                            "top": 14,
                            "trailing": 16
                          },
                          "size": {
                            "height": {
                              "type": "fixed",
                              "value": 200
                            },
                            "width": {
                              "type": "fixed",
                              "value": 200
                            }
                          },
                          "icon_background": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "secondary"
                              }
                            },
                            "shape": {
                              "corners": {
                                "top_leading": 3,
                                "top_trailing": 4,
                                "bottom_leading": 1,
                                "bottom_trailing": 2
                              },
                              "type": "rectangle"
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
                            },
                            "border": {
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "secondary"
                                }
                              },
                              "width": 123
                            }
                          },
                          "type": "icon"
                        }
                        """.trimIndent(),
                        expected = PartialIconComponent(
                            visible = true,
                            baseUrl = "https://assets.pawwalls.com",
                            iconName = "Test icon name",
                            formats = IconComponent.Formats(
                                webp = "test.webp"
                            ),
                            size = Size(
                                height = SizeConstraint.Fixed(200u),
                                width = SizeConstraint.Fixed(200u),
                            ),
                            color = ColorScheme(
                                light = ColorInfo.Alias(ColorAlias("primary"))
                            ),
                            padding = Padding(
                                bottom = 16.0,
                                leading = 14.0,
                                top = 12.0,
                                trailing = 10.0,
                            ),
                            margin = Padding(
                                bottom = 10.0,
                                leading = 12.0,
                                top = 14.0,
                                trailing = 16.0,
                            ),
                            iconBackground = IconComponent.IconBackground(
                                color = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("secondary"))
                                ),
                                shape = MaskShape.Rectangle(
                                    corners = CornerRadiuses.Dp(
                                        topLeading = 3.0,
                                        topTrailing = 4.0,
                                        bottomLeading = 1.0,
                                        bottomTrailing = 2.0,
                                    )
                                ),
                                shadow = Shadow(
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("tertiary"))
                                    ),
                                    radius = 20.1,
                                    x = 23.6,
                                    y = 45.2,
                                ),
                                border = Border(
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("secondary"))
                                    ),
                                    width = 123.0,
                                ),
                            ),
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialIconComponent()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialIconComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialIconComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
