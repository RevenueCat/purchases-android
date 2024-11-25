package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@RunWith(Parameterized::class)
internal class LocalizationDataTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val serialized: String,
        val deserialized: LocalizationData,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "String",
                Args(
                    serialized = """
                        "some text"
                        """.trimIndent(),
                    deserialized = LocalizationData.Text("some text"),
                ),
            ),
            arrayOf(
                "ThemeImageUrls",
                Args(
                    serialized = """
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
                    deserialized = LocalizationData.Image(
                        ThemeImageUrls(
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
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize LocalizationData`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<LocalizationData>(args.serialized)

        // Assert
        assert(actual == args.deserialized)
    }
}
