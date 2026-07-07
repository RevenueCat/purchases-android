package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric is required because the navigation-policy helper parses origins via android.net.Uri.
@RunWith(RobolectricTestRunner::class)
class WebViewNavigationPolicyTest {

    @Test
    fun `allows same-origin main-frame navigation on a different path`() {
        assertThat(
            shouldBlockWebViewNavigation(
                url = "https://assets.example.com/promo/step-two.html",
                isMainFrame = true,
                expectedOrigin = "https://assets.example.com",
            ),
        ).isFalse()
    }

    @Test
    fun `blocks cross-origin main-frame navigation even over https`() {
        assertThat(
            shouldBlockWebViewNavigation(
                url = "https://evil.example.org/phish.html",
                isMainFrame = true,
                expectedOrigin = "https://assets.example.com",
            ),
        ).isTrue()
    }

    @Test
    fun `blocks non-https navigation in any frame`() {
        assertThat(
            shouldBlockWebViewNavigation(
                url = "http://assets.example.com/promo/index.html",
                isMainFrame = false,
                expectedOrigin = "https://assets.example.com",
            ),
        ).isTrue()
        assertThat(
            shouldBlockWebViewNavigation(
                url = "custom://assets.example.com/",
                isMainFrame = true,
                expectedOrigin = "https://assets.example.com",
            ),
        ).isTrue()
    }

    @Test
    fun `allows cross-origin https sub-frame loads`() {
        // Sub-frame content is governed by the Content-Security-Policy, not the navigation policy.
        assertThat(
            shouldBlockWebViewNavigation(
                url = "https://other.example.com/frame.html",
                isMainFrame = false,
                expectedOrigin = "https://assets.example.com",
            ),
        ).isFalse()
    }

    @Test
    fun `blocks unparseable main-frame navigation targets`() {
        assertThat(
            shouldBlockWebViewNavigation(url = null, isMainFrame = true, expectedOrigin = "https://a.example"),
        ).isTrue()
    }
}
