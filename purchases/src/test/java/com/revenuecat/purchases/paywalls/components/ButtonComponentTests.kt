package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ButtonComponentTests {

    @RunWith(Parameterized::class)
    public class DeserializeButtonComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        public class Args(
            @Language("json")
            public val json: String,
            val expected: ButtonComponent,
        )

        public companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "restore_purchases",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "restore_purchases"
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.RestorePurchases,
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
                    "navigate_back",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_back"
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateBack,
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
                    "navigate_to - customer_center",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "customer_center"
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.CustomerCenter
                            ),
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
                    "navigate_to - privacy_policy",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "privacy_policy",
                            "url": {
                              "url_lid": "ef54",
                              "method": "in_app_browser"
                            }
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.PrivacyPolicy(
                                    urlLid = LocalizationKey("ef54"),
                                    method = ButtonComponent.UrlMethod.IN_APP_BROWSER
                                )
                            ),
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
                    "navigate_to - terms",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "terms",
                            "url": {
                              "url_lid": "ef64",
                              "method": "external_browser"
                            }
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.Terms(
                                    urlLid = LocalizationKey("ef64"),
                                    method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER
                                )
                            ),
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
                    "navigate_to - url",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "url",
                            "url": {
                              "url_lid": "ef74",
                              "method": "deep_link"
                            }
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.Url(
                                    urlLid = LocalizationKey("ef74"),
                                    method = ButtonComponent.UrlMethod.DEEP_LINK
                                )
                            ),
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
                    "navigate_to - sheet",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "sheet",
                            "sheet": {
                              "id": "ef74",
                              "name": "my sheet",
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
                              "background_blur": true,
                              "size": {
                                "width": {
                                  "type": "fill",
                                  "value": null
                                },
                                "height": {
                                  "type": "fit",
                                  "value": null
                                }
                              }
                            }
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.Sheet(
                                    id = "ef74",
                                    name = "my sheet",
                                    stack = StackComponent(
                                        components = listOf(
                                            TextComponent(
                                                text = LocalizationKey("7bkohQjzIE"),
                                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                            )
                                        ),
                                    ),
                                    backgroundBlur = true,
                                    size = Size(
                                        width = SizeConstraint.Fill,
                                        height = SizeConstraint.Fit,
                                    )
                                )
                            ),
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
                    "navigate_to - unknown",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "some_unknown_destination",
                            "url": {
                              "url_lid": "ef74",
                              "method": "deep_link"
                            }
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.Unknown
                            ),
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
                    "navigate_to - url - unknown method",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "navigate_to",
                            "destination": "url",
                            "url": {
                              "url_lid": "ef74",
                              "method": "some_unknown_method"
                            }
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.NavigateTo(
                                destination = ButtonComponent.Destination.Url(
                                    urlLid = LocalizationKey("ef74"),
                                    method = ButtonComponent.UrlMethod.UNKNOWN
                                )
                            ),
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
                    "unknown",
                    Args(
                        json = """
                        {
                          "type": "button",
                          "action": {
                            "type": "unknown",
                            "unknown_property": "some_value"
                          },
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
                        expected = ButtonComponent(
                            action = ButtonComponent.Action.Unknown,
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
            )
        }

        @Test
        fun `Should properly deserialize ButtonComponent as ButtonComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ButtonComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize ButtonComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    internal class DeserializeUrlMethodTests(
        private val serialized: String,
        private val expected: ButtonComponent.UrlMethod,
    ) {

        public companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = ButtonComponent.UrlMethod.values().map { expected ->
                val serialized = when (expected) {
                    ButtonComponent.UrlMethod.IN_APP_BROWSER -> "\"in_app_browser\""
                    ButtonComponent.UrlMethod.EXTERNAL_BROWSER -> "\"external_browser\""
                    ButtonComponent.UrlMethod.DEEP_LINK -> "\"deep_link\""
                    // Testing this doesn't make a ton of sense, but it allows us to keep the exhaustive `when`.
                    ButtonComponent.UrlMethod.UNKNOWN -> "\"unknown\""
                }
                arrayOf(serialized, expected)
            } + listOf(
                arrayOf("\"some_unknown_method\"", ButtonComponent.UrlMethod.UNKNOWN),
            )
        }

        @Test
        fun `Should properly deserialize UrlMethod`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ButtonComponent.UrlMethod>(serialized)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    public class ActionSerializerTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        public class Args(
            @Language("json")
            public val serialized: String,
            val deserialized: ButtonComponent.Action,
        )

        public companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            public fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "restore_purchases",
                    Args(
                        serialized = """
                        {
                          "type": "restore_purchases"
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.RestorePurchases,
                    ),
                ),
                arrayOf(
                    "navigate_back",
                    Args(
                        serialized = """
                         {
                           "type": "navigate_back"
                         }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.NavigateBack,
                    ),
                ),
                arrayOf(
                    "navigate_to - customer_center",
                    Args(
                        serialized = """
                        {
                          "type": "navigate_to",
                          "destination": "customer_center"
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.NavigateTo(
                            destination = ButtonComponent.Destination.CustomerCenter
                        )
                    ),
                ),
                arrayOf(
                    "navigate_to - privacy_policy",
                    Args(
                        serialized = """
                        {
                          "type": "navigate_to",
                          "destination": "privacy_policy",
                          "url": {
                            "url_lid": "ef54",
                            "method": "in_app_browser"
                          }
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.NavigateTo(
                            destination = ButtonComponent.Destination.PrivacyPolicy(
                                urlLid = LocalizationKey("ef54"),
                                method = ButtonComponent.UrlMethod.IN_APP_BROWSER
                            )
                        ),
                    ),
                ),
                arrayOf(
                    "navigate_to - terms",
                    Args(
                        serialized = """
                        {
                          "type": "navigate_to",
                          "destination": "terms",
                          "url": {
                            "url_lid": "ef64",
                            "method": "external_browser"
                          }
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.NavigateTo(
                            destination = ButtonComponent.Destination.Terms(
                                urlLid = LocalizationKey("ef64"),
                                method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER
                            )
                        )
                    ),
                ),
                arrayOf(
                    "navigate_to - url",
                    Args(
                        serialized = """
                        {
                          "type": "navigate_to",
                          "destination": "url",
                          "url": {
                            "url_lid": "ef74",
                            "method": "deep_link"
                          }
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.NavigateTo(
                            destination = ButtonComponent.Destination.Url(
                                urlLid = LocalizationKey("ef74"),
                                method = ButtonComponent.UrlMethod.DEEP_LINK
                            )
                        )
                    ),
                ),
                arrayOf(
                    "navigate_to - sheet",
                    Args(
                        serialized = """
                        {
                          "type": "navigate_to",
                          "destination": "sheet",
                          "sheet": {
                            "id": "ef74",
                            "name": "my sheet",
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
                            "background_blur": true,
                            "size": {
                              "width": {
                                "type": "fill",
                                "value": null
                              },
                              "height": {
                                "type": "fit",
                                "value": null
                              }
                            }
                          }
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.NavigateTo(
                            destination = ButtonComponent.Destination.Sheet(
                                id = "ef74",
                                name = "my sheet",
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("7bkohQjzIE"),
                                            color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                        )
                                    ),
                                ),
                                backgroundBlur = true,
                                size = Size(
                                    width = SizeConstraint.Fill,
                                    height = SizeConstraint.Fit,
                                )
                            )
                        )
                    ),
                ),
                arrayOf(
                    "unknown",
                    Args(
                        serialized = """
                        {
                          "type": "some_unknown_type",
                          "unknown_property": "some_value"
                        }
                        """.trimIndent(),
                        deserialized = ButtonComponent.Action.Unknown,
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize Action`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ButtonComponent.Action>(args.serialized)

            // Assert
            assert(actual == args.deserialized)
        }
    }
}
