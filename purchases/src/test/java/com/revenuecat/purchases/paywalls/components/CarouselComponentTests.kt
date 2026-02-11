package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class CarouselComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeCarouselComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            public val json: String,
            public val expected: CarouselComponent,
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
                          "type": "carousel",
                          "initial_page_index": 2,
                          "page_alignment": "center",
                          "size": {
                            "height": {
                              "type": "fit",
                              "value": null
                            },
                            "width": {
                              "type": "fill",
                              "value": null
                            }
                          },
                          "page_peek": 10,
                          "padding": {
                            "top": 1,
                            "leading": 2,
                            "bottom": 3,
                            "trailing": 4
                          },
                          "margin": {
                            "top": 5,
                            "leading": 6,
                            "bottom": 7,
                            "trailing": 8
                          },
                          "shape": {
                            "type": "pill"
                          },
                          "page_spacing": 8,
                          "background_color": {
                            "light": {
                              "type": "alias",
                              "value": "tertiary"
                            },
                            "dark": {
                              "type": "alias",
                              "value": "another_alias"
                            }
                          },
                          "background": {
                            "type": "color",
                            "value": {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            }
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
                                "value": "primary"
                              }
                            },
                            "width": 123
                          },
                          "page_control": {
                            "position": "top",
                            "spacing": 5,
                            "margin": {
                              "top": 1,
                              "leading": 2,
                              "bottom": 3,
                              "trailing": 4
                            },
                            "padding": {
                              "top": 5,
                              "leading": 6,
                              "bottom": 7,
                              "trailing": 8
                            },
                            "background_color": {
                              "light": {
                                "type": "alias",
                                "value": "secondary"
                              }
                            },
                            "shape": {
                              "type": "pill"
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
                                  "value": "primary"
                                }
                              },
                              "width": 123
                            },
                            "active": {
                              "width": 10,
                              "height": 20,
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "primary"
                                },
                                "dark": {
                                  "type": "alias",
                                  "value": "secondary"
                                }
                              }
                            },
                            "default": {
                              "width": 30,
                              "height": 40,
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "tertiary"
                                },
                                "dark": {
                                  "type": "alias",
                                  "value": "another_alias"
                                }
                              }
                            }
                          },
                          "loop": true,
                          "auto_advance": {
                            "ms_time_per_page": 1000,
                            "ms_transition_time": 500,
                            "transition_type": "fade"
                          },
                          "pages": [
                            {
                              "type": "stack",
                              "components": []
                            }
                          ],
                          "visible": false
                        }
                        """.trimIndent(),
                        expected = CarouselComponent(
                            pages = listOf(
                                StackComponent(
                                    components = emptyList()
                                )
                            ),
                            visible = false,
                            initialPageIndex = 2,
                            pageAlignment = VerticalAlignment.CENTER,
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            pagePeek = 10,
                            padding = Padding(
                                top = 1.0,
                                leading = 2.0,
                                bottom = 3.0,
                                trailing = 4.0
                            ),
                            margin = Padding(
                                top = 5.0,
                                leading = 6.0,
                                bottom = 7.0,
                                trailing = 8.0
                            ),
                            shape = Shape.Pill,
                            pageSpacing = 8f,
                            backgroundColor = ColorScheme(
                                light = ColorInfo.Alias(ColorAlias("tertiary")),
                                dark = ColorInfo.Alias(ColorAlias("another_alias"))
                            ),
                            background = Background.Color(
                                value = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                            ),
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                radius = 20.1,
                                x = 23.6,
                                y = 45.2
                            ),
                            border = Border(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                                width = 123.0
                            ),
                            pageControl = CarouselComponent.PageControl(
                                position = CarouselComponent.PageControl.Position.TOP,
                                spacing = 5,
                                padding = Padding(
                                    top = 5.0,
                                    leading = 6.0,
                                    bottom = 7.0,
                                    trailing = 8.0
                                ),
                                margin = Padding(
                                    top = 1.0,
                                    leading = 2.0,
                                    bottom = 3.0,
                                    trailing = 4.0
                                ),
                                backgroundColor = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("secondary"))
                                ),
                                shape = Shape.Pill,
                                shadow = Shadow(
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                    radius = 20.1,
                                    x = 23.6,
                                    y = 45.2
                                ),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                                    width = 123.0
                                ),
                                active = CarouselComponent.PageControl.Indicator(
                                    width = 10u,
                                    height = 20u,
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary")),
                                        dark = ColorInfo.Alias(ColorAlias("secondary"))
                                    ),
                                ),
                                default = CarouselComponent.PageControl.Indicator(
                                    width = 30u,
                                    height = 40u,
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("tertiary")),
                                        dark = ColorInfo.Alias(ColorAlias("another_alias"))
                                    ),
                                )
                            ),
                            loop = true,
                            autoAdvance = CarouselComponent.AutoAdvancePages(
                                msTimePerPage = 1000,
                                msTransitionTime = 500,
                                transitionType = CarouselComponent.AutoAdvancePages.TransitionType.FADE,
                            )
                        )
                    ),
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "type": "carousel",
                          "page_alignment": "center",
                          "pages": [
                            {
                              "type": "stack",
                              "components": []
                            }
                          ]
                        }
                        """.trimIndent(),
                        expected = CarouselComponent(
                            pages = listOf(
                                StackComponent(
                                    components = emptyList()
                                )
                            ),
                            pageAlignment = VerticalAlignment.CENTER,
                        )
                    ),
                ),
                arrayOf(
                    "unknown page_control position",
                    Args(
                        json = """
                        {
                          "type": "carousel",
                          "page_alignment": "center",
                          "pages": [
                            {
                              "type": "stack",
                              "components": []
                            }
                          ],
                          "page_control": {
                            "position": "some_unknown_value",
                            "active": {
                              "width": 10,
                              "height": 20,
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "primary"
                                },
                                "dark": {
                                  "type": "alias",
                                  "value": "secondary"
                                }
                              }
                            },
                            "default": {
                              "width": 30,
                              "height": 40,
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "tertiary"
                                },
                                "dark": {
                                  "type": "alias",
                                  "value": "another_alias"
                                }
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                        expected = CarouselComponent(
                            pages = listOf(
                                StackComponent(
                                    components = emptyList()
                                )
                            ),
                            pageAlignment = VerticalAlignment.CENTER,
                            pageControl = CarouselComponent.PageControl(
                                position = CarouselComponent.PageControl.Position.BOTTOM,
                                active = CarouselComponent.PageControl.Indicator(
                                    width = 10u,
                                    height = 20u,
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary")),
                                        dark = ColorInfo.Alias(ColorAlias("secondary"))
                                    ),
                                ),
                                default = CarouselComponent.PageControl.Indicator(
                                    width = 30u,
                                    height = 40u,
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("tertiary")),
                                        dark = ColorInfo.Alias(ColorAlias("another_alias"))
                                    ),
                                )
                            )
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize CarouselComponent as CarouselComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<CarouselComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize CarouselComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<CarouselComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialCarouselComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            public val json: String,
            public val expected: PartialCarouselComponent,
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
                          "initial_page_index": 2,
                          "page_alignment": "center",
                          "size": {
                            "height": {
                              "type": "fit",
                              "value": null
                            },
                            "width": {
                              "type": "fill",
                              "value": null
                            }
                          },
                          "page_peek": 10,
                          "padding": {
                            "top": 1,
                            "leading": 2,
                            "bottom": 3,
                            "trailing": 4
                          },
                          "margin": {
                            "top": 5,
                            "leading": 6,
                            "bottom": 7,
                            "trailing": 8
                          },
                          "shape": {
                            "type": "pill"
                          },
                          "page_spacing": 8,
                          "background_color": {
                            "light": {
                              "type": "alias",
                              "value": "tertiary"
                            },
                            "dark": {
                              "type": "alias",
                              "value": "another_alias"
                            }
                          },
                          "background": {
                            "type": "color",
                            "value": {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            }
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
                                "value": "primary"
                              }
                            },
                            "width": 123
                          },
                          "page_control": {
                            "position": "top",
                            "spacing": 5,
                            "margin": {
                              "top": 1,
                              "leading": 2,
                              "bottom": 3,
                              "trailing": 4
                            },
                            "padding": {
                              "top": 5,
                              "leading": 6,
                              "bottom": 7,
                              "trailing": 8
                            },
                            "background_color": {
                              "light": {
                                "type": "alias",
                                "value": "secondary"
                              }
                            },
                            "shape": {
                              "type": "pill"
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
                                  "value": "primary"
                                }
                              },
                              "width": 123
                            },
                            "active": {
                              "width": 10,
                              "height": 20,
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "primary"
                                },
                                "dark": {
                                  "type": "alias",
                                  "value": "secondary"
                                }
                              }
                            },
                            "default": {
                              "width": 30,
                              "height": 40,
                              "color": {
                                "light": {
                                  "type": "alias",
                                  "value": "tertiary"
                                },
                                "dark": {
                                  "type": "alias",
                                  "value": "another_alias"
                                }
                              }
                            }
                          },
                          "loop": true,
                          "auto_advance": {
                            "ms_time_per_page": 1000,
                            "ms_transition_time": 500,
                            "transition_type": "fade"
                          }
                        }
                        """.trimIndent(),
                        expected = PartialCarouselComponent(
                            visible = true,
                            initialPageIndex = 2,
                            pageAlignment = VerticalAlignment.CENTER,
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            pageSpacing = 8f,
                            backgroundColor = ColorScheme(
                                light = ColorInfo.Alias(ColorAlias("tertiary")),
                                dark = ColorInfo.Alias(ColorAlias("another_alias"))
                            ),
                            background = Background.Color(
                                value = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                            ),
                            pagePeek = 10,
                            padding = Padding(
                                top = 1.0,
                                leading = 2.0,
                                bottom = 3.0,
                                trailing = 4.0
                            ),
                            margin = Padding(
                                top = 5.0,
                                leading = 6.0,
                                bottom = 7.0,
                                trailing = 8.0
                            ),
                            shape = Shape.Pill,
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                radius = 20.1,
                                x = 23.6,
                                y = 45.2
                            ),
                            border = Border(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                                width = 123.0
                            ),
                            pageControl = CarouselComponent.PageControl(
                                position = CarouselComponent.PageControl.Position.TOP,
                                spacing = 5,
                                margin = Padding(
                                    top = 1.0,
                                    leading = 2.0,
                                    bottom = 3.0,
                                    trailing = 4.0
                                ),
                                padding = Padding(
                                    top = 5.0,
                                    leading = 6.0,
                                    bottom = 7.0,
                                    trailing = 8.0
                                ),
                                backgroundColor = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("secondary"))
                                ),
                                shape = Shape.Pill,
                                shadow = Shadow(
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                    radius = 20.1,
                                    x = 23.6,
                                    y = 45.2
                                ),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                                    width = 123.0
                                ),
                                active = CarouselComponent.PageControl.Indicator(
                                    width = 10u,
                                    height = 20u,
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary")),
                                        dark = ColorInfo.Alias(ColorAlias("secondary"))
                                    ),
                                ),
                                default = CarouselComponent.PageControl.Indicator(
                                    width = 30u,
                                    height = 40u,
                                    color = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("tertiary")),
                                        dark = ColorInfo.Alias(ColorAlias("another_alias"))
                                    ),
                                )
                            ),
                            loop = true,
                            autoAdvance = CarouselComponent.AutoAdvancePages(
                                msTimePerPage = 1000,
                                msTransitionTime = 500,
                                transitionType = CarouselComponent.AutoAdvancePages.TransitionType.FADE,
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
                        expected = PartialCarouselComponent()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialCarouselComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<PartialCarouselComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
