package com.revenuecat.purchases

/**
 * Builds an explicitly empty [UiConfig] for tests. `ui_config` decoding no longer falls back to a default when
 * fields are missing, so tests that just need a placeholder config construct one in memory via this helper.
 */
@OptIn(InternalRevenueCatAPI::class)
internal fun emptyUiConfig(): UiConfig = uiConfigWithFonts(emptyMap())

/**
 * Builds a [UiConfig] whose only meaningful content is [fonts], with every other required field empty. Useful for
 * font pre-download tests that only care about the fonts declared in the config.
 */
@OptIn(InternalRevenueCatAPI::class)
internal fun uiConfigWithFonts(fonts: Map<FontAlias, UiConfig.AppConfig.FontsConfig>): UiConfig =
    UiConfig(
        app = UiConfig.AppConfig(colors = emptyMap(), fonts = fonts),
        localizations = emptyMap(),
        variableConfig = UiConfig.VariableConfig(
            variableCompatibilityMap = emptyMap(),
            functionCompatibilityMap = emptyMap(),
        ),
    )
