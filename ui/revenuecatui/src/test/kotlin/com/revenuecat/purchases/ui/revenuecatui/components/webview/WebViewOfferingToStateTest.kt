package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end activation check: a components config with `web_view` validates and lands as
 * [WebViewComponentStyle] in the paywall state (serializer + StyleFactory wiring).
 */
@RunWith(AndroidJUnit4::class)
class WebViewOfferingToStateTest {

    @Test
    fun `components config with web_view produces WebViewComponentStyle in paywall state`() {
        val webViewUrl = "https://paywalls.revenuecat.com/index.html"
        val webViewId = "promo_web_view"
        val size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit())

        val state = FakePaywallState(
            components = listOf(
                WebViewComponent(
                    url = webViewUrl,
                    id = webViewId,
                    protocolVersion = 1,
                    size = size,
                ),
            ),
            packages = listOf(TestData.Packages.monthly),
        )

        val rootStack = state.stack as StackComponentStyle
        val webViewStyle = rootStack.children.filterIsInstance<WebViewComponentStyle>().single()
        assertThat(webViewStyle.url).isEqualTo(webViewUrl)
        assertThat(webViewStyle.componentId).isEqualTo(webViewId)
        assertThat(webViewStyle.size).isEqualTo(size)
        assertThat(webViewStyle.visible).isTrue()
    }
}
