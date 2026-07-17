package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class WebViewEffectiveSizeTest {

    @Test
    fun `fit axes use placeholders until the content reports a size`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fit(), height = Fit()),
            contentWidthCssPx = 0,
            contentHeightCssPx = 0,
        )

        assertThat(size.width).isEqualTo(Fixed(FIT_PLACEHOLDER_WIDTH))
        assertThat(size.height).isEqualTo(Fixed(FIT_PLACEHOLDER_HEIGHT))
    }

    @Test
    fun `fit axes use the reported content size once available`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fit(), height = Fit()),
            contentWidthCssPx = 320,
            contentHeightCssPx = 480,
        )

        assertThat(size.width).isEqualTo(Fixed(320u))
        assertThat(size.height).isEqualTo(Fixed(480u))
    }

    @Test
    fun `non-fit axes ignore reported content sizes`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fill, height = Fixed(200u)),
            contentWidthCssPx = 320,
            contentHeightCssPx = 480,
        )

        assertThat(size.width).isEqualTo(Fill)
        assertThat(size.height).isEqualTo(Fixed(200u))
    }
}
