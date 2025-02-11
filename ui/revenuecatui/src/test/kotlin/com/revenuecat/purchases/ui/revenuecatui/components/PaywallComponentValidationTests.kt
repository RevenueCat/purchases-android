package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
internal class PaywallComponentValidationTests {
    private companion object {
        private val localizationKey = LocalizationKey("hello-world")

        const val EXPECTED_TEXT_EN = "Hello, world!"

        val paywallComponents = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = localizationKey,
                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
                            )
                        ),
                        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    localizationKey to LocalizationData.Text(EXPECTED_TEXT_EN),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), paywallComponents),
        )
    }

    // This tests a temporal hack to make the root component fill the screen. This will be removed once we have a
    // definite solution for positioning the root component.
    @Test
    fun `Should use Fill size in root component even if given Fit`() {
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        assert((validated.stack as StackComponentStyle).size.height == SizeConstraint.Fill)
    }

}
