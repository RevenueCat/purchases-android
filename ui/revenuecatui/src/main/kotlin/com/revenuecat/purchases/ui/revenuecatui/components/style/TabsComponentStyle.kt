@file:Suppress("LongParameterList")

package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTabsPartial
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList

@Immutable
internal class TabControlButtonComponentStyle(
    @get:JvmSynthetic
    val tabIndex: Int,
    @get:JvmSynthetic
    val stack: StackComponentStyle,
) : ComponentStyle {
    override val size: Size = stack.size
}

@Immutable
internal class TabControlToggleComponentStyle(
    @get:JvmSynthetic
    val defaultValue: Boolean,
    @get:JvmSynthetic
    val thumbColorOn: ColorStyles,
    @get:JvmSynthetic
    val thumbColorOff: ColorStyles,
    @get:JvmSynthetic
    val trackColorOn: ColorStyles,
    @get:JvmSynthetic
    val trackColorOff: ColorStyles,
) : ComponentStyle {
    override val size: Size = Size(width = Fit, height = Fit)
}

@Immutable
internal class TabsComponentStyle(
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val backgroundColor: ColorStyles?,
    @get:JvmSynthetic
    val shape: Shape,
    @get:JvmSynthetic
    val border: BorderStyles?,
    @get:JvmSynthetic
    val shadow: ShadowStyles?,
    @get:JvmSynthetic
    val control: TabControl,
    @get:JvmSynthetic
    val tabs: NonEmptyList<Tab>,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedTabsPartial>?,
) : ComponentStyle {

    @Immutable
    class Tab(@get:JvmSynthetic val stack: StackComponentStyle)

    @Immutable
    sealed interface TabControl {
        @Immutable
        class Buttons(@get:JvmSynthetic val stack: StackComponentStyle) : TabControl

        @Immutable
        class Toggle(@get:JvmSynthetic val stack: StackComponentStyle) : TabControl
    }
}
