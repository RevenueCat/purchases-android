package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PackageComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: PackageComponent,
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
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
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
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
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
                "empty stack",
                Args(
                    json = """
                        {
                          "type": "package",
                          "package_id": "${"$"}rc_weekly",
                          "is_selected_by_default": true,
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                    expected = PackageComponent(
                        packageId = "${"$"}rc_weekly",
                        isSelectedByDefault = true,
                        stack = StackComponent(
                            components = emptyList(),
                        )
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PackageComponent as PackageComponent`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<PackageComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }

    @Test
    fun `Should properly deserialize PackageComponent as PaywallComponent`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
