package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.DownloadedFont
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.revenuecat.purchases.paywalls.components.properties.FontStyle as RcFontStyle

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
class FontSpecResolveTests {

    @Test
    fun `resolve on empty downloaded family returns the default family`() {
        val spec = FontSpec.Downloaded(DownloadedFontFamily(family = "MyFont", fonts = emptyList()))

        val result = spec.resolve(weight = FontWeight.Normal, style = FontStyle.Normal)

        assertThat(result).isEqualTo(FontFamily.Default)
    }

    @Test
    fun `resolve on multi-weight downloaded family returns a non-default family`() {
        val spec = FontSpec.Downloaded(
            DownloadedFontFamily(
                family = "MyFont",
                fonts = listOf(
                    DownloadedFont(weight = 400, style = RcFontStyle.NORMAL, file = File("regular.otf")),
                    DownloadedFont(weight = 600, style = RcFontStyle.NORMAL, file = File("semibold.otf")),
                    DownloadedFont(weight = 700, style = RcFontStyle.ITALIC, file = File("bolditalic.otf")),
                ),
            ),
        )

        val result = spec.resolve(weight = FontWeight.Normal, style = FontStyle.Normal)

        assertThat(result).isNotEqualTo(FontFamily.Default)
    }
}
