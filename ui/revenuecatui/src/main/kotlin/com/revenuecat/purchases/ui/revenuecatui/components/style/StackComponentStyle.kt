package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import java.util.Date

@Suppress("LongParameterList")
@Immutable
internal data class StackComponentStyle(
    @get:JvmSynthetic
    val children: List<ComponentStyle>,
    @get:JvmSynthetic
    val dimension: Dimension,
    @get:JvmSynthetic
    override val visible: Boolean,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    val spacing: Dp,
    @get:JvmSynthetic
    val background: BackgroundStyles?,
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
    @get:JvmSynthetic
    val scrollOrientation: Orientation?,
    /**
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    /**
     * Unique identifier for the package, combining package ID and offer ID.
     * Used for selection comparison when multiple components reference the same package with different offers.
     * If null, falls back to comparing by [rcPackage]'s identifier for backwards compatibility.
     */
    @get:JvmSynthetic
    val packageUniqueId: String? = null,
    /**
     * Whether this component uses a configured promo offer (Play Store offer ID).
     * Used to determine if the `promo_offer` override condition should apply.
     */
    @get:JvmSynthetic
    val isPromoOffer: Boolean = false,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for stacks inside tab control elements. Not for all stacks within a tab.
     */
    @get:JvmSynthetic
    val tabIndex: Int?,
    /**
     * If this is non-null, it means this stack is inside a countdown component.
     */
    @get:JvmSynthetic
    val countdownDate: Date?,
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedStackPartial>>,
    /**
     * Will cause this stack to apply the top window insets to all children, except ImageComponentStyles that
     * have `ignoreTopWindowInsets` set to true.
     */
    @get:JvmSynthetic
    val applyTopWindowInsets: Boolean = false,
    /**
     * Will cause this stack to apply the bottom window insets to its content.
     */
    @get:JvmSynthetic
    val applyBottomWindowInsets: Boolean = false,
) : ComponentStyle
