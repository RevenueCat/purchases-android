package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction

@Immutable
internal class ButtonComponentStyle(
    @get:JvmSynthetic
    val stackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val action: PaywallAction,
    @get:JvmSynthetic
    val actionHandler: suspend (PaywallAction) -> Unit,
) : ComponentStyle
