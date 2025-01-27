package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import kotlinx.serialization.encodeToString
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ButtonComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeButtonComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ButtonComponent,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
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
            )
        }

        @Test
        fun `Should properly deserialize ButtonComponent as ButtonComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ButtonComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize ButtonComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    internal class DeserializeUrlMethodTests(
        private val serialized: String,
        private val expected: ButtonComponent.UrlMethod,
    ) {

        companion object {
            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = ButtonComponent.UrlMethod.values().map { expected ->
                val serialized = when (expected) {
                    ButtonComponent.UrlMethod.IN_APP_BROWSER -> "\"in_app_browser\""
                    ButtonComponent.UrlMethod.EXTERNAL_BROWSER -> "\"external_browser\""
                    ButtonComponent.UrlMethod.DEEP_LINK -> "\"deep_link\""
                }
                arrayOf(serialized, expected)
            }
        }

        @Test
        fun `Should properly deserialize UrlMethod`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ButtonComponent.UrlMethod>(serialized)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    class ActionSerializerTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val serialized: String,
            val deserialized: ButtonComponent.Action,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
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
            )
        }

        @Test
        fun `Should properly deserialize Action`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ButtonComponent.Action>(args.serialized)

            // Assert
            assert(actual == args.deserialized)
        }

        // We don't actually need serialization for our functionality, but it helps when debugging the custom
        // ActionSerializer.
        @Test
        fun `Should properly serialize Action`() {
            // Arrange, Act
            val actual = OfferingParser.json.encodeToString(args.deserialized)
            // Serialization removes all white space.
            val expected = args.serialized.filterNot { it.isWhitespace() }

            // Assert
            assert(actual == expected)
        }
    }
}
