package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.colorInt
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class TextComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeTextComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: TextComponent,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all values present",
                    Args(
                        json = """
                        {
                          "background_color": {
                            "light": {
                              "type": "hex",
                              "value": "#eeeeee"
                            }
                          },
                          "color": {
                            "light": {
                              "type": "hex",
                              "value": "#000000"
                            }
                          },
                          "components": [],
                          "font_name": "some font",
                          "font_size": "body_l",
                          "font_weight": "bold",
                          "horizontal_alignment": "leading",
                          "id": "xmpgCrN9Rb",
                          "margin": {
                            "top": 4,
                            "bottom": 8,
                            "leading": 6,
                            "trailing": 2
                          },
                          "name": "Text",
                          "padding": {
                            "top": 12,
                            "bottom": 16,
                            "leading": 14,
                            "trailing": 10
                          },
                          "size": {
                            "height": {
                              "type": "fill",
                              "value": null
                            },
                            "width": {
                              "type": "fill",
                              "value": null
                            }
                          },
                          "text_lid": "7bkohQjzIE",
                          "type": "text"
                        }
                        """.trimIndent(),
                        expected = TextComponent(
                            text = LocalizationKey("7bkohQjzIE"),
                            color = ColorScheme(
                                light = ColorInfo.Hex(colorInt(alpha = 0xff, red = 0, green = 0, blue = 0))
                            ),
                            backgroundColor = ColorScheme(
                                light = ColorInfo.Hex(colorInt(alpha = 0xff, red = 0xee, green = 0xee, blue = 0xee))
                            ),
                            fontName = "some font",
                            fontWeight = FontWeight.BOLD,
                            fontSize = FontSize.BODY_L,
                            horizontalAlignment = HorizontalAlignment.LEADING,
                            size = Size(height = SizeConstraint.Fill, width = SizeConstraint.Fill),
                            padding = Padding(top = 12.0, bottom = 16.0, leading = 14.0, trailing = 10.0),
                            margin = Padding(top = 4.0, bottom = 8.0, leading = 6.0, trailing = 2.0),
                        )
                    )
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "color": {
                            "light": {
                              "type": "hex",
                              "value": "#ffffff"
                            }
                          },
                          "components": [],
                          "id": "xmpgCrN9Rb",
                          "name": "Text",
                          "text_lid": "7bkohQjzIE",
                          "type": "text"
                        }
                        """.trimIndent(),
                        expected = TextComponent(
                            text = LocalizationKey("7bkohQjzIE"),
                            color = ColorScheme(
                                light = ColorInfo.Hex(colorInt(alpha = 0xff, red = 0xff, green = 0xff, blue = 0xff))
                            ),
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize TextComponent as TextComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<TextComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize TextComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialTextComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialTextComponent,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "all values present",
                    Args(
                        json = """
                        {
                          "visible": false,
                          "background_color": {
                            "light": {
                              "type": "hex",
                              "value": "#eeeeee"
                            }
                          },
                          "color": {
                            "light": {
                              "type": "hex",
                              "value": "#000000"
                            }
                          },
                          "font_name": "some font",
                          "font_size": "body_l",
                          "font_weight": "bold",
                          "horizontal_alignment": "leading",
                          "margin": {
                            "top": 4,
                            "bottom": 8,
                            "leading": 6,
                            "trailing": 2
                          },
                          "padding": {
                            "top": 12,
                            "bottom": 16,
                            "leading": 14,
                            "trailing": 10
                          },
                          "size": {
                            "height": {
                              "type": "fill",
                              "value": null
                            },
                            "width": {
                              "type": "fill",
                              "value": null
                            }
                          },
                          "text_lid": "7bkohQjzIE"
                        }
                        """.trimIndent(),
                        expected = PartialTextComponent(
                            visible = false,
                            text = LocalizationKey("7bkohQjzIE"),
                            color = ColorScheme(
                                light = ColorInfo.Hex(colorInt(alpha = 0xff, red = 0, green = 0, blue = 0))
                            ),
                            backgroundColor = ColorScheme(
                                light = ColorInfo.Hex(colorInt(alpha = 0xff, red = 0xee, green = 0xee, blue = 0xee))
                            ),
                            fontName = "some font",
                            fontWeight = FontWeight.BOLD,
                            fontSize = FontSize.BODY_L,
                            horizontalAlignment = HorizontalAlignment.LEADING,
                            size = Size(height = SizeConstraint.Fill, width = SizeConstraint.Fill),
                            padding = Padding(top = 12.0, bottom = 16.0, leading = 14.0, trailing = 10.0),
                            margin = Padding(top = 4.0, bottom = 8.0, leading = 6.0, trailing = 2.0),
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialTextComponent()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialTextComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialTextComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
