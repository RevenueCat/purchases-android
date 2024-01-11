package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowHeightSizeClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowHelperTest {
    @Test
    fun `shouldUseLandscapeLayout with condensed height`() {
        assertThat(WindowHeightSizeClass.COMPACT.shouldUseLandscapeLayout).isTrue()
    }

    @Test
    fun `shouldUseLandscapeLayout with medium height`() {
        assertThat(WindowHeightSizeClass.MEDIUM.shouldUseLandscapeLayout).isFalse()
    }

    @Test
    fun `shouldUseLandscapeLayout with expanded height`() {
        assertThat(WindowHeightSizeClass.EXPANDED.shouldUseLandscapeLayout).isFalse()
    }
}