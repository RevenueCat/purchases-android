package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError

internal sealed interface PaywallValidationResult {
    val errors: NonEmptyList<PaywallValidationError>?

    data class Legacy(
        val displayablePaywall: PaywallData,
        val template: PaywallTemplate,
        override val errors: NonEmptyList<PaywallValidationError>? = null,
    ) : PaywallValidationResult {
        constructor(
            displayablePaywall: PaywallData,
            template: PaywallTemplate,
            error: PaywallValidationError,
        ) : this(
            displayablePaywall = displayablePaywall,
            template = template,
            errors = nonEmptyListOf(error),
        )
    }

    data class Components(
        val stack: ComponentStyle,
        val stickyFooter: ComponentStyle?,
        val background: BackgroundStyles,
        /**
         * All locales that this paywall supports, with `locales.head` being the default one.
         */
        val locales: NonEmptySet<LocaleId>,
        /**
         * Any countries whose currencies should not show any decimals when displaying prices.
         */
        val zeroDecimalPlaceCountries: Set<String>,
        val variableConfig: UiConfig.VariableConfig,
    ) : PaywallValidationResult {
        // If a Components Paywall has an error, it will be reflected as a Legacy type so we can use the Legacy
        // fallback.
        override val errors: NonEmptyList<PaywallValidationError>? = null
    }
}
