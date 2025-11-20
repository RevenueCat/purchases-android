package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenConditionSnapshot
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

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
        screenConditionProvider = { paywallState.screenConditionSnapshot },
        hasAnyIntroOfferEligiblePackage = paywallState.hasAnyIntroOfferEligiblePackage,
        hasAnyMultipleIntroOffersEligiblePackage = paywallState.hasAnyMultipleIntroOffersEligiblePackage,
    )

@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedCarouselComponentState(
    style: CarouselComponentStyle,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
    screenConditionProvider: () -> ScreenConditionSnapshot,
    hasAnyIntroOfferEligiblePackage: Boolean = false,
    hasAnyMultipleIntroOffersEligiblePackage: Boolean = false,
): CarouselComponentState {
    val screenCondition = screenConditionProvider()

    return remember(style) {
        CarouselComponentState(
            initialScreenCondition = screenCondition,
            style = style,
            selectedPackageProvider = selectedPackageProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            hasAnyIntroOfferEligiblePackage = hasAnyIntroOfferEligiblePackage,
            hasAnyMultipleIntroOffersEligiblePackage = hasAnyMultipleIntroOffersEligiblePackage,
        )
    }.apply {
        update(
            screenCondition = screenCondition,
        )
    }
}

@Stable
internal class CarouselComponentState(
    initialScreenCondition: ScreenConditionSnapshot,
    private val style: CarouselComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
    private val hasAnyIntroOfferEligiblePackage: Boolean,
    private val hasAnyMultipleIntroOffersEligiblePackage: Boolean,
) {

    private var screenConditionSnapshot by mutableStateOf(initialScreenCondition)
    private val selected by derivedStateOf {
        if (style.rcPackage != null) {
            style.rcPackage.identifier == selectedPackageProvider()?.identifier
        } else if (style.tabIndex != null) {
            style.tabIndex == selectedTabIndexProvider()
        } else {
            false
        }
    }

    private val applicablePackage by derivedStateOf {
        style.rcPackage ?: selectedPackageProvider()
    }

    private val presentedPartial by derivedStateOf {
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT
        val introOfferEligibility = applicablePackage?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

        style.overrides.buildPresentedPartial(
            screenCondition = screenConditionSnapshot,
            introOfferEligibility = introOfferEligibility,
            hasAnyIntroOfferEligiblePackage = hasAnyIntroOfferEligiblePackage,
            hasAnyMultipleIntroOffersEligiblePackage = hasAnyMultipleIntroOffersEligiblePackage,
            state = componentState,
            selectedPackageIdentifier = applicablePackage?.identifier,
        )
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
        screenCondition: ScreenConditionSnapshot? = null,
    ) {
        if (screenCondition != null) this.screenConditionSnapshot = screenCondition
    }
}
