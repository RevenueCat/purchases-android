package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.Padding
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class PaddingTest {

    @Test
    fun `positive values are converted as-is`() {
        val padding = Padding(top = 10.0, bottom = 20.0, leading = 30.0, trailing = 40.0)

        val actual = padding.toPaddingValues()

        assertThat(actual).isEqualTo(
            PaddingValues(start = 30.dp, top = 10.dp, end = 40.dp, bottom = 20.dp),
        )
    }

    @Test
    fun `negative values are clamped to zero`() {
        val padding = Padding(top = -10.0, bottom = -20.0, leading = -30.0, trailing = -40.0)

        val actual = padding.toPaddingValues()

        assertThat(actual).isEqualTo(
            PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
        )
    }

    @Test
    fun `only negative values are clamped, positive values are preserved`() {
        val padding = Padding(top = -10.0, bottom = 20.0, leading = -30.0, trailing = 40.0)

        val actual = padding.toPaddingValues()

        assertThat(actual).isEqualTo(
            PaddingValues(start = 0.dp, top = 0.dp, end = 40.dp, bottom = 20.dp),
        )
    }
}
