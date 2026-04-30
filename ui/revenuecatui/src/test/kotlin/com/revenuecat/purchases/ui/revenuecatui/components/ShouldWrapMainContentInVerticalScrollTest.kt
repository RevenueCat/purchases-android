package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.gestures.Orientation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ShouldWrapMainContentInVerticalScrollTest {

    @Test
    fun `vertically scrolling root stack is not wrapped`() {
        val stack = previewStackComponentStyle(
            children = emptyList(),
            scrollOrientation = Orientation.Vertical,
        )

        assertThat(shouldWrapMainContentInVerticalScroll(stack)).isFalse()
    }

    @Test
    fun `horizontally scrolling root stack is wrapped`() {
        val stack = previewStackComponentStyle(
            children = emptyList(),
            scrollOrientation = Orientation.Horizontal,
        )

        assertThat(shouldWrapMainContentInVerticalScroll(stack)).isTrue()
    }

    @Test
    fun `non-scrolling root stack is wrapped`() {
        val stack = previewStackComponentStyle(
            children = emptyList(),
            scrollOrientation = null,
        )

        assertThat(shouldWrapMainContentInVerticalScroll(stack)).isTrue()
    }

    @Test
    fun `non-stack root component is wrapped`() {
        val text = previewTextComponentStyle(text = "non-stack root")

        assertThat(shouldWrapMainContentInVerticalScroll(text)).isTrue()
    }
}
