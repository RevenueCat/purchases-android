package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class CarouselComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeCarouselComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: CarouselComponent,
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
                          "type": "carousel",
                          "alignment": "center",
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
                          "shape": {
                            "type": "pill"
                          },
                          "spacing": 8,
                          "shadow": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "tertiary"
                              }
                            },
                            "radius": 20.1,
                            "x": 23.6,
                            "y": 45.2
                          },
                          "border": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            },
                            "width": 123
                          },
                          "loop": true,
                          "auto_advance": {
                            "ms_time_per_slide": 1000,
                            "ms_transition_time": 500
                          },
                          "slides": [
                            {
                              "type": "stack",
                              "components": []
                            }
                          ]
                        }
                        """.trimIndent(),
                        expected = CarouselComponent(
                            slides = listOf(
                                StackComponent(
                                    components = emptyList()
                                )
                            ),
                            alignment = VerticalAlignment.CENTER,
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
                            shape = Shape.Pill,
                            spacing = 8f,
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                radius = 20.1,
                                x = 23.6,
                                y = 45.2
                            ),
                            border = Border(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                                width = 123.0
                            ),
                            loop = true,
                            autoAdvance = CarouselComponent.AutoAdvanceSlides(
                                msTimePerSlide = 1000,
                                msTransitionTime = 500
                            )
                        )
                    ),
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "type": "carousel",
                          "alignment": "center",
                          "slides": [
                            {
                              "type": "stack",
                              "components": []
                            }
                          ]
                        }
                        """.trimIndent(),
                        expected = CarouselComponent(
                            slides = listOf(
                                StackComponent(
                                    components = emptyList()
                                )
                            ),
                            alignment = VerticalAlignment.CENTER,
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize CarouselComponent as CarouselComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<CarouselComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize CarouselComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<CarouselComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialCarouselComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialCarouselComponent,
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
                          "alignment": "center",
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
                          "shape": {
                            "type": "pill"
                          },
                          "spacing": 8,
                          "shadow": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "tertiary"
                              }
                            },
                            "radius": 20.1,
                            "x": 23.6,
                            "y": 45.2
                          },
                          "border": {
                            "color": {
                              "light": {
                                "type": "alias",
                                "value": "primary"
                              }
                            },
                            "width": 123
                          },
                          "loop": true,
                          "auto_advance": {
                            "ms_time_per_slide": 1000,
                            "ms_transition_time": 500
                          }
                        }
                        """.trimIndent(),
                        expected = PartialCarouselComponent(
                            visible = true,
                            alignment = VerticalAlignment.CENTER,
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            spacing = 8f,
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
                            shape = Shape.Pill,
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("tertiary"))),
                                radius = 20.1,
                                x = 23.6,
                                y = 45.2
                            ),
                            border = Border(
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("primary"))),
                                width = 123.0
                            ),
                            loop = true,
                            autoAdvance = CarouselComponent.AutoAdvanceSlides(
                                msTimePerSlide = 1000,
                                msTransitionTime = 500
                            )
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialCarouselComponent()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialCarouselComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialCarouselComponent>(args.json)

            // Assert
//            assert(actual == args.expected)
            Assertions.assertThat(actual).isEqualTo(args.expected)
        }
    }
}
