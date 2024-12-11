@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
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
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
import java.net.URL
import java.util.Locale

@Composable
internal fun LoadedPaywallComponents(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // Configured locales take precedence over the default one.
    val preferredIds = configuration.locales.mapToLocaleIds() + state.data.defaultLocaleIdentifier
    // Find the first locale we have a LocalizationDictionary for.
    val localeId = preferredIds.first { id -> state.data.componentsLocalizations.containsKey(id) }
    val localizationDictionary = state.data.componentsLocalizations.getValue(localeId)
    val locale = localeId.toLocale()

    val windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val windowSize = ScreenCondition.from(windowSizeClass.windowWidthSizeClass)

    val styleFactory = remember(locale, windowSize) {
        StyleFactory(
            windowSize = windowSize,
            isEligibleForIntroOffer = false,
            componentState = ComponentViewState.DEFAULT,
            packageContext = PackageContext(
                initialSelectedPackage = null,
                initialVariableContext = PackageContext.VariableContext(
                    packages = state.offering.availablePackages,
                    showZeroDecimalPlacePrices = true,
                ),
            ),
            localizationDictionary = localizationDictionary,
            locale = locale,
            variables = VariableDataProvider(context.toResourceProvider()),
        )
    }

    val config = state.data.componentsConfig.base
    val style = styleFactory.create(config.stack).getOrThrow()
    val background = config.background.toBackgroundStyle()

    ComponentView(
        style = style,
        modifier = modifier
            .background(background),
    )
}

private fun LocaleList.mapToLocaleIds(): List<LocaleId> {
    val result = ArrayList<LocaleId>(size())
    for (i in 0 until size()) {
        val locale = get(i)
        if (locale != null) result.add(locale.toLocaleId())
    }
    return result
}

private fun LocaleId.toLocale(): Locale =
    Locale.forLanguageTag(value)

private fun Locale.toLocaleId(): LocaleId =
    LocaleId(toLanguageTag())

@Suppress("LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun LoadedPaywallComponents_Preview() {
    LoadedPaywallComponents(
        state = PaywallState.Loaded.Components(
            offering = Offering(
                identifier = "id",
                serverDescription = "description",
                metadata = emptyMap(),
                availablePackages = emptyList(),
                paywall = null,
            ),
            data = PaywallComponentsData(
                templateName = "template",
                assetBaseURL = URL("https://assets.pawwalls.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("hello-world"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                ),
                            ),
                            dimension = Vertical(alignment = CENTER, distribution = START),
                            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                        ),
                        background = Background.Color(
                            ColorScheme(
                                light = ColorInfo.Hex(Color.Blue.toArgb()),
                                dark = ColorInfo.Hex(Color.Red.toArgb()),
                            ),
                        ),
                        stickyFooter = StickyFooterComponent(
                            stack = StackComponent(
                                components = listOf(
                                    TextComponent(
                                        text = LocalizationKey("hello-world"),
                                        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                    ),
                                ),
                                dimension = Vertical(alignment = CENTER, distribution = START),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                                shape = Shape.Rectangle(
                                    corners = CornerRadiuses(
                                        topLeading = 10.0,
                                        topTrailing = 10.0,
                                        bottomLeading = 0.0,
                                        bottomTrailing = 0.0,
                                    ),
                                ),
                                shadow = Shadow(
                                    ColorScheme(
                                        light = ColorInfo.Hex(Color.Black.toArgb()),
                                        dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                                    ),
                                    radius = 10.0,
                                    x = 0.0,
                                    y = -5.0,
                                ),
                            ),
                        ),
                    ),
                ),
                componentsLocalizations = mapOf(
                    LocaleId("en_US") to mapOf(
                        LocalizationKey("hello-world") to LocalizationData.Text("Hello, world!"),
                    ),
                ),
                defaultLocaleIdentifier = LocaleId("en_US"),
            ),
        ),
        modifier = Modifier
            .fillMaxSize(),
    )
}
