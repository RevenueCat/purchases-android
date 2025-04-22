package com.revenuecat.paywallstester.paywalls

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.paywallstester.SampleData
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
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
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.CENTER
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import java.net.URL

@Suppress("LongMethod")
@OptIn(InternalRevenueCatAPI::class)
internal fun purchaseButtonInPackage(): SampleData.Components {
    val textColor = ColorScheme(
        light = ColorInfo.Hex(Color.Black.toArgb()),
        dark = ColorInfo.Hex(Color.White.toArgb()),
    )
    val backgroundColor = ColorScheme(
        light = ColorInfo.Hex(Color.White.toArgb()),
        dark = ColorInfo.Hex(Color.Black.toArgb()),
    )

    return SampleData.Components(
        data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = LocalizationKey("title"),
                                color = textColor,
                                fontWeight = FontWeight.BOLD,
                                fontSize = 28,
                                horizontalAlignment = LEADING,
                                size = Size(width = Fill, height = Fit),
                                margin = Padding(top = 32.0, bottom = 40.0, leading = 16.0, trailing = 16.0),
                            ),
                            PackageComponent(
                                packageId = "\$rc_annual",
                                isSelectedByDefault = false,
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("annual-text"),
                                            color = textColor,
                                        ),
                                        PurchaseButtonComponent(
                                            stack = StackComponent(
                                                components = listOf(
                                                    TextComponent(
                                                        text = LocalizationKey("annual-cta"),
                                                        color = textColor,
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            PackageComponent(
                                packageId = "\$rc_monthly",
                                isSelectedByDefault = false,
                                stack = StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("monthly-text"),
                                            color = textColor,
                                        ),
                                        PurchaseButtonComponent(
                                            stack = StackComponent(
                                                components = listOf(
                                                    TextComponent(
                                                        text = LocalizationKey("monthly-cta"),
                                                        color = textColor,
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                        dimension = Vertical(alignment = LEADING, distribution = CENTER),
                        size = Size(width = Fill, height = Fill),
                        backgroundColor = backgroundColor,
                    ),
                    background = Background.Color(backgroundColor),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    LocalizationKey("title") to LocalizationData.Text("Unlock premium."),
                    LocalizationKey("annual-text") to LocalizationData.Text("Annual text"),
                    LocalizationKey("annual-cta") to LocalizationData.Text("Annual CTA"),
                    LocalizationKey("monthly-text") to LocalizationData.Text("Monthly text"),
                    LocalizationKey("monthly-cta") to LocalizationData.Text("Monthly CTA"),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        ),
    )
}
