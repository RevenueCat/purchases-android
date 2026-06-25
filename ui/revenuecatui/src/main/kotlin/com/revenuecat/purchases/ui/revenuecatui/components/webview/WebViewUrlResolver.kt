@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessorV2
import java.net.URL
import java.util.Locale

internal object WebViewUrlResolver {

    fun resolve(
        urlTemplate: String,
        state: PaywallState.Loaded.Components,
    ): URL? = resolve(
        urlTemplate = urlTemplate,
        variableConfig = state.variableConfig,
        customVariables = state.customVariables,
        defaultCustomVariables = state.defaultCustomVariables,
        locale = state.locale.toJavaLocale(),
    )

    fun resolve(
        urlTemplate: String,
        variableConfig: UiConfig.VariableConfig,
        customVariables: Map<String, CustomVariableValue>,
        defaultCustomVariables: Map<String, CustomVariableValue>,
        locale: Locale,
    ): URL? {
        val resolvedUrl = VariableProcessorV2.processVariables(
            template = urlTemplate,
            variableConfig = variableConfig,
            dateLocale = locale,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )

        return runCatching { URL(resolvedUrl) }
            .getOrNull()
            ?.takeIf { it.protocol == HTTPS_SCHEME && it.host.isNotBlank() }
    }

    private const val HTTPS_SCHEME = "https"
}
