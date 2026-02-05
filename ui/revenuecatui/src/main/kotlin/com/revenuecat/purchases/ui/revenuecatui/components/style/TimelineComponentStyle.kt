package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.TimelineComponent.IconAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelineItemPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelinePartial
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

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
    override val visible: Boolean,
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
    override val rcPackage: Package?,
    /**
     * The resolved offer for this package, containing the subscription option and promo offer status.
     * Used to determine offer eligibility and pricing phase information.
     */
    @get:JvmSynthetic
    override val resolvedOffer: ResolvedOffer? = null,
    /**
     * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
     * available. This should only be set for timelines inside tab control elements. Not for all timelines within a tab.
     */
    @get:JvmSynthetic
    override val tabIndex: Int?,
    /**
     * The pre-computed offer eligibility for this component's package context.
     * Used for applying conditional overrides based on intro/promo offer status.
     * Null if this component is not in a package scope.
     */
    @get:JvmSynthetic
    override val offerEligibility: OfferEligibility? = null,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedTimelinePartial>>,
) : ComponentStyle, PackageContext {
    @Immutable
    data class ItemStyle(
        @get:JvmSynthetic
        val title: TextComponentStyle,
        @get:JvmSynthetic
        val visible: Boolean,
        @get:JvmSynthetic
        val description: TextComponentStyle?,
        @get:JvmSynthetic
        val icon: IconComponentStyle,
        @get:JvmSynthetic
        val connector: ConnectorStyle?,
        /**
         * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
         * available.
         */
        @get:JvmSynthetic
        override val rcPackage: Package?,
        /**
         * The resolved offer for this package, containing the subscription option and promo offer status.
         * Used to determine offer eligibility and pricing phase information.
         */
        @get:JvmSynthetic
        override val resolvedOffer: ResolvedOffer? = null,
        /**
         * If this is non-null and equal to the currently selected tab index, the `selected` [overrides] will be used if
         * available. This should only be set for items inside tab control elements. Not for all items within a tab.
         */
        @get:JvmSynthetic
        override val tabIndex: Int?,
        /**
         * The pre-computed offer eligibility for this component's package context.
         * Used for applying conditional overrides based on intro/promo offer status.
         * Null if this component is not in a package scope.
         */
        @get:JvmSynthetic
        override val offerEligibility: OfferEligibility? = null,
        @get:JvmSynthetic
        val overrides: List<PresentedOverride<PresentedTimelineItemPartial>>,
    ) : PackageContext

    @Immutable
    data class ConnectorStyle(
        @get:JvmSynthetic
        val width: Int,
        @get:JvmSynthetic
        val margin: PaddingValues,
        @get:JvmSynthetic
        val color: ColorStyles,
    )
}
