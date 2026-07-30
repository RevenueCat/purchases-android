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
    fun `fit axes use schema default until the content reports a size`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fit(default = 400u), height = Fit(default = 250u)),
            contentWidthCssPx = 0,
            contentHeightCssPx = 0,
        )

        assertThat(size.width).isEqualTo(Fixed(400u))
        assertThat(size.height).isEqualTo(Fixed(250u))
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

    @Test
    fun `bounded fill axes are left untouched`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fill, height = Fill),
            contentWidthCssPx = 0,
            contentHeightCssPx = 0,
            widthAxisUnbounded = false,
            heightAxisUnbounded = false,
        )

        assertThat(size.width).isEqualTo(Fill)
        assertThat(size.height).isEqualTo(Fill)
    }

    @Test
    fun `unbounded fill axes use the placeholder until the content reports a size`() {
        // A bare WebView has no intrinsic size, so a fill axis that's genuinely unbounded at
        // measure time (an ancestor scrolls, or a Fit container sits under one that does) would
        // otherwise collapse to zero, since fillMaxWidth/Height just passes the unbounded
        // constraint through. Fill (unlike Fit) also carries no `default` field in the schema, so
        // there's nothing to fall back on but the hardcoded placeholder.
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fill, height = Fill),
            contentWidthCssPx = 0,
            contentHeightCssPx = 0,
            widthAxisUnbounded = true,
            heightAxisUnbounded = true,
        )

        assertThat(size.width).isEqualTo(Fixed(FIT_PLACEHOLDER_WIDTH))
        assertThat(size.height).isEqualTo(Fixed(FIT_PLACEHOLDER_HEIGHT))
    }

    @Test
    fun `unbounded fill axes use the reported content size once available`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fill, height = Fill),
            contentWidthCssPx = 320,
            contentHeightCssPx = 480,
            widthAxisUnbounded = true,
            heightAxisUnbounded = true,
        )

        assertThat(size.width).isEqualTo(Fixed(320u))
        assertThat(size.height).isEqualTo(Fixed(480u))
    }

    @Test
    fun `fixed axes are never affected by unbounded tracking`() {
        val size = webViewEffectiveSize(
            declaredSize = Size(width = Fixed(200u), height = Fixed(300u)),
            contentWidthCssPx = 320,
            contentHeightCssPx = 480,
            widthAxisUnbounded = true,
            heightAxisUnbounded = true,
        )

        assertThat(size.width).isEqualTo(Fixed(200u))
        assertThat(size.height).isEqualTo(Fixed(300u))
    }
}
