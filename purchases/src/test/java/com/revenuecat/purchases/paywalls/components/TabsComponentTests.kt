package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class TabsComponentTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        val json: String,
        val expected: PaywallComponent,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "tab_control_button",
                Args(
                    json = """
                        {
                          "type": "tab_control_button",
                          "tab_index": 0,
                          "stack": {
                            "type": "stack",
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
                            ]
                          }
                        }
                        """.trimIndent(),
                    expected = TabControlButtonComponent(
                        tabIndex = 0,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("7bkohQjzIE"),
                                    color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                )
                            ),
                        )
                    )
                ),
            ),
            arrayOf(
                "tab_control_toggle",
                Args(
                    json = """
                        {
                          "type": "tab_control_toggle",
                          "default_value": true,
                          "thumb_color_on": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "thumb_color_off": {
                            "light": {
                              "type": "alias",
                              "value": "secondary"
                            }
                          },
                          "track_color_on": {
                            "light": {
                              "type": "alias",
                              "value": "tertiary"
                            }
                          },
                          "track_color_off": {
                            "light": {
                              "type": "alias",
                              "value": "primary_alt"
                            }
                          }
                        }
                        """.trimIndent(),
                    expected = TabControlToggleComponent(
                        defaultValue = true,
                        thumbColorOn = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                        thumbColorOff = ColorScheme(light = ColorInfo.Alias(ColorAlias("secondary"))),
                        trackColorOn = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                        trackColorOff = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary_alt"))),
                    )
                ),
            ),
            arrayOf(
                "tab_control",
                Args(
                    json = """
                        {
                          "type": "tab_control"
                        }
                        """.trimIndent(),
                    expected = TabControlComponent,
                ),
            ),
            arrayOf(
                "stack with tab_control",
                Args(
                    json = """
                        {
                          "type": "stack",
                          "components": [
                            {
                              "type": "tab_control"
                            }
                          ]
                        }
                        """.trimIndent(),
                    expected = StackComponent(
                        components = listOf(
                            TabControlComponent,
                        ),
                    )
                ),
            ),
            arrayOf(
                "tabs - all values present",
                Args(
                    json = """
                        {
                          "type": "tabs",
                          "control": {
                            "type": "buttons",
                            "stack": {
                              "type": "stack",
                              "components": [
                                {
                                  "type": "tab_control_button",
                                  "tab_index": 0,
                                  "stack": {
                                    "components": []
                                  }
                                },
                                {
                                  "type": "tab_control_button",
                                  "tab_index": 1,
                                  "stack": {
                                    "components": []
                                  }
                                }
                              ]
                            }
                          },
                          "tabs": [
                            {
                              "stack": {
                                "components": [
                                  {
                                    "type": "tab_control"
                                  }
                                ]
                              }
                            },
                            {
                              "stack": {
                                "components": [
                                  {
                                    "type": "tab_control"
                                  }
                                ]
                              }
                            }
                          ],
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
                          "id": "WLbwQoNUKF",
                          "margin": {
                            "bottom": 10,
                            "leading": 12,
                            "top": 14,
                            "trailing": 16
                          },
                          "name": "Tabs",
                          "padding": {
                            "bottom": 16,
                            "leading": 14,
                            "top": 12,
                            "trailing": 10
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
                          }
                        }
                        """.trimIndent(),
                    expected = TabsComponent(
                        control = TabsComponent.TabControl.Buttons(
                            stack = StackComponent(
                                components = listOf(
                                    TabControlButtonComponent(
                                        tabIndex = 0,
                                        stack = StackComponent(components = emptyList())
                                    ),
                                    TabControlButtonComponent(
                                        tabIndex = 1,
                                        stack = StackComponent(components = emptyList())
                                    ),
                                )
                            )
                        ),
                        tabs = listOf(
                            TabsComponent.Tab(stack = StackComponent(components = listOf(TabControlComponent))),
                            TabsComponent.Tab(stack = StackComponent(components = listOf(TabControlComponent))),
                        ),
                        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                        backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("secondary"))),
                        padding = Padding(top = 12.0, leading = 14.0, bottom = 16.0, trailing = 10.0),
                        margin = Padding(top = 14.0, leading = 12.0, bottom = 10.0, trailing = 16.0),
                        shape = Shape.Pill,
                        border = Border(
                            color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                            width = 123.0
                        ),
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
                "tabs - optional values absent",
                Args(
                    json = """
                        {
                          "type": "tabs",
                          "control": {
                            "type": "toggle",
                            "stack": {
                              "type": "stack",
                              "components": [
                                {
                                  "type": "tab_control_toggle",
                                  "default_value": true,
                                  "thumb_color_on": {
                                    "light": {
                                      "type": "alias",
                                      "value": "primary"
                                    }
                                  },
                                  "thumb_color_off": {
                                    "light": {
                                      "type": "alias",
                                      "value": "secondary"
                                    }
                                  },
                                  "track_color_on": {
                                    "light": {
                                      "type": "alias",
                                      "value": "tertiary"
                                    }
                                  },
                                  "track_color_off": {
                                    "light": {
                                      "type": "alias",
                                      "value": "primary_alt"
                                    }
                                  }
                                }
                              ]
                            }
                          },
                          "tabs": [
                            {
                              "stack": {
                                "components": [
                                  {
                                    "type": "tab_control"
                                  }
                                ]
                              }
                            },
                            {
                              "stack": {
                                "components": [
                                  {
                                    "type": "tab_control"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                    expected = TabsComponent(
                        control = TabsComponent.TabControl.Toggle(
                            stack = StackComponent(
                                components = listOf(
                                    TabControlToggleComponent(
                                        defaultValue = true,
                                        thumbColorOn = ColorScheme(ColorInfo.Alias(ColorAlias("primary"))),
                                        thumbColorOff = ColorScheme(ColorInfo.Alias(ColorAlias("secondary"))),
                                        trackColorOn = ColorScheme(ColorInfo.Alias(ColorAlias("tertiary"))),
                                        trackColorOff = ColorScheme(ColorInfo.Alias(ColorAlias("primary_alt")))
                                    )
                                )
                            )
                        ),
                        tabs = listOf(
                            TabsComponent.Tab(stack = StackComponent(components = listOf(TabControlComponent))),
                            TabsComponent.Tab(stack = StackComponent(components = listOf(TabControlComponent))),
                        ),
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize as PaywallComponent`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
