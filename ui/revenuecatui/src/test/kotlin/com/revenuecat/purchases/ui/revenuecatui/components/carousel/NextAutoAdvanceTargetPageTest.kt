package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class NextAutoAdvanceTargetPageTest {

    @Test
    fun `non-loop advances until last page then returns null`() {
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = false, pageCount = 3, currentPage = 0)).isEqualTo(1)
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = false, pageCount = 3, currentPage = 1)).isEqualTo(2)
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = false, pageCount = 3, currentPage = 2)).isNull()
    }

    @Test
    fun `non-loop single page never advances`() {
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = false, pageCount = 1, currentPage = 0)).isNull()
    }

    @Test
    fun `loop always increments`() {
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = true, pageCount = 3, currentPage = 0)).isEqualTo(1)
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = true, pageCount = 3, currentPage = 2)).isEqualTo(3)
    }

    @Test
    fun `empty carousel returns null`() {
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = false, pageCount = 0, currentPage = 0)).isNull()
        assertThat(nextAutoAdvanceTargetPage(shouldLoop = true, pageCount = 0, currentPage = 0)).isNull()
    }
}
