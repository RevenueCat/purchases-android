package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ComponentsConfigTests {

    public class DeserializeComponentsConfigTests {

        @Test
        fun `Should properly deserialize ComponentsConfig`() {
            // Arrange
            @Language("json")
            public val json = """
                {
                  "base": {
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
                    },
                    "background": {
                      "type": "color",
                      "value": {
                        "light": {
                          "type": "alias",
                          "value": "primary"
                        }
                      }
                    }
                  }
                }
            """.trimIndent()
            val expected = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("7bkohQjzIE"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                            )
                        ),
                    ),
                    background = Background.Color(
                        value = ColorScheme(
                            light = ColorInfo.Alias(ColorAlias("primary"))
                        )
                    )
                )
            )

            // Act
            val actual = JsonTools.json.decodeFromString<ComponentsConfig>(json)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    public class DeserializePaywallComponentsConfigTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        public class Args(
            @Language("json")
            public val json: String,
            val expected: PaywallComponentsConfig,
        )

        public companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "sticky footer present",
                    Args(
                        json = """
                        {
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
                          "sticky_footer": {
                            "type": "sticky_footer",
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
                        }
                        """.trimIndent(),
                        expected = PaywallComponentsConfig(
                            stack = StackComponent(
                                components = listOf(
                                    TextComponent(
                                        text = LocalizationKey("7bkohQjzIE"),
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                    )
                                ),
                            ),
                            background = Background.Color(
                                value = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("primary"))
                                )
                            ),
                            stickyFooter = StickyFooterComponent(
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("7bkohQjzIE"),
                                            color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                        )
                                    ),
                                )
                            )
                        )
                    ),
                ),
                arrayOf(
                    "sticky footer absent",
                    Args(
                        json = """
                        {
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
                          },
                          "background": {
                            "type": "color",
                            "value": {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                        expected = PaywallComponentsConfig(
                            stack = StackComponent(
                                components = listOf(
                                    TextComponent(
                                        text = LocalizationKey("7bkohQjzIE"),
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                    )
                                ),
                            ),
                            background = Background.Color(
                                value = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("primary"))
                                )
                            )
                        )
                    ),
                ),
                arrayOf(
                    "sticky footer null",
                    Args(
                        json = """
                        {
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
                          "sticky_footer": null
                        }
                        """.trimIndent(),
                        expected = PaywallComponentsConfig(
                            stack = StackComponent(
                                components = listOf(
                                    TextComponent(
                                        text = LocalizationKey("7bkohQjzIE"),
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                    )
                                ),
                            ),
                            background = Background.Color(
                                value = ColorScheme(
                                    light = ColorInfo.Alias(ColorAlias("primary"))
                                )
                            )
                        )
                    ),
                ),
                arrayOf(
                    "unknown background type",
                    Args(
                        json = """
                        {
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
                          },
                          "background": {
                            "type": "some_unknown_background_type_that_doesnt_exist_in_the_paywall_data_model_yet",
                            "unknown_property": "some_value"
                          }
                        }
                        """.trimIndent(),
                        expected = PaywallComponentsConfig(
                            stack = StackComponent(
                                components = listOf(
                                    TextComponent(
                                        text = LocalizationKey("7bkohQjzIE"),
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                    )
                                ),
                            ),
                            background = Background.Unknown(
                                type = "some_unknown_background_type_that_doesnt_exist_in_the_paywall_data_model_yet"
                            ),
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize PaywallComponentsConfig`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<PaywallComponentsConfig>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
