package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.UiConfig.AppConfig
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.UiConfig as ActualUiConfig

/**
 * Same as the production-code namesake, but with some actual variable localizations to make test code easier to write.
 */
@Suppress("TestFunctionName")
internal fun UiConfig(
    app: AppConfig = AppConfig(),
    localizations: Map<LocaleId, Map<VariableLocalizationKey, String>> = mapOf(
        LocaleId("en_US") to variableLocalizationKeysForEnUs()
    ),
    variableConfig: VariableConfig = VariableConfig(),
): ActualUiConfig =
    ActualUiConfig(
        app = app,
        localizations = localizations,
        variableConfig = variableConfig
    )
