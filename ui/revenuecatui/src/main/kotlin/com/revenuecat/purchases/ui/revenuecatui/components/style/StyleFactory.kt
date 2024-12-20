package com.revenuecat.purchases.ui.revenuecatui.components.style

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
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
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

    fun create(
        component: PaywallComponent,
        actionHandler: suspend (PaywallAction) -> Unit,
    ): Result<ComponentStyle, NonEmptyList<PaywallValidationError>> =
        when (component) {
            is ButtonComponent -> createButtonComponentStyle(component, actionHandler)
            is ImageComponent -> TODO("ImageComponentStyle is not yet implemented.")
            is PackageComponent -> TODO("PackageComponentStyle is not yet implemented.")
            is PurchaseButtonComponent -> createPurchaseButtonComponentStyle(component, actionHandler)
            is StackComponent -> createStackComponentStyle(component, actionHandler)
            is StickyFooterComponent -> createStickyFooterComponentStyle(component, actionHandler)
            is TextComponent -> createTextComponentStyle(component = component)
        }

    fun createStickyFooterComponentStyle(
        component: StickyFooterComponent,
        actionHandler: suspend (PaywallAction) -> Unit,
    ): Result<StickyFooterComponentStyle, NonEmptyList<PaywallValidationError>> =
        createStackComponentStyle(component.stack, actionHandler).map {
            StickyFooterComponentStyle(stackComponentStyle = it)
        }

    private fun createButtonComponentStyle(
        component: ButtonComponent,
        actionHandler: suspend (PaywallAction) -> Unit,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = createStackComponentStyle(
        component.stack,
        actionHandler,
    ).map {
        ButtonComponentStyle(
            stackComponentStyle = it,
            action = component.action.mapButtonComponentActionToPaywallAction(),
            actionHandler = actionHandler,
        )
    }

    private fun createPurchaseButtonComponentStyle(
        component: PurchaseButtonComponent,
        actionHandler: suspend (PaywallAction) -> Unit,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = createStackComponentStyle(
        component.stack,
        actionHandler,
    ).map {
        ButtonComponentStyle(
            stackComponentStyle = it,
            action = PaywallAction.PurchasePackage,
            actionHandler = actionHandler,
        )
    }

    private fun ButtonComponent.Action.mapButtonComponentActionToPaywallAction(): PaywallAction {
        return when (this) {
            ButtonComponent.Action.NavigateBack -> PaywallAction.NavigateBack
            ButtonComponent.Action.RestorePurchases -> PaywallAction.RestorePurchases
            is ButtonComponent.Action.NavigateTo -> PaywallAction.NavigateTo(destination)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createStackComponentStyle(
        component: StackComponent,
        actionHandler: suspend (PaywallAction) -> Unit,
    ): Result<StackComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            ?.toPresentedOverrides { partial -> Result.Success(PresentedStackPartial(partial)) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        // Build all children styles.
        second = component.components
            .map { create(it, actionHandler) }
            .mapOrAccumulate { it },
    ) { presentedOverrides, children ->
        // Combine them into our StackComponentStyle.
        val partial = presentedOverrides?.buildPresentedPartial(
            windowSize = windowSize,
            isEligibleForIntroOffer = isEligibleForIntroOffer,
            state = componentState,
        )?.partial

        val badge = partial?.badge ?: component.badge
        val badgeStyle = badge?.let {
            val stackComponentStyle = when (val stackComponentStyleResult = createStackComponentStyle(it.stack)) {
                is Result.Success -> stackComponentStyleResult.value
                is Result.Error -> return stackComponentStyleResult
            }
            BadgeStyle(
                stackStyle = stackComponentStyle,
                style = it.style,
                alignment = it.alignment,
            )
        }

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
            badge = badgeStyle,
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
            .mapError { nonEmptyListOf(it) },
    ) { text, presentedOverrides ->
        val weight = component.fontWeight.toFontWeight()
        TextComponentStyle(
            visible = true,
            text = text,
            color = component.color,
            fontSize = component.fontSize,
            fontWeight = weight,
            fontFamily = component.fontName?.let { SystemFontFamily(it, weight) },
            textAlign = component.horizontalAlignment.toTextAlign(),
            horizontalAlignment = component.horizontalAlignment.toAlignment(),
            backgroundColor = component.backgroundColor,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            overrides = presentedOverrides,
        )
    }
}
