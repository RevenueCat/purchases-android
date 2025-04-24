package com.revenuecat.purchases.paywalls

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallColorTest {
    @Test
    fun `paywall color can be created from an RGB ColorInt`() {
        val stringRepresentation = "#FFAABB"
        val colorInt = Color.parseColor(stringRepresentation)
        val paywallColor = PaywallColor(colorInt)

        assertThat(colorInt).isEqualTo(paywallColor.colorInt)
        assertThat(stringRepresentation).isEqualTo(paywallColor.stringRepresentation)
        assertThat(Color.valueOf(colorInt)).isEqualTo(paywallColor.underlyingColor)
    }

    @Test
    fun `paywall color can be created from RGB string`() {
        val stringRepresentation = "#FFAABB"
        val paywallColor = PaywallColor(stringRepresentation)
        val color = Color.valueOf(Color.parseColor(stringRepresentation))

        assertThat(stringRepresentation).isEqualTo(paywallColor.stringRepresentation)
        assertThat(color).isEqualTo(paywallColor.underlyingColor)
    }

    @Test
    fun `paywall color can be created from RGBA string`() {
        val stringRepresentation = "#FFAABB11"
        val expectedColor = Color.argb("11".toInt(16), "FF".toInt(16), "AA".toInt(16), "BB".toInt(16))
        val paywallColor = PaywallColor(stringRepresentation)

        assertThat(stringRepresentation).isEqualTo(paywallColor.stringRepresentation)
        assertThat(paywallColor.underlyingColor).isEqualTo(Color.valueOf(expectedColor))
    }
}
