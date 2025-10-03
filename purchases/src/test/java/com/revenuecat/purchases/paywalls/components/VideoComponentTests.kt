package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.net.URL

class VideoComponentTests {
    @Language("json")
    val json = """
        {
          "auto_play": true,
          "color_overlay": {
            "light": {
              "type": "alias",
              "value": "primary"
            }
          },
          "fallback_source": {
            "light": {
              "heic": "https://assets.revenuecat.com/1151049_1732039548.heic",
              "heic_low_res": "https://assets.revenuecat.com/1151049_low_res_1732039548.heic",
              "height": 200,
              "original": "https://assets.revenuecat.com/1151049_1732039548.png",
              "webp": "https://assets.revenuecat.com/1151049_1732039548.webp",
              "webp_low_res": "https://assets.revenuecat.com/1151049_low_res_1732039548.webp",
              "width": 400
            }
          },
          "fit_mode": "fill",
          "id": "P0Tzh3p6d3",
          "loop": true,
          "mask_shape": {
            "corners": {
              "bottom_leading": 1,
              "bottom_trailing": 2,
              "top_leading": 3,
              "top_trailing": 4
            },
            "type": "rectangle"
          },
          "mute_audio": true,
          "name": "",
          "override_source_lid": "abc123",
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
          "show_controls": false,
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
              "height": 400,
              "url_low_res": "https://RevenueCat.com/video-files/herding_tiny_cats.mp4",
              "url": "https://RevenueCat.com/video-files/herding_cats.mp4",
              "width": 200
            }
          },
          "type": "video"
        }
        """

    @Suppress("LongMethod")
    @Test
    fun `deserializes correctly`() {
        val images = ThemeImageUrls(
            light = ImageUrls(
                width = 400u,
                height = 200u,
                original = URL("https://assets.revenuecat.com/1151049_1732039548.png"),
                webp = URL("https://assets.revenuecat.com/1151049_1732039548.webp"),
                webpLowRes = URL("https://assets.revenuecat.com/1151049_low_res_1732039548.webp"),
            ),
            dark = null,
        )
        val videos = ThemeVideoUrls(
            light = VideoUrls(
                width = 200u,
                height = 400u,
                url = URL("https://RevenueCat.com/video-files/herding_cats.mp4"),
                urlLowRes = URL("https://RevenueCat.com/video-files/herding_tiny_cats.mp4"),
            ),
            dark = null,
        )

        val size = Size(
            width = SizeConstraint.Fill,
            height = SizeConstraint.Fit,
        )

        val mask = MaskShape.Rectangle(
            corners = CornerRadiuses.Dp(
                topLeading = 3.0,
                topTrailing = 4.0,
                bottomLeading = 1.0,
                bottomTrailing = 2.0,
            ),
        )

        val overlay = ColorScheme(
            light = ColorInfo.Alias(ColorAlias("primary")),
            dark = null,
        )

        val expected = VideoComponent(
            source = videos,
            fallbackSource = images,
            visible = null,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = size,
            fitMode = FitMode.FILL,
            maskShape = mask,
            colorOverlay = overlay,
            padding = null,
            margin = null,
            border = null,
            shadow = Shadow(
                ColorScheme(
                    light = ColorInfo.Alias(ColorAlias("tertiary")),
                ),
                radius = 20.1,
                x = 23.6,
                y = 45.2,
            ),
            overrides = null,
            overrideSourceLid = LocalizationKey("abc123"),
        )
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<VideoComponent>(json)

        // Assert
        assert(actual == expected)
    }
}
