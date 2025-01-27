package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.parseRGBAColor
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@RunWith(Parameterized::class)
internal class BackgroundTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: Background,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "String",
                Args(
                    json = """
                        {
                          "type": "color",
                          "value": {
                            "light": {
                              "type": "alias",
                              "value": "primary"
                            }
                          }
                        }
                        """.trimIndent(),
                    expected = Background.Color(
                        value = ColorScheme(
                            light = ColorInfo.Alias(ColorAlias("primary"))
                        )
                    ),
                ),
            ),
            arrayOf(
                "ThemeImageUrls",
                Args(
                    json = """
                         {
                           "type": "image",
                           "value": {
                             "light": {
                               "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                               "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                               "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                               "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                               "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                               "width": 2345,
                               "height": 1234
                             }
                           }
                         }
                        """.trimIndent(),
                    expected = Background.Image(
                        value = ThemeImageUrls(
                            light = ImageUrls(
                                original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                                width = 2345.toUInt(),
                                height = 1234.toUInt(),
                            )
                        )
                    ),
                ),
            ),
            arrayOf(
                "ThemeImageUrls with FitMode and ColorScheme",
                Args(
                    json = """
                        {
                          "color_overlay": {
                            "light": {
                              "type": "hex",
                              "value": "#080808FF"
                            }
                          },
                          "fit_mode": "fit",
                          "type": "image",
                          "value": {
                            "light": {
                              "heic": "https://assets.pawwalls.com/1181742_1734689045.heic",
                              "heic_low_res": "https://assets.pawwalls.com/1181742_low_res_1734689045.heic",
                              "height": 1710,
                              "original": "https://assets.pawwalls.com/1181742_1734689045.jpg",
                              "webp": "https://assets.pawwalls.com/1181742_1734689045.webp",
                              "webp_low_res": "https://assets.pawwalls.com/1181742_low_res_1734689045.webp",
                              "width": 1140
                            }
                          }
                        }
                        """.trimIndent(),
                    expected = Background.Image(
                        value = ThemeImageUrls(
                            light = ImageUrls(
                                original = URL("https://assets.pawwalls.com/1181742_1734689045.jpg"),
                                webp = URL("https://assets.pawwalls.com/1181742_1734689045.webp"),
                                webpLowRes = URL("https://assets.pawwalls.com/1181742_low_res_1734689045.webp"),
                                width = 1140.toUInt(),
                                height = 1710.toUInt(),
                            )
                        ),
                        fitMode = FitMode.FIT,
                        colorOverlay = ColorScheme(
                            light = ColorInfo.Hex(parseRGBAColor("#080808FF"))
                        )
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize Background`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<Background>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
