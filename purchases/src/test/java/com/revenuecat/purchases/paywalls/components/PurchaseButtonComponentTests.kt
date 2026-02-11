package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PurchaseButtonComponentTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        public val json: String,
        public val expected: PurchaseButtonComponent,
    )

    public companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = listOf(
            arrayOf(
                "non-empty stack",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
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
                    expected = PurchaseButtonComponent(
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
                "action - in_app_checkout",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "action": "in_app_checkout"
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        action = PurchaseButtonComponent.Action.IN_APP_CHECKOUT
                    )
                ),
            ),
            arrayOf(
                "action - web_checkout",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "action": "web_checkout"
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        action = PurchaseButtonComponent.Action.WEB_CHECKOUT
                    )
                ),
            ),
            arrayOf(
                "action - web_product_selection",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "action": "web_product_selection"
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        action = PurchaseButtonComponent.Action.WEB_PRODUCT_SELECTION
                    )
                ),
            ),
            arrayOf(
                "method - in_app_checkout",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "in_app_checkout"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.InAppCheckout
                    )
                ),
            ),
            arrayOf(
                "method - web_checkout - no params",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "web_checkout"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.WebCheckout()
                    )
                ),
            ),
            arrayOf(
                "method - web_checkout - in_app_browser",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "web_checkout",
                            "auto_dismiss": true,
                            "open_method": "in_app_browser"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.WebCheckout(
                            autoDismiss = true,
                            openMethod = ButtonComponent.UrlMethod.IN_APP_BROWSER,
                        )
                    )
                ),
            ),
            arrayOf(
                "method - web_checkout - external_browser",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "web_checkout",
                            "auto_dismiss": true,
                            "open_method": "external_browser"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.WebCheckout(
                            autoDismiss = true,
                            openMethod = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                        )
                    )
                ),
            ),
            arrayOf(
                "method - web_product_selection - no params",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "web_product_selection"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.WebProductSelection()
                    )
                ),
            ),
            arrayOf(
                "method - web_product_selection - in_app_browser",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "web_product_selection",
                            "auto_dismiss": true,
                            "open_method": "in_app_browser"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.WebProductSelection(
                            autoDismiss = true,
                            openMethod = ButtonComponent.UrlMethod.IN_APP_BROWSER,
                        )
                    )
                ),
            ),
            arrayOf(
                "method - custom_web_checkout",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "custom_web_checkout",
                            "custom_url": {
                               "url_lid": "123",
                               "package_param": "rc_package" 
                            },
                            "auto_dismiss": true,
                            "open_method": "in_app_browser"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.CustomWebCheckout(
                            customUrl = PurchaseButtonComponent.CustomUrl(
                                urlLid = LocalizationKey("123"),
                                packageParam = "rc_package",
                            ),
                            autoDismiss = true,
                            openMethod = ButtonComponent.UrlMethod.IN_APP_BROWSER,
                        )
                    )
                ),
            ),
            arrayOf(
                "method - unknown",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          },
                          "method": {
                            "type": "invalid_method"
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(components = listOf()),
                        method = PurchaseButtonComponent.Method.Unknown,
                    )
                ),
            ),
            arrayOf(
                "empty stack",
                Args(
                    json = """
                        {
                          "type": "purchase_button",
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                    expected = PurchaseButtonComponent(
                        stack = StackComponent(
                            components = emptyList(),
                        )
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PurchaseButtonComponent as PurchaseButtonComponent`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PurchaseButtonComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }

    @Test
    fun `Should properly deserialize PurchaseButtonComponent as PaywallComponent`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
