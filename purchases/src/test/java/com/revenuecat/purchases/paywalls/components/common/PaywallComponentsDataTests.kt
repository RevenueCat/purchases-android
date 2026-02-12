package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
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
                          "id": "paywall_id",
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "badge": {
                                  "stack": {
                                    "type": "stack",
                                    "components": []
                                  },
                                  "style": "overlay",
                                  "alignment": "bottom_trailing"
                                },
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
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList(),
                                    badge = Badge(
                                        stack = StackComponent(
                                            components = emptyList()
                                        ),
                                        style = Badge.Style.Overlay,
                                        alignment = TwoDimensionalAlignment.BOTTOM_TRAILING
                                    ),
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
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
                          "id": "paywall_id",
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
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
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
            arrayOf(
                "id absent",
                Args(
                    json = """
                        {
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "revision": 5,
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
                        id = null,
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
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
                        revision = 5
                    )
                ),
            ),
            arrayOf(
                "with zero_decimal_place_countries",
                Args(
                    json = """
                {
                  "id": "paywall_id",
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
                  "zero_decimal_place_countries": {
                    "apple": [
                      "TWN",
                      "KAZ",
                      "MEX",
                      "PHL",
                      "THA"
                    ],
                    "google": [
                      "TW",
                      "KZ",
                      "MX",
                      "PH",
                      "TH"
                    ]
                  }
                }""".trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
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
                        revision = 0,
                        zeroDecimalPlaceCountries = listOf(
                            "TW",
                            "KZ",
                            "MX",
                            "PH",
                            "TH",
                        ),
                    )
                ),
            ),
            arrayOf(
                "with exit_offers",
                Args(
                    json = """
                {
                  "id": "paywall_id",
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
                  "exit_offers": {
                    "dismiss": {
                      "offering_id": "exit-offering-id"
                    }
                  }
                }""".trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
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
                        revision = 0,
                        exitOffers = ExitOffers(
                            dismiss = ExitOffer(
                                offeringId = "exit-offering-id",
                            ),
                        ),
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PaywallComponentsData`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PaywallComponentsData>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
