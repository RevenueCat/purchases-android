package com.revenuecat.purchases

import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
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
                      "value": "Roboto",
                      "url": "https://example.com/testfont123.ttf",
                      "hash": "123abc",
                      "weight": 400,
                      "style": "italic",
                      "family": "custom-family"
                    }
                  },
                  "secondary": {
                    "ios": {
                      "type": "name",
                      "value": "Roboto"
                    },
                    "android": {
                      "type": "google_fonts",
                      "value": "Gothic"
                    },
                    "family": "custom-family2",
                    "weight": "regular",
                    "style": "normal"
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
                        android = UiConfig.AppConfig.FontsConfig.FontInfo.Name(
                            value = "Roboto",
                            url = "https://example.com/testfont123.ttf",
                            hash = "123abc",
                            family = "custom-family",
                            weight = 400,
                            style = FontStyle.ITALIC,
                        ),
                    ),
                    FontAlias("secondary") to UiConfig.AppConfig.FontsConfig(
                        android = UiConfig.AppConfig.FontsConfig.FontInfo.GoogleFonts("Gothic"),
                    ),
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
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should ignore unknown VariableLocalizationKeys`() {
        // Arrange
        // language=json
        val serialized = """
            {
              "localizations": {
                "en_US": {
                  "monthly": "monthly",
                  "a_very_futuristic_key_we_dont_know_about": "unknown"
                },
                "es_ES": {
                  "monthly": "mensual",
                  "a_very_futuristic_key_we_dont_know_about": "desconocido"
                }
              }
            }
            """.trimIndent()
        val expected = UiConfig(
            localizations = mapOf(
                LocaleId("en_US") to mapOf(
                    VariableLocalizationKey.MONTHLY to "monthly"
                ),
                LocaleId("es_ES") to mapOf(
                    VariableLocalizationKey.MONTHLY to "mensual"
                )
            ),
        )

        // Act
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should deserialize all known VariableLocalizationKeys`() {
        // Arrange
        // When new VariableLocalizationKeys are added, this test will fail. Make it pass by adding the new keys to the
        // `serialized` JSON below.
        // language=json
        val serialized = """
            {
              "localizations": {
                "en_US": {
                  "day": "DAY",
                  "daily": "DAILY",
                  "day_short": "DAY_SHORT",
                  "week": "WEEK",
                  "weekly": "WEEKLY",
                  "week_short": "WEEK_SHORT",
                  "month": "MONTH",
                  "monthly": "MONTHLY",
                  "month_short": "MONTH_SHORT",
                  "year": "YEAR",
                  "yearly": "YEARLY",
                  "year_short": "YEAR_SHORT",
                  "annual": "ANNUAL",
                  "annually": "ANNUALLY",
                  "annual_short": "ANNUAL_SHORT",
                  "lifetime": "LIFETIME",
                  "free_price": "FREE_PRICE",
                  "percent": "PERCENT",
                  "num_day_zero": "NUM_DAY_ZERO",
                  "num_day_one": "NUM_DAY_ONE",
                  "num_day_two": "NUM_DAY_TWO",
                  "num_day_few": "NUM_DAY_FEW",
                  "num_day_many": "NUM_DAY_MANY",
                  "num_day_other": "NUM_DAY_OTHER",
                  "num_week_zero": "NUM_WEEK_ZERO",
                  "num_week_one": "NUM_WEEK_ONE",
                  "num_week_two": "NUM_WEEK_TWO",
                  "num_week_few": "NUM_WEEK_FEW",
                  "num_week_many": "NUM_WEEK_MANY",
                  "num_week_other": "NUM_WEEK_OTHER",
                  "num_month_zero": "NUM_MONTH_ZERO",
                  "num_month_one": "NUM_MONTH_ONE",
                  "num_month_two": "NUM_MONTH_TWO",
                  "num_month_few": "NUM_MONTH_FEW",
                  "num_month_many": "NUM_MONTH_MANY",
                  "num_month_other": "NUM_MONTH_OTHER",
                  "num_year_zero": "NUM_YEAR_ZERO",
                  "num_year_one": "NUM_YEAR_ONE",
                  "num_year_two": "NUM_YEAR_TWO",
                  "num_year_few": "NUM_YEAR_FEW",
                  "num_year_many": "NUM_YEAR_MANY",
                  "num_year_other": "NUM_YEAR_OTHER",
                  "num_days_short": "NUM_DAYS_SHORT",
                  "num_weeks_short": "NUM_WEEKS_SHORT",
                  "num_months_short": "NUM_MONTHS_SHORT",
                  "num_years_short": "NUM_YEARS_SHORT"
                }
              }
            }
            """.trimIndent()
        val expected = UiConfig(
            localizations = mapOf(
                LocaleId("en_US") to VariableLocalizationKey.values().associateWith { key -> key.name },
            ),
        )

        // Act
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should properly deserialize custom_variables with native types`() {
        // Arrange
        // Backend only sends string, number, and boolean types
        // language=json
        val serialized = """
            {
              "custom_variables": {
                "user_name": {
                  "type": "string",
                  "default_value": "Friend"
                },
                "discount_percent": {
                  "type": "number",
                  "default_value": 20.5
                },
                "max_items": {
                  "type": "number",
                  "default_value": 100
                },
                "is_premium": {
                  "type": "boolean",
                  "default_value": true
                }
              }
            }
            """.trimIndent()

        // Act
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual.customVariables).hasSize(4)

        val userName = actual.customVariables["user_name"]!!
        assertThat(userName.type).isEqualTo("string")
        assertThat(userName.defaultValue).isEqualTo("Friend")

        val discountPercent = actual.customVariables["discount_percent"]!!
        assertThat(discountPercent.type).isEqualTo("number")
        assertThat(discountPercent.defaultValue).isEqualTo(20.5)

        val maxItems = actual.customVariables["max_items"]!!
        assertThat(maxItems.type).isEqualTo("number")
        assertThat(maxItems.defaultValue).isEqualTo(100.0) // Integer values are deserialized as Double

        val isPremium = actual.customVariables["is_premium"]!!
        assertThat(isPremium.type).isEqualTo("boolean")
        assertThat(isPremium.defaultValue).isEqualTo(true)
    }

    @Test
    fun `Should properly deserialize custom_variables with string values falling back to string conversion`() {
        // When values are provided as strings but typed as non-strings, they should be converted
        // Arrange
        // language=json
        val serialized = """
            {
              "custom_variables": {
                "discount_percent": {
                  "type": "number",
                  "default_value": "20.5"
                },
                "is_premium": {
                  "type": "boolean",
                  "default_value": "true"
                }
              }
            }
            """.trimIndent()

        // Act
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        val discountPercent = actual.customVariables["discount_percent"]!!
        assertThat(discountPercent.type).isEqualTo("number")
        assertThat(discountPercent.defaultValue).isEqualTo(20.5) // Parsed from string

        val isPremium = actual.customVariables["is_premium"]!!
        assertThat(isPremium.type).isEqualTo("boolean")
        assertThat(isPremium.defaultValue).isEqualTo(true) // Parsed from string
    }

    @Test
    fun `Should deserialize empty custom_variables`() {
        // Arrange
        // language=json
        val serialized = """
            {
              "custom_variables": {}
            }
            """.trimIndent()
        val expected = UiConfig(
            customVariables = emptyMap(),
        )

        // Act
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should default custom_variables to empty map when not present`() {
        // Arrange
        // language=json
        val serialized = """
            {
              "app": {}
            }
            """.trimIndent()

        // Act
        val actual = JsonTools.json.decodeFromString<UiConfig>(serialized)

        // Assert
        assertThat(actual.customVariables).isEmpty()
    }
}
