package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.BorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.ShadowStyle

@Suppress("LongParameterList")
@Immutable
internal class StackComponentStyle(
    @JvmSynthetic
    val visible: Boolean,
    @JvmSynthetic
    val dimension: Dimension,
    @JvmSynthetic
    val size: Size,
    @JvmSynthetic
    val spacing: Dp,
    @JvmSynthetic
    val background: BackgroundStyle?,
    @JvmSynthetic
    val padding: PaddingValues,
    @JvmSynthetic
    val margin: PaddingValues,
    @JvmSynthetic
    val shape: Shape,
    @JvmSynthetic
    val border: BorderStyle?,
    @JvmSynthetic
    val shadow: ShadowStyle?,
)
