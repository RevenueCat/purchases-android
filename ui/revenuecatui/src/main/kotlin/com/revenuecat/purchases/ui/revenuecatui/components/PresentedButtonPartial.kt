package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.runtime.Immutable

@Immutable
internal data class PresentedButtonPartial(
    @get:JvmSynthetic val visible: Boolean?,
) : PresentedPartial<PresentedButtonPartial> {
    override fun combine(with: PresentedButtonPartial?): PresentedButtonPartial =
        PresentedButtonPartial(
            visible = with?.visible ?: visible,
        )
}
