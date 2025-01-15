package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial

@Suppress("LongParameterList")
@Immutable
internal data class StackComponentStyle(
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
    val border: Border?,
    @get:JvmSynthetic
    val shadow: Shadow?,
    @get:JvmSynthetic
    val badge: BadgeStyle?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedStackPartial>?,
) : ComponentStyle
