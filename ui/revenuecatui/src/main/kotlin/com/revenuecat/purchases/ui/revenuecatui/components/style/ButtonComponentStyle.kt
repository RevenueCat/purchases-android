package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction

@Immutable
internal class ButtonComponentStyle private constructor(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val action: PaywallAction,
    @get:JvmSynthetic
    val actionHandler: (PaywallAction) -> Unit,
) : ComponentStyle {

    companion object {

        @JvmSynthetic
        @Composable
        operator fun invoke(
            stackComponentStyle: StackComponentStyle,
            action: PaywallAction,
            actionHandler: (PaywallAction) -> Unit,
        ): ButtonComponentStyle {
            return ButtonComponentStyle(
                stackComponentStyle = stackComponentStyle,
                action = action,
                actionHandler = actionHandler,
            )
        }
    }
}
