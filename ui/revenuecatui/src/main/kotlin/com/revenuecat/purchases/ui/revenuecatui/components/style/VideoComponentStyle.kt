package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedVideoPartial
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

@Suppress("LongParameterList")
@Immutable
internal data class VideoComponentStyle(
    @get:JvmSynthetic
    val sources: NonEmptyMap<LocaleId, ThemeVideoUrls>,
    @get:JvmSynthetic
    val fallbackSources: NonEmptyMap<LocaleId, ThemeImageUrls>?,
    @get:JvmSynthetic
    val showControls: Boolean,
    @get:JvmSynthetic
    val autoplay: Boolean,
    @get:JvmSynthetic
    val loop: Boolean,
    @get:JvmSynthetic
    val muteAudio: Boolean,
    @get:JvmSynthetic
    override val size: Size,
    @get:JvmSynthetic
    override val visible: Boolean,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val shape: Shape?,
    @get:JvmSynthetic
    val border: BorderStyles?,
    @get:JvmSynthetic
    val shadow: ShadowStyles?,
    @get:JvmSynthetic
    val overlay: ColorStyles?,
    @get:JvmSynthetic
    val contentScale: ContentScale,
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
     * available. This should only be set for video inside tab control elements. Not for all video within a tab.
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
    /**
     * This will be read by the parent stack, if that `StackComponentStyle` has `applyTopWindowInsets` set to true.
     */
    @get:JvmSynthetic
    val ignoreTopWindowInsets: Boolean = false,
    @get:JvmSynthetic
    val overrides: List<PresentedOverride<PresentedVideoPartial>>,
) : ComponentStyle, PackageContext
