package com.revenuecat.purchases.paywalls

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallColorTest {

    @Test
    fun `paywall color can be created from a ColorInt`() {
        val colorInt = Color.parseColor("#FFAABB")
        val paywallColor = PaywallColor(colorInt)

        assertThat(colorInt).isEqualTo(paywallColor.colorInt)
        assertThat("#FFAABB").isEqualTo(paywallColor.stringRepresentation)
        assertThat(Color.valueOf(colorInt)).isEqualTo(paywallColor.underlyingColor)
    }

}
