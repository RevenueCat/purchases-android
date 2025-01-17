package com.revenuecat.purchases

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.parseRGBAColor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class UiConfigTests {

    @Test
    fun `Should properly deserialize UiConfig`() {
        // Arrange
        // language=json
        val serialized = """
            {
              "app": {
                "colors": {
                  "primary": {
                    "light": {
                      "type": "hex",
                      "value": "#ffcc00"
                    }
                  },
                  "secondary": {
                    "light": {
                      "type": "linear",
                      "degrees": 45,
                      "points": [
                        {
                          "color": "#032400ff",
                          "percent": 0
                        },
                        {
                          "color": "#090979ff",
                          "percent": 35
                        },
                        {
                          "color": "#216c32ff",
                          "percent": 100
                        }
                      ]
                    }
                  },
                  "tertiary": {
                    "light": {
                      "type": "radial",
                      "points": [
                        {
                          "color": "#032400ff",
                          "percent": 0
                        },
                        {
                          "color": "#090979ff",
                          "percent": 35
                        },
                        {
                          "color": "#216c32ff",
                          "percent": 100
                        }
                      ]
                    }
                  }
                },
                "fonts": {
                  "primary": {
                    "ios": {
                      "type": "name",
                      "value": "SF Pro"
                    },
                    "android": {
                      "type": "name",
                      "value": "Roboto"
                    },
                    "web": {
                      "type": "google_fonts",
                      "value": "Gothic"
                    }
                  }
                }
              },
              "localizations": {
                "en_US": {
                  "monthly": "monthly"
                },
                "es_ES": {
                  "monthly": "mensual"
                }
              },
              "variable_config": {
                "variable_compatibility_map": {
                  "new var": "guaranteed var"
                },
                "function_compatibility_map": {
                  "new fun": "guaranteed fun"
                }
              }
            }
            """.trimIndent()
        val expected = UiConfig(
            app = UiConfig.AppConfig(
                colors = mapOf(
                    ColorAlias("primary") to ColorScheme(
                        light = ColorInfo.Hex(parseRGBAColor("#ffcc00")),
                    ),
                    ColorAlias("secondary") to ColorScheme(
                        light = ColorInfo.Gradient.Linear(
                            degrees = 45f,
                            points = listOf(
                                ColorInfo.Gradient.Point(
                                    color = parseRGBAColor("#032400ff"),
                                    percent = 0f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = parseRGBAColor("#090979ff"),
                                    percent = 35f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = parseRGBAColor("#216c32ff"),
                                    percent = 100f,
                                )
                            )
                        )
                    ),
                    ColorAlias("tertiary") to ColorScheme(
                        light = ColorInfo.Gradient.Radial(
                            points = listOf(
                                ColorInfo.Gradient.Point(
                                    color = parseRGBAColor("#032400ff"),
                                    percent = 0f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = parseRGBAColor("#090979ff"),
                                    percent = 35f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = parseRGBAColor("#216c32ff"),
                                    percent = 100f,
                                )
                            )
                        )
                    )
                ),
                fonts = mapOf(
                    FontAlias("primary") to UiConfig.AppConfig.FontsConfig(
                        android = UiConfig.AppConfig.FontsConfig.FontInfo.Name("Roboto"),
                    )
                )
            ),
            localizations = mapOf(
                LocaleId("en_US") to mapOf(
                    VariableLocalizationKey.MONTHLY to "monthly"
                ),
                LocaleId("es_ES") to mapOf(
                    VariableLocalizationKey.MONTHLY to "mensual"
                )
            ),
            variableConfig = UiConfig.VariableConfig(
                variableCompatibilityMap = mapOf(
                    "new var" to "guaranteed var"
                ),
                functionCompatibilityMap = mapOf(
                    "new fun" to "guaranteed fun"
                )
            )
        )

        // Act
        val actual = OfferingParser.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }
}
