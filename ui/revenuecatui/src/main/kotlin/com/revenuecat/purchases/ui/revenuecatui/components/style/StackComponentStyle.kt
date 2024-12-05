package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import java.util.Locale

private const val DefaultSpacing = 0f
private val DefaultShape = RectangleShape

@Suppress("LongParameterList")
@Immutable
internal class StackComponentStyle(
    @get:JvmSynthetic
    val visible: Boolean,
    @get:JvmSynthetic
    val children: List<ComponentStyle>,
    @get:JvmSynthetic
    val dimension: Dimension,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val spacing: Dp,
    @get:JvmSynthetic
    val background: BackgroundStyle?,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val shape: Shape,
    @get:JvmSynthetic
    val border: BorderStyle?,
    @get:JvmSynthetic
    val shadow: ShadowStyle?,
) : ComponentStyle {

    companion object {

        @JvmSynthetic
        @Composable
        operator fun invoke(
            component: StackComponent,
            windowSize: ScreenCondition,
            isEligibleForIntroOffer: Boolean,
            componentState: ComponentViewState,
            packageContext: PackageContext,
            localizationDictionary: LocalizationDictionary,
            locale: Locale,
            variables: VariableDataProvider,
        ): Result<StackComponentStyle, List<PaywallValidationError>> = zipOrAccumulate(
            // Build the PresentedOverrides.
            first = component.overrides
                ?.toPresentedOverrides { partial -> Result.Success(PresentedStackPartial(partial)) }
                .orSuccessfullyNull()
                .mapError { listOf(it) },
            // Build all children styles.
            second = component.components
                .map {
                    it.toComponentStyle(
                        windowSize = windowSize,
                        isEligibleForIntroOffer = isEligibleForIntroOffer,
                        componentState = componentState,
                        packageContext = packageContext,
                        localizationDictionary = localizationDictionary,
                        locale = locale,
                        variables = variables,
                    )
                }
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
                spacing = (partial?.spacing ?: component.spacing ?: DefaultSpacing).dp,
                background = (partial?.backgroundColor ?: component.backgroundColor)?.toBackgroundStyle(),
                padding = (partial?.padding ?: component.padding).toPaddingValues(),
                margin = (partial?.margin ?: component.margin).toPaddingValues(),
                shape = (partial?.shape ?: component.shape)?.toShape() ?: DefaultShape,
                border = (partial?.border ?: component.border)?.toBorderStyle(),
                shadow = (partial?.shadow ?: component.shadow)?.toShadowStyle(),
            )
        }
    }
}
