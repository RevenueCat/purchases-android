package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class NextAutoAdvanceTargetPageTest {

    @Test
    fun `non-loop advances until last page then returns null`() {
        assertThat(nextAutoAdvanceTargetPage(ringCount = 3, currentPage = 0)).isEqualTo(1)
        assertThat(nextAutoAdvanceTargetPage(ringCount = 3, currentPage = 1)).isEqualTo(2)
        assertThat(nextAutoAdvanceTargetPage(ringCount = 3, currentPage = 2)).isNull()
    }

    @Test
    fun `non-loop single page never advances`() {
        assertThat(nextAutoAdvanceTargetPage(ringCount = 1, currentPage = 0)).isNull()
    }

    @Test
    fun `loop advances forward through the ring, including past the last real page`() {
        // Ring 0..6, real zone 2..4. Sliding on into the clone is what makes the wrap look like an
        // ordinary forward transition.
        assertThat(nextAutoAdvanceTargetPage(ringCount = 7, currentPage = 2)).isEqualTo(3)
        assertThat(nextAutoAdvanceTargetPage(ringCount = 7, currentPage = 4)).isEqualTo(5)
        assertThat(nextAutoAdvanceTargetPage(ringCount = 7, currentPage = 5)).isEqualTo(6)
    }

    @Test
    fun `loop never targets an index outside the ring`() {
        assertThat(nextAutoAdvanceTargetPage(ringCount = 7, currentPage = 6)).isNull()
    }

    @Test
    fun `single page loop never advances`() {
        // A single page gets no clones, so its ring is just the one index.
        assertThat(nextAutoAdvanceTargetPage(ringCount = 1, currentPage = 0)).isNull()
    }

    @Test
    fun `empty carousel returns null`() {
        assertThat(nextAutoAdvanceTargetPage(ringCount = 0, currentPage = 0)).isNull()
    }
}
