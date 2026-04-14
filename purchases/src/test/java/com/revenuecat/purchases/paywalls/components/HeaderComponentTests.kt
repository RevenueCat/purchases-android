package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class HeaderComponentTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        val json: String,
        val expected: HeaderComponent,
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
                          "type": "header",
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
                    expected = HeaderComponent(
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
                          "type": "header",
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                    expected = HeaderComponent(
                        stack = StackComponent(
                            components = emptyList(),
                        )
                    )
                ),
            ),
            arrayOf(
                "extra fields ignored",
                Args(
                    json = """
                        {
                          "type": "header",
                          "id": "header_1",
                          "name": "My Header",
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                    expected = HeaderComponent(
                        stack = StackComponent(
                            components = emptyList(),
                        )
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize HeaderComponent as HeaderComponent`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<HeaderComponent>(args.json)

        // Assert
        assertThat(actual).isEqualTo(args.expected)
    }

    @Test
    fun `Should properly deserialize HeaderComponent as PaywallComponent`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)

        // Assert
        assertThat(actual).isEqualTo(args.expected)
    }
}
