package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.LayoutDirection
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConstrainedFillArrangementTest {

    @Test
    fun `single space between item is placed at logical start in RTL`() {
        val measureScope = mockk<MeasureScope> {
            every { layoutDirection } returns LayoutDirection.Rtl
        }
        val config = ConstrainedFillLayout.Config.Horizontal(
            distribution = FlexDistribution.SPACE_BETWEEN,
            arrangement = Arrangement.SpaceBetween,
            alignment = Alignment.Top,
        )

        val positions = with(measureScope) {
            arrangeConstrainedFillItems(
                config = config,
                totalSize = 100,
                sizes = intArrayOf(40),
                spacing = 0,
            )
        }

        assertThat(positions).containsExactly(60)
    }
}
