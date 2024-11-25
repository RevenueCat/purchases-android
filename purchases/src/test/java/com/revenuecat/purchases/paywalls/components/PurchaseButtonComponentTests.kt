package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.common.OfferingParser
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
        val json: String,
        val expected: PurchaseButtonComponent,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
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
                                    text = "7bkohQjzIE",
                                    color = ColorScheme(light = ColorInfo.Alias("primary"))
                                )
                            ),
                        )
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
        val actual = OfferingParser.json.decodeFromString<PurchaseButtonComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }

    @Test
    fun `Should properly deserialize PurchaseButtonComponent as PaywallComponent`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
