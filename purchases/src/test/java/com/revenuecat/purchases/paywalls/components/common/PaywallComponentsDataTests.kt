package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@RunWith(Parameterized::class)
internal class PaywallComponentsDataTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        val json: String,
        val expected: PaywallComponentsData,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "revision present",
                Args(
                    json = """
                        {
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "components": []
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
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US",
                          "revision": 123
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias("primary")
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 123
                    )
                ),
            ),
            arrayOf(
                "revision absent",
                Args(
                    json = """
                        {
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "components": []
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
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US"
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias("primary")
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 0
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PaywallComponentsData`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<PaywallComponentsData>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}