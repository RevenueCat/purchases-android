package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.offerEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedCarouselComponentState(
    style: CarouselComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): CarouselComponentState =
    rememberUpdatedCarouselComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        selectedTabIndexProvider = { paywallState.selectedTabIndex },
        selectedPackageUniqueIdProvider = { paywallState.selectedPackageInfo?.uniqueId },
        selectedOfferEligibilityProvider = {
            paywallState.selectedPackageInfo?.resolvedOffer?.offerEligibility ?: OfferEligibility.Ineligible
        },
    )

@Suppress("LongParameterList")
@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedCarouselComponentState(
    style: CarouselComponentStyle,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
    selectedPackageUniqueIdProvider: () -> String? = { null },
    selectedOfferEligibilityProvider: () -> OfferEligibility = { OfferEligibility.Ineligible },
): CarouselComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        CarouselComponentState(
            initialWindowSize = windowSize,
            style = style,
            selectedPackageProvider = selectedPackageProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedPackageUniqueIdProvider = selectedPackageUniqueIdProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
        )
    }
}

@Suppress("LongParameterList")
@Stable
internal class CarouselComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: CarouselComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedPackageUniqueIdProvider: () -> String?,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
) {

    private var windowSize by mutableStateOf(initialWindowSize)
    private val selected by derivedStateOf {
        if (style.rcPackage != null) {
            style.rcPackage.identifier == selectedPackageProvider()?.identifier
        } else if (style.tabIndex != null) {
            style.tabIndex == selectedTabIndexProvider()
        } else {
            false
        }
    }

    /**
     * The offer eligibility for this component, encoding both offer type (intro/promo) and phase count.
     * If the style has its own package, uses the promo offer flag to determine the eligibility type.
     * Otherwise, uses the selected package's resolved offer eligibility.
     */
    private val offerEligibility by derivedStateOf {
        if (style.rcPackage != null) {
            // When isPromoOffer is true, the PromoOffer condition should match
            if (style.isPromoOffer) {
                OfferEligibility.PromoOfferIneligible
            } else {
                style.rcPackage.offerEligibility
            }
        } else {
            selectedOfferEligibilityProvider()
        }
    }

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides.buildPresentedPartial(windowCondition, offerEligibility, componentState)
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @get:JvmSynthetic
    val initialPageIndex by derivedStateOf { presentedPartial?.partial?.initialPageIndex ?: style.initialPageIndex }

    @get:JvmSynthetic
    val pages by derivedStateOf { style.pages }

    @get:JvmSynthetic
    val pageAlignment by derivedStateOf {
        presentedPartial?.partial?.pageAlignment?.toAlignment() ?: style.pageAlignment
    }

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val pagePeek by derivedStateOf { presentedPartial?.partial?.pagePeek?.dp ?: style.pagePeek }

    @get:JvmSynthetic
    val background by derivedStateOf { presentedPartial?.backgroundStyles ?: style.background }

    @get:JvmSynthetic
    val pageSpacing by derivedStateOf { presentedPartial?.partial?.pageSpacing?.dp ?: style.pageSpacing }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val shape by derivedStateOf { presentedPartial?.partial?.shape?.toShape() ?: style.shape.toShape() }

    @get:JvmSynthetic
    val border by derivedStateOf { presentedPartial?.borderStyles ?: style.border }

    @get:JvmSynthetic
    val shadow by derivedStateOf { presentedPartial?.shadowStyles ?: style.shadow }

    @get:JvmSynthetic
    val pageControl by derivedStateOf { presentedPartial?.pageControlStyles ?: style.pageControl }

    @get:JvmSynthetic
    val loop by derivedStateOf { presentedPartial?.partial?.loop ?: style.loop }

    @get:JvmSynthetic
    val autoAdvance by derivedStateOf { presentedPartial?.partial?.autoAdvance ?: style.autoAdvance }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
    }
}
