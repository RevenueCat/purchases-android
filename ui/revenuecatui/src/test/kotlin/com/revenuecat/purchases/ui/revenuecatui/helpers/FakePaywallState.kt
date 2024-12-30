@file:Suppress("TestFunctionName")

package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import java.net.URL

internal fun FakePaywallState(vararg pkg: Package): PaywallState.Loaded.Components =
    FakePaywallState(packages = pkg.toList())

internal fun FakePaywallState(vararg component: PaywallComponent): PaywallState.Loaded.Components =
    FakePaywallState(components = component.toList())

internal fun FakePaywallState(
    localizations: NonEmptyMap<LocaleId, LocalizationDictionary> = nonEmptyMapOf(
        LocaleId("en_US") to nonEmptyMapOf(
            LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
        )
    ),
    defaultLocaleIdentifier: LocaleId = LocaleId("en_US"),
    vararg component: PaywallComponent,
): PaywallState.Loaded.Components =
    FakePaywallState(
        components = component.toList(),
        localizations = localizations,
        defaultLocaleIdentifier = defaultLocaleIdentifier
    )

internal fun FakePaywallState(
    components: List<PaywallComponent> = emptyList(),
    packages: List<Package> = emptyList(),
    localizations: NonEmptyMap<LocaleId, LocalizationDictionary> = nonEmptyMapOf(
        LocaleId("en_US") to nonEmptyMapOf(
            LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
        )
    ),
    defaultLocaleIdentifier: LocaleId = LocaleId("en_US"),
): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(components = components),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleIdentifier,
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = packages,
        paywallComponents = data,
    )
    val validated = data.validate().getOrThrow()
    return offering.toComponentsPaywallState(validated)
}
