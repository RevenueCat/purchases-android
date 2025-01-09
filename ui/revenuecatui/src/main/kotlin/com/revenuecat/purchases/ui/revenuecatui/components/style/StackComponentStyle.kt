package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial

@Suppress("LongParameterList")
@Immutable
internal class StackComponentStyle(
    @get:JvmSynthetic
    val children: List<ComponentStyle>,
    @get:JvmSynthetic
    val dimension: Dimension,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val spacing: Dp,
    @get:JvmSynthetic
    val backgroundColor: ColorScheme?,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val shape: Shape,
    @get:JvmSynthetic
    val cornerRadiuses: CornerRadiuses?,
    @get:JvmSynthetic
    val border: Border?,
    @get:JvmSynthetic
    val shadow: Shadow?,
    @get:JvmSynthetic
    val badge: BadgeStyle?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedStackPartial>?,
) : ComponentStyle {
    @JvmSynthetic
    internal fun copy(
        children: List<ComponentStyle> = this.children,
        dimension: Dimension = this.dimension,
        size: Size = this.size,
        spacing: Dp = this.spacing,
        backgroundColor: ColorScheme? = this.backgroundColor,
        padding: PaddingValues = this.padding,
        margin: PaddingValues = this.margin,
        shape: Shape = this.shape,
        cornerRadiuses: CornerRadiuses? = this.cornerRadiuses,
        border: Border? = this.border,
        shadow: Shadow? = this.shadow,
        badge: BadgeStyle? = this.badge,
        overrides: PresentedOverrides<PresentedStackPartial>? = this.overrides,
    ): StackComponentStyle = StackComponentStyle(
        children = children,
        dimension = dimension,
        size = size,
        spacing = spacing,
        backgroundColor = backgroundColor,
        padding = padding,
        margin = margin,
        shape = shape,
        cornerRadiuses = cornerRadiuses,
        border = border,
        shadow = shadow,
        badge = badge,
        overrides = overrides,
    )
}
