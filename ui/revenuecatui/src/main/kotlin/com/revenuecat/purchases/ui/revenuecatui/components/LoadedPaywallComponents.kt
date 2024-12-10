@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import android.os.LocaleList
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toResourceProvider
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
