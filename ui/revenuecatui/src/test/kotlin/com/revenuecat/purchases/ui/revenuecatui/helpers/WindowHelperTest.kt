package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowHeightSizeClass
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowHelperTest {
    @Test
    fun `shouldUseLandscapeLayout with fullscreen condensed height`() {
        assertThat(shouldUseLandscapeLayout(PaywallMode.FULL_SCREEN, WindowHeightSizeClass.COMPACT)).isTrue()
    }

    @Test
    fun `shouldUseLandscapeLayout with fullscreen medium height`() {
        assertThat(shouldUseLandscapeLayout(PaywallMode.FULL_SCREEN, WindowHeightSizeClass.MEDIUM)).isFalse()
    }

    @Test
    fun `shouldUseLandscapeLayout with fullscreen expanded height`() {
        assertThat(shouldUseLandscapeLayout(PaywallMode.FULL_SCREEN, WindowHeightSizeClass.EXPANDED)).isFalse()
    }

    @Test
    fun `shouldUseLandscapeLayout with footer mode`() {
        assertThat(shouldUseLandscapeLayout(PaywallMode.FOOTER, WindowHeightSizeClass.COMPACT)).isFalse()
        assertThat(shouldUseLandscapeLayout(PaywallMode.FOOTER, WindowHeightSizeClass.MEDIUM)).isFalse()
        assertThat(shouldUseLandscapeLayout(PaywallMode.FOOTER, WindowHeightSizeClass.EXPANDED)).isFalse()
    }

    @Test
    fun `shouldUseLandscapeLayout with condensed mode`() {
        assertThat(shouldUseLandscapeLayout(PaywallMode.FOOTER_CONDENSED, WindowHeightSizeClass.COMPACT)).isFalse()
        assertThat(shouldUseLandscapeLayout(PaywallMode.FOOTER_CONDENSED, WindowHeightSizeClass.MEDIUM)).isFalse()
        assertThat(shouldUseLandscapeLayout(PaywallMode.FOOTER_CONDENSED, WindowHeightSizeClass.EXPANDED)).isFalse()
    }
}
