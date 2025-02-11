package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles

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
    val backgroundColor: ColorStyles?,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val shape: Shape,
    @get:JvmSynthetic
    val border: BorderStyles?,
    @get:JvmSynthetic
    val shadow: ShadowStyles?,
    @get:JvmSynthetic
    val badge: BadgeStyle?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for stacks inside tab control elements. Not for all stacks within a tab.
     */
    @get:JvmSynthetic
    val tabIndex: Int?,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedStackPartial>>,
    /**
     * Will cause this stack to apply the top window insets to all children.
     */
    @get:JvmSynthetic
    val applyTopWindowInsets: Boolean = false,
) : ComponentStyle
