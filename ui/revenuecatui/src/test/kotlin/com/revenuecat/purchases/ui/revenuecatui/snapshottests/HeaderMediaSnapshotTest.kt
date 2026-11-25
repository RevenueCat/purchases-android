package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.junit.Test
import java.net.URL

internal class HeaderMediaSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {
    @Test
    fun emptySpacerKeepsFullWidthMediaBelowHeader() {
        val spacer = StackComponent(components = emptyList(), size = Size(Fill(), Fixed(0u)))
        // A full-width image triggers hero detection. The colored stacks make its position visible.
        val url = URL("https://example.invalid/transparent.png")
        val media = ImageComponent(
            source = ThemeImageUrls(light = ImageUrls(url, url, url, 1u, 1u)),
            size = Size(Fill(), Fixed(1u)),
        )
        val state = FakePaywallState(
            header = HeaderComponent(stack = band(0xFF374151.toInt())),
            components = listOf(
                spacer,
                band(0xFF88D4C6.toInt(), listOf(media)),
                band(0xFFFFCA82.toInt()),
                band(0xFFC9C1ED.toInt()),
            ),
        )

        screenshotTest { LoadedPaywallComponents(state, clickHandler = {}, modifier = Modifier.fillMaxSize()) }
    }

    private fun band(color: Int, children: List<PaywallComponent> = emptyList()) = StackComponent(
        components = children,
        size = Size(Fill(), Fixed(64u)),
        backgroundColor = ColorScheme(light = ColorInfo.Hex(color)),
    )
}
