package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ComponentsConfigTests {

    class DeserializeComponentsConfigTests {

        @Test
        fun `Should properly deserialize ComponentsConfig`() {
            // Arrange
            @Language("json")
            val json = """
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
                    }
                  }
                }
            """.trimIndent()
            val expected = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = "7bkohQjzIE",
                                color = ColorScheme(light = ColorInfo.Alias("primary"))
                            )
                        ),
                    )
                )
            )

            // Act
            val actual = OfferingParser.json.decodeFromString<ComponentsConfig>(json)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePaywallComponentsConfigTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PaywallComponentsConfig,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
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
                                        text = "7bkohQjzIE",
                                        color = ColorScheme(light = ColorInfo.Alias("primary"))
                                    )
                                ),
                            ),
                            stickyFooter = StickyFooterComponent(
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = "7bkohQjzIE",
                                            color = ColorScheme(light = ColorInfo.Alias("primary"))
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
                          }
                        }
                        """.trimIndent(),
                        expected = PaywallComponentsConfig(
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
                          "sticky_footer": null
                        }
                        """.trimIndent(),
                        expected = PaywallComponentsConfig(
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
            )
        }

        @Test
        fun `Should properly deserialize PaywallComponentsConfig`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponentsConfig>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
