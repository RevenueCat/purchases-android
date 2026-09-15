package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SizeTest {

    private val margin = PaddingValues(
        start = 4.dp,
        top = 3.dp,
        end = 6.dp,
        bottom = 7.dp,
    )

    @Test
    fun `adds margins to fixed dimensions`() {
        val result = Size(
            width = Fixed(20u),
            height = Fixed(30u),
        ).addMargin(margin, LayoutDirection.Ltr)

        assertThat(result.width).isEqualTo(Fixed(30u))
        assertThat(result.height).isEqualTo(Fixed(40u))
    }

    @Test
    fun `adds margins to fill limits`() {
        val result = Size(
            width = Fill(min = 20u, max = 30u),
            height = Fill(min = 40u, max = 50u),
        ).addMargin(margin, LayoutDirection.Ltr)

        assertThat(result.width).isEqualTo(Fill(min = 30u, max = 40u))
        assertThat(result.height).isEqualTo(Fill(min = 50u, max = 60u))
    }

    @Test
    fun `adds margins to fit limits without changing default`() {
        val result = Size(
            width = Fit(default = 15u, min = 20u, max = 30u),
            height = Fit(default = 25u),
        ).addMargin(margin, LayoutDirection.Ltr)

        assertThat(result.width).isEqualTo(Fit(default = 15u, min = 30u, max = 40u))
        assertThat(result.height).isEqualTo(Fit(default = 25u))
    }
}
