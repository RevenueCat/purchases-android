package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MediaSizeTest {

    @Test
    fun `fit dimensions use intrinsic size when it is within limits`() {
        val size = Size(width = Fit(max = 300u), height = Fit(min = 40u))

        assertThat(size.adjustForMedia(widthPx = 200u, heightPx = 100u, density = Density(1f)))
            .isEqualTo(Size(width = Fixed(200u), height = Fixed(100u)))
    }

    @Test
    fun `width maximum scales both fit dimensions down`() {
        val size = Size(width = Fit(max = 100u), height = Fit())

        assertThat(size.adjustForMedia(widthPx = 200u, heightPx = 100u, density = Density(1f)))
            .isEqualTo(Size(width = Fixed(100u), height = Fixed(50u)))
    }

    @Test
    fun `height minimum scales both fit dimensions up`() {
        val size = Size(width = Fit(), height = Fit(min = 150u))

        assertThat(size.adjustForMedia(widthPx = 200u, heightPx = 100u, density = Density(1f)))
            .isEqualTo(Size(width = Fixed(300u), height = Fixed(150u)))
    }

    @Test
    fun `most restrictive maximum scales both fit dimensions`() {
        val size = Size(width = Fit(max = 150u), height = Fit(max = 40u))

        assertThat(size.adjustForMedia(widthPx = 200u, heightPx = 100u, density = Density(1f)))
            .isEqualTo(Size(width = Fixed(80u), height = Fixed(40u)))
    }
}
