@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import java.util.Locale

/**
 * Marker interface for component styles.
 */
internal sealed interface ComponentStyle

@Suppress("LongParameterList")
@Composable
@JvmSynthetic
internal fun PaywallComponent.toComponentStyle(
    windowSize: ScreenCondition,
    isEligibleForIntroOffer: Boolean,
    componentState: ComponentViewState,
    packageContext: PackageContext,
    localizationDictionary: LocalizationDictionary,
    locale: Locale,
    variables: VariableDataProvider,
): Result<ComponentStyle, List<PaywallValidationError>> =
    when (this) {
        is ButtonComponent -> TODO("ButtonComponentStyle is not yet implemented.")
        is ImageComponent -> TODO("ImageComponentStyle is not yet implemented.")
        is PackageComponent -> TODO("PackageComponentStyle is not yet implemented.")
        is PurchaseButtonComponent -> TODO("PurchaseButtonComponentStyle is not yet implemented.")
        is StackComponent -> StackComponentStyle(
            component = this,
            windowSize = windowSize,
            isEligibleForIntroOffer = isEligibleForIntroOffer,
            componentState = componentState,
            packageContext = packageContext,
            localizationDictionary = localizationDictionary,
            locale = locale,
            variables = variables,
        )
        is StickyFooterComponent -> TODO("StickyFooterComponentStyle is not yet implemented.")
        is TextComponent -> TextComponentStyle(
            component = this,
            windowSize = windowSize,
            isEligibleForIntroOffer = isEligibleForIntroOffer,
            componentState = componentState,
            packageContext = packageContext,
            localizationDictionary = localizationDictionary,
            locale = locale,
            variables = variables,
        )
    }
