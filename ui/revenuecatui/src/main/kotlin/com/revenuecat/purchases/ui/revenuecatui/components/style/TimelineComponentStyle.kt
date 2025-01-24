package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent.IconAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelineIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelinePartial

@Suppress("LongParameterList")
@Immutable
internal data class TimelineComponentStyle(
    @get:JvmSynthetic
    val itemSpacing: Int,
    @get:JvmSynthetic
    val textSpacing: Int,
    @get:JvmSynthetic
    val columnGutter: Int,
    @get:JvmSynthetic
    val iconAlignment: IconAlignment,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val items: List<ItemStyle>,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<PresentedTimelinePartial>?,
) : ComponentStyle {
    @Immutable
    data class ItemStyle(
        @get:JvmSynthetic
        val title: TextComponentStyle,
        @get:JvmSynthetic
        val description: TextComponentStyle?,
        @get:JvmSynthetic
        val icon: IconComponentStyle,
        @get:JvmSynthetic
        val connector: TimelineComponent.Connector?,
        /**
         * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
         * available.
         */
        @get:JvmSynthetic
        val rcPackage: Package?,
        @get:JvmSynthetic
        val overrides: PresentedOverrides<PresentedTimelineIconPartial>?,
    )
}
