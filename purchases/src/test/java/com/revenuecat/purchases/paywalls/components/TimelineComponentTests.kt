package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.colorInt
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class TimelineComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeTimelineComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: TimelineComponent,
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
                          "type": "timeline",
                          "item_spacing": 10,
                          "text_spacing": 20,
                          "column_gutter": 30,
                          "icon_alignment": "title_and_description",
                          "size": {
                            "height": {
                              "type": "fit",
                              "value": null
                            },
                            "width": {
                              "type": "fill",
                              "value": null
                            }
                          },
                          "padding": {
                            "top": 1,
                            "leading": 2,
                            "bottom": 3,
                            "trailing": 4
                          },
                          "margin": {
                            "top": 5,
                            "leading": 6,
                            "bottom": 7,
                            "trailing": 8
                          },
                          "items": [
                              {
                                 "title": {
                                    "type": "text",
                                    "text_lid": "title",
                                    "color": { "light": { "type": "hex", "value": "#000000" } }
                                 },
                                 "description": {
                                    "type": "text",
                                    "text_lid": "description",
                                    "color": { "light": { "type": "hex", "value": "#FFFFFF" } }
                                 },
                                 "icon": {
                                    "type": "icon",
                                    "base_url": "https://example.com",
                                    "icon_name": "Test icon name",
                                    "formats": {
                                      "webp": "test.webp"
                                    }
                                 },
                                 "connector": {
                                    "width": 40,
                                    "margin": {
                                      "top": 9,
                                      "leading": 10,
                                      "bottom": 11,
                                      "trailing": 12
                                    },
                                    "color": { "light": { "type": "alias", "value": "primary" } }
                                 }
                              }
                          ]
                        }
                        """.trimIndent(),
                        expected = TimelineComponent(
                            itemSpacing = 10,
                            textSpacing = 20,
                            columnGutter = 30,
                            iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            padding = Padding(
                                top = 1.0,
                                leading = 2.0,
                                bottom = 3.0,
                                trailing = 4.0
                            ),
                            margin = Padding(
                                top = 5.0,
                                leading = 6.0,
                                bottom = 7.0,
                                trailing = 8.0
                            ),
                            items = listOf(
                                TimelineComponent.Item(
                                    title = TextComponent(
                                        text = LocalizationKey("title"),
                                        color = ColorScheme(
                                            light = ColorInfo.Hex(
                                                colorInt(alpha = 0xff, red = 0, green = 0, blue = 0)
                                            )
                                        )
                                    ),
                                    description = TextComponent(
                                        text = LocalizationKey("description"),
                                        color = ColorScheme(
                                            light = ColorInfo.Hex(
                                                colorInt(alpha = 0xff, red = 0xff, green = 0xff, blue = 0xff)
                                            )
                                        )
                                    ),
                                    icon = IconComponent(
                                        baseUrl = "https://example.com",
                                        iconName = "Test icon name",
                                        formats = IconComponent.Formats(
                                            webp = "test.webp"
                                        ),
                                    ),
                                    connector = TimelineComponent.Connector(
                                        width = 40,
                                        margin = Padding(
                                            top = 9.0,
                                            leading = 10.0,
                                            bottom = 11.0,
                                            trailing = 12.0
                                        ),
                                        color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                                    ),
                                )
                            ),
                        )
                    ),
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "type": "timeline",
                          "item_spacing": 10,
                          "text_spacing": 20,
                          "column_gutter": 30,
                          "icon_alignment": "title_and_description"
                        }
                        """.trimIndent(),
                        expected = TimelineComponent(
                            itemSpacing = 10,
                            textSpacing = 20,
                            columnGutter = 30,
                            iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
                        )
                    ),
                ),
                arrayOf(
                    "optional values in item absent",
                    Args(
                        json = """
                        {
                          "type": "timeline",
                          "item_spacing": 10,
                          "text_spacing": 20,
                          "column_gutter": 30,
                          "icon_alignment": "title_and_description",
                          "items": [
                              {
                                 "title": {
                                    "type": "text",
                                    "text_lid": "title",
                                    "color": { "light": { "type": "hex", "value": "#000000" } }
                                 },
                                 "icon": {
                                    "type": "icon",
                                    "base_url": "https://example.com",
                                    "icon_name": "Test icon name",
                                    "formats": {
                                      "webp": "test.webp"
                                    }
                                 }
                              }
                          ]
                        }
                        """.trimIndent(),
                        expected = TimelineComponent(
                            itemSpacing = 10,
                            textSpacing = 20,
                            columnGutter = 30,
                            iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
                            items = listOf(
                                TimelineComponent.Item(
                                    title = TextComponent(
                                        text = LocalizationKey("title"),
                                        color = ColorScheme(
                                            light = ColorInfo.Hex(
                                                colorInt(alpha = 0xff, red = 0, green = 0, blue = 0)
                                            )
                                        )
                                    ),
                                    description = null,
                                    icon = IconComponent(
                                        baseUrl = "https://example.com",
                                        iconName = "Test icon name",
                                        formats = IconComponent.Formats(
                                            webp = "test.webp"
                                        ),
                                    ),
                                    connector = null,
                                )
                            ),
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize TimelineComponent as TimelineComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<TimelineComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize TimelineComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialTimelineComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialTimelineComponent,
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
                          "item_spacing": 10,
                          "text_spacing": 20,
                          "column_gutter": 30,  
                          "icon_alignment": "title_and_description",
                          "size": {
                            "height": {
                              "type": "fit",
                              "value": null
                            },
                            "width": {
                              "type": "fill",
                              "value": null
                            }
                          },
                          "padding": {
                            "top": 1,
                            "leading": 2,
                            "bottom": 3,
                            "trailing": 4
                          },
                          "margin": {
                            "top": 5,
                            "leading": 6,
                            "bottom": 7,
                            "trailing": 8
                          },
                          "items": [
                            {
                             "title": {
                                "type": "text",
                                "text_lid": "title",
                                "color": { "light": { "type": "hex", "value": "#000000" } }
                             },
                             "description": {
                                "type": "text",
                                "text_lid": "description",
                                "color": { "light": { "type": "hex", "value": "#FFFFFF" } }
                             },
                             "icon": {
                                "type": "icon",
                                "base_url": "https://example.com",
                                "icon_name": "Test icon name",
                                "formats": {
                                  "webp": "test.webp"
                                }
                             },
                             "connector": {
                                "width": 40,
                                "margin": {
                                  "top": 9,
                                  "leading": 10,
                                  "bottom": 11,
                                  "trailing": 12
                                },
                                "color": { "light": { "type": "alias", "value": "primary" } }
                             }
                            }
                          ]
                        }
                        """.trimIndent(),
                        expected = PartialTimelineComponent(
                            itemSpacing = 10,
                            textSpacing = 20,
                            columnGutter = 30,
                            iconAlignment = TimelineComponent.IconAlignment.TitleAndDescription,
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            padding = Padding(
                                top = 1.0,
                                leading = 2.0,
                                bottom = 3.0,
                                trailing = 4.0
                            ),
                            margin = Padding(
                                top = 5.0,
                                leading = 6.0,
                                bottom = 7.0,
                                trailing = 8.0
                            ),
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialTimelineComponent()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialTimelineComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialTimelineComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialTimelineComponentItemTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialTimelineComponentItem,
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
                          "visible": true,
                            "connector": {
                                "width": 40,
                                "margin": {
                                    "top": 9,
                                    "leading": 10,
                                    "bottom": 11,
                                    "trailing": 12
                                },
                                "color": { "light": { "type": "alias", "value": "primary" } }
                            }
                        }
                        """.trimIndent(),
                        expected = PartialTimelineComponentItem(
                            visible = true,
                            connector = TimelineComponent.Connector(
                                width = 40,
                                margin = Padding(
                                    top = 9.0,
                                    leading = 10.0,
                                    bottom = 11.0,
                                    trailing = 12.0
                                ),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary")))
                            ),
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialTimelineComponentItem()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialTimelineComponentItem`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialTimelineComponentItem>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
