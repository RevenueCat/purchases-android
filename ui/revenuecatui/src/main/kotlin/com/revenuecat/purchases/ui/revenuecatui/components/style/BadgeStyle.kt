package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import dev.drewhamilton.poko.Poko

@Poko
@Immutable
internal class BadgeStyle(
    @get:JvmSynthetic
    val stackStyle: StackComponentStyle,
    @get:JvmSynthetic
    val style: Badge.Style,
    @get:JvmSynthetic
    val alignment: TwoDimensionalAlignment,
)

@JvmSynthetic
internal fun Badge.toBadgeStyle(
    createStackComponentStyle: (StackComponent) -> Result<StackComponentStyle, NonEmptyList<PaywallValidationError>>,
): Result<BadgeStyle, NonEmptyList<PaywallValidationError>> =
    createStackComponentStyle(stack).map { stackStyle ->
        BadgeStyle(
            stackStyle = stackStyle,
            style = style,
            alignment = alignment,
        )
    }
