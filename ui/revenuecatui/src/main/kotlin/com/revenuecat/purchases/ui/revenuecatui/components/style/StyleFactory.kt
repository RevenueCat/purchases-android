package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
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
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.SystemFontFamily
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.string
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

internal class StyleFactory(
    private val windowSize: ScreenCondition,
    private val isEligibleForIntroOffer: Boolean,
    private val componentState: ComponentViewState,
    private val localizationDictionary: LocalizationDictionary,
) {

    private companion object {
        private const val DEFAULT_SPACING = 0f
        private val DEFAULT_SHAPE = RectangleShape
    }

    @Composable
    fun create(component: PaywallComponent): Result<ComponentStyle, NonEmptyList<PaywallValidationError>> =
        when (component) {
            is ButtonComponent -> TODO("ButtonComponentStyle is not yet implemented.")
            is ImageComponent -> TODO("ImageComponentStyle is not yet implemented.")
            is PackageComponent -> TODO("PackageComponentStyle is not yet implemented.")
            is PurchaseButtonComponent -> TODO("PurchaseButtonComponentStyle is not yet implemented.")
            is StackComponent -> createStackComponentStyle(component = component)
            is StickyFooterComponent -> TODO("StickyFooterComponentStyle is not yet implemented.")
            is TextComponent -> createTextComponentStyle(component = component)
        }

    @Composable
    private fun createStackComponentStyle(
        component: StackComponent,
    ): Result<StackComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            ?.toPresentedOverrides { partial -> Result.Success(PresentedStackPartial(partial)) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        // Build all children styles.
        second = component.components
            .map { create(it) }
            .mapOrAccumulate { it },
    ) { presentedOverrides, children ->
        // Combine them into our StackComponentStyle.
        val partial = presentedOverrides?.buildPresentedPartial(
            windowSize = windowSize,
            isEligibleForIntroOffer = isEligibleForIntroOffer,
            state = componentState,
        )?.partial

        StackComponentStyle(
            visible = partial?.visible ?: true,
            children = children,
            dimension = partial?.dimension ?: component.dimension,
            size = partial?.size ?: component.size,
            spacing = (partial?.spacing ?: component.spacing ?: DEFAULT_SPACING).dp,
            backgroundColor = partial?.backgroundColor ?: component.backgroundColor,
            padding = (partial?.padding ?: component.padding).toPaddingValues(),
            margin = (partial?.margin ?: component.margin).toPaddingValues(),
            shape = (partial?.shape ?: component.shape)?.toShape() ?: DEFAULT_SHAPE,
            border = partial?.border ?: component.border,
            shadow = partial?.shadow ?: component.shadow,
        )
    }

    private fun createTextComponentStyle(
        component: TextComponent,
    ): Result<TextComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Get our text from the localization dictionary.
        first = localizationDictionary.string(component.text).mapError { nonEmptyListOf(it) },
        second = component.overrides
            // Map all overrides to PresentedOverrides.
            ?.toPresentedOverrides { LocalizedTextPartial(from = it, using = localizationDictionary) }
            .orSuccessfullyNull()
            // Pick a single PresentedPartial to show.
            .map { it?.buildPresentedPartial(windowSize, isEligibleForIntroOffer, componentState) }
            .mapError { nonEmptyListOf(it) },
    ) { text, presentedPartial ->
        // Combine the text and PresentedPartial into a TextComponentStyle.
        val partial = presentedPartial?.partial
        val weight = (partial?.fontWeight ?: component.fontWeight).toFontWeight()

        TextComponentStyle(
            visible = partial?.visible ?: true,
            text = presentedPartial?.text ?: text,
            color = partial?.color ?: component.color,
            fontSize = partial?.fontSize ?: component.fontSize,
            fontWeight = weight,
            fontFamily = (partial?.fontName ?: component.fontName)?.let { SystemFontFamily(it, weight) },
            textAlign = (partial?.horizontalAlignment ?: component.horizontalAlignment).toTextAlign(),
            horizontalAlignment = (partial?.horizontalAlignment ?: component.horizontalAlignment).toAlignment(),
            backgroundColor = partial?.backgroundColor ?: component.backgroundColor,
            size = partial?.size ?: component.size,
            padding = (partial?.padding ?: component.padding).toPaddingValues(),
            margin = (partial?.margin ?: component.margin).toPaddingValues(),
        )
    }
}
