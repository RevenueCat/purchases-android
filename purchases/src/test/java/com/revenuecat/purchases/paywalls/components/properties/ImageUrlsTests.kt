package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.JsonTools
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@RunWith(Enclosed::class)
internal class ImageUrlsTests {

    class DeserializeImageUrlsTests {

        @Test
        fun `Should properly deserialize ImageUrls`() {
            // Arrange
            @Language("json")
            val json = """
                {
                  "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                  "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                  "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                  "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                  "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                  "width": 5467,
                  "height": 3564
                }
            """.trimIndent()
            val expected = ImageUrls(
                original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                width = 5467.toUInt(),
                height = 3564.toUInt(),
            )
            
            // Act
            val actual = JsonTools.json.decodeFromString<ImageUrls>(json)

            // Assert
            assert(actual == expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializeThemeImageUrlsTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            @Language("json")
            val json: String,
            val expected: ThemeImageUrls,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "light and dark",
                    Args(
                        json = """
                            {
                              "light": {
                                "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                                "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                                "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                                "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                                "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                                "width": 2345,
                                "height": 1234
                              },
                              "dark": {
                                "heic": "https://assets.pawwalls.com/2262159_2843140659.heic",
                                "heic_low_res": "https://assets.pawwalls.com/2262159_low_res_2843140659.heic",
                                "original": "https://assets.pawwalls.com/2262159_2843140659.png",
                                "webp": "https://assets.pawwalls.com/2262159_2843140659.webp",
                                "webp_low_res": "https://assets.pawwalls.com/2262159_low_res_2843140659.webp",
                                "width": 35687,
                                "height": 3568
                              }
                            }
                        """.trimIndent(),
                        expected = ThemeImageUrls(
                            light = ImageUrls(
                                original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                                width = 2345.toUInt(),
                                height = 1234.toUInt(),
                            ),
                            dark = ImageUrls(
                                original = URL("https://assets.pawwalls.com/2262159_2843140659.png"),
                                webp = URL("https://assets.pawwalls.com/2262159_2843140659.webp"),
                                webpLowRes = URL("https://assets.pawwalls.com/2262159_low_res_2843140659.webp"),
                                width = 35687.toUInt(),
                                height = 3568.toUInt(),
                            ),
                        )
                    )
                ),
                arrayOf(
                    "light only",
                    Args(
                        json = """
                            {
                              "light": {
                                "heic": "https://assets.pawwalls.com/1151049_1732039548.heic",
                                "heic_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.heic",
                                "original": "https://assets.pawwalls.com/1151049_1732039548.png",
                                "webp": "https://assets.pawwalls.com/1151049_1732039548.webp",
                                "webp_low_res": "https://assets.pawwalls.com/1151049_low_res_1732039548.webp",
                                "width": 547,
                                "height": 257
                              }
                            }
                        """.trimIndent(),
                        expected = ThemeImageUrls(
                            light = ImageUrls(
                                original = URL("https://assets.pawwalls.com/1151049_1732039548.png"),
                                webp = URL("https://assets.pawwalls.com/1151049_1732039548.webp"),
                                webpLowRes = URL("https://assets.pawwalls.com/1151049_low_res_1732039548.webp"),
                                width = 547.toUInt(),
                                height = 257.toUInt(),
                            ),
                        )
                    )
                ),
            )
        }

        @Test
        fun `Should properly deserialize ThemeImageUrls`() {
            // Arrange, Act
            val actual = JsonTools.json.decodeFromString<ThemeImageUrls>(args.json)

            // Assert
            assert(actual == args.expected)
        }
    }
}
