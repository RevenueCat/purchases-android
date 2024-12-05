package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.string
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import java.util.Locale

@Suppress("LongParameterList")
internal class StyleFactory(
    private val windowSize: ScreenCondition,
    private val isEligibleForIntroOffer: Boolean,
    private val componentState: ComponentViewState,
    private val packageContext: PackageContext,
    private val localizationDictionary: LocalizationDictionary,
    private val locale: Locale,
    private val variables: VariableDataProvider,
) {

    private companion object {
        private const val DEFAULT_SPACING = 0f
        private val DEFAULT_SHAPE = RectangleShape
    }

    @Composable
    fun create(component: PaywallComponent): Result<ComponentStyle, List<PaywallValidationError>> =
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
    ): Result<StackComponentStyle, List<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            ?.toPresentedOverrides { partial -> Result.Success(PresentedStackPartial(partial)) }
            .orSuccessfullyNull()
            .mapError { listOf(it) },
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
            background = (partial?.backgroundColor ?: component.backgroundColor)?.toBackgroundStyle(),
            padding = (partial?.padding ?: component.padding).toPaddingValues(),
            margin = (partial?.margin ?: component.margin).toPaddingValues(),
            shape = (partial?.shape ?: component.shape)?.toShape() ?: DEFAULT_SHAPE,
            border = (partial?.border ?: component.border)?.toBorderStyle(),
            shadow = (partial?.shadow ?: component.shadow)?.toShadowStyle(),
        )
    }

    @Composable
    private fun createTextComponentStyle(
        component: TextComponent,
    ): Result<TextComponentStyle, List<PaywallValidationError>> = zipOrAccumulate(
        // Get our text from the localization dictionary.
        first = localizationDictionary.string(component.text).mapError { listOf(it) },
        second = component.overrides
            // Map all overrides to PresentedOverrides.
            ?.toPresentedOverrides { LocalizedTextPartial(from = it, using = localizationDictionary) }
            .orSuccessfullyNull()
            // Pick a single PresentedPartial to show.
            .map { it?.buildPresentedPartial(windowSize, isEligibleForIntroOffer, componentState) }
            .mapError { listOf(it) },
    ) { text, presentedPartial ->
        // Combine the text and PresentedPartial into a TextComponentStyle.
        val partial = presentedPartial?.partial

        TextComponentStyle(
            visible = partial?.visible ?: true,
            text = rememberProcessedText(
                originalText = presentedPartial?.text ?: text,
                packageContext = packageContext,
                locale = locale,
                variables = variables,
            ),
            color = partial?.color ?: component.color,
            fontSize = partial?.fontSize ?: component.fontSize,
            fontWeight = partial?.fontWeight ?: component.fontWeight,
            fontFamily = partial?.fontName ?: component.fontName,
            textAlign = partial?.horizontalAlignment ?: component.horizontalAlignment,
            horizontalAlignment = partial?.horizontalAlignment ?: component.horizontalAlignment,
            backgroundColor = partial?.backgroundColor ?: component.backgroundColor,
            size = partial?.size ?: component.size,
            padding = partial?.padding ?: component.padding,
            margin = partial?.margin ?: component.margin,
        )
    }

    /**
     * Replaces any [variables] in the [originalText] with values based on the currently selected
     * [package][PackageContext.selectedPackage] and [locale].
     */
    @Composable
    private fun rememberProcessedText(
        originalText: String,
        packageContext: PackageContext,
        variables: VariableDataProvider,
        locale: Locale,
    ): String {
        val processedText by remember(packageContext) {
            derivedStateOf {
                packageContext.selectedPackage?.let { selectedPackage ->
                    val discount = discountPercentage(
                        pricePerMonthMicros = selectedPackage.product.pricePerMonth()?.amountMicros,
                        mostExpensiveMicros = packageContext.variableContext.mostExpensivePricePerMonthMicros,
                    )
                    val variableContext: VariableProcessor.PackageContext = VariableProcessor.PackageContext(
                        discountRelativeToMostExpensivePerMonth = discount,
                        showZeroDecimalPlacePrices = packageContext.variableContext.showZeroDecimalPlacePrices,
                    )
                    VariableProcessor.processVariables(
                        variableDataProvider = variables,
                        context = variableContext,
                        originalString = originalText,
                        rcPackage = selectedPackage,
                        locale = locale,
                    )
                } ?: originalText
            }
        }

        return processedText
    }

    private fun discountPercentage(pricePerMonthMicros: Long?, mostExpensiveMicros: Long?): Double? {
        if (pricePerMonthMicros == null ||
            mostExpensiveMicros == null ||
            mostExpensiveMicros <= pricePerMonthMicros
        ) {
            return null
        }

        return (mostExpensiveMicros - pricePerMonthMicros) / mostExpensiveMicros.toDouble()
    }
}
