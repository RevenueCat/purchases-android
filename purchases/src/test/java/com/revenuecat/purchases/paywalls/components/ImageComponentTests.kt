package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@RunWith(Enclosed::class)
internal class ImageComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeImageComponentTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ImageComponent,
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
                          "corner_radiuses": {
                            "bottom_leading": 5,
                            "bottom_trailing": 6,
                            "top_leading": 7,
                            "top_trailing": 8
                          },
                          "fit_mode": "fill",
                          "color_overlay": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "id": "P0Tzh3p6d3",
                          "mask_shape": {
                            "corners": {
                              "top_leading": 3,
                              "top_trailing": 4,
                              "bottom_leading": 1,
                              "bottom_trailing": 2
                            },
                            "type": "rectangle"
                          },
                          "name": "",
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
                          "source": {
                            "light": {
                              "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                              "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                              "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                              "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                              "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                              "height": 200,
                              "width": 400
                            }
                          },
                          "override_source_lid": "abc123",
                          "type": "image"
                        }
                        """.trimIndent(),
                        expected = ImageComponent(
                            source = ThemeImageUrls(
                                light = ImageUrls(
                                    original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                    webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                    webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                                    height = 200.toUInt(),
                                    width = 400.toUInt(),
                                )
                            ),
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            overrideSourceLid = LocalizationKey("abc123"),
                            maskShape = MaskShape.Rectangle(
                                corners = CornerRadiuses.Dp(
                                    topLeading = 3.0,
                                    topTrailing = 4.0,
                                    bottomLeading = 1.0,
                                    bottomTrailing = 2.0,
                                )
                            ),
                            colorOverlay = ColorScheme(
                                light = ColorInfo.Alias("primary")
                            ),
                            fitMode = FitMode.FILL,
                        )
                    ),
                ),
                arrayOf(
                    "optional values absent",
                    Args(
                        json = """
                        {
                          "id": "P0Tzh3p6d3",
                          "name": "",
                          "source": {
                            "light": {
                              "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                              "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                              "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                              "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                              "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                              "height": 200,
                              "width": 400
                            }
                          },
                          "type": "image"
                        }
                        """.trimIndent(),
                        expected = ImageComponent(
                            source = ThemeImageUrls(
                                light = ImageUrls(
                                    original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                    webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                    webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                                    height = 200.toUInt(),
                                    width = 400.toUInt(),
                                )
                            ),
                        )
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize ImageComponent as ImageComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<ImageComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }

        @Test
        fun `Should properly deserialize ImageComponent as PaywallComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PaywallComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialImageComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialImageComponent,
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
                          "fit_mode": "fill",
                          "color_overlay": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          },
                          "mask_shape": {
                            "corners": {
                              "top_leading": 3,
                              "top_trailing": 4,
                              "bottom_leading": 1,
                              "bottom_trailing": 2
                            },
                            "type": "rectangle"
                          },
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
                          "source": {
                            "light": {
                              "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                              "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                              "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                              "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                              "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                              "height": 200,
                              "width": 400
                            }
                          },
                          "override_source_lid": "abc123"
                        }
                        """.trimIndent(),
                        expected = PartialImageComponent(
                            visible = false,
                            source = ThemeImageUrls(
                                light = ImageUrls(
                                    original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                    webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                    webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                                    height = 200.toUInt(),
                                    width = 400.toUInt(),
                                )
                            ),
                            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                            overrideSourceLid = LocalizationKey("abc123"),
                            fitMode = FitMode.FILL,
                            maskShape = MaskShape.Rectangle(
                                corners = CornerRadiuses.Dp(
                                    topLeading = 3.0,
                                    topTrailing = 4.0,
                                    bottomLeading = 1.0,
                                    bottomTrailing = 2.0,
                                )
                            ),
                            colorOverlay = ColorScheme(light = ColorInfo.Alias("primary"))
                        )
                    )
                ),
                arrayOf(
                    "all values absent",
                    Args(
                        json = """
                        { }
                        """.trimIndent(),
                        expected = PartialImageComponent()
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize PartialImageComponent`() {
            // Arrange, Act
            val actual = OfferingParser.json.decodeFromString<PartialImageComponent>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
