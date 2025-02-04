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
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

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
    )

@JvmSynthetic
@Composable
private fun rememberUpdatedCarouselComponentState(
    style: CarouselComponentStyle,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
): CarouselComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        CarouselComponentState(
            initialWindowSize = windowSize,
            style = style,
            selectedPackageProvider = selectedPackageProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
        )
    }
}

@Stable
internal class CarouselComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: CarouselComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
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

    private val applicablePackage by derivedStateOf {
        style.rcPackage ?: selectedPackageProvider()
    }

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT
        val introOfferEligibility = applicablePackage?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

        style.overrides?.buildPresentedPartial(windowCondition, introOfferEligibility, componentState)
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: true }

    @get:JvmSynthetic
    val initialSlideIndex by derivedStateOf { presentedPartial?.partial?.initialSlideIndex ?: style.initialSlideIndex }

    @get:JvmSynthetic
    val slides by derivedStateOf { style.slides }

    @get:JvmSynthetic
    val alignment by derivedStateOf { presentedPartial?.partial?.alignment?.toAlignment() ?: style.alignment }

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val sidePagePeek by derivedStateOf { presentedPartial?.partial?.sidePagePeek?.dp ?: style.sidePagePeek }

    @get:JvmSynthetic
    val backgroundColor by derivedStateOf { presentedPartial?.backgroundColorStyles ?: style.backgroundColor }

    @get:JvmSynthetic
    val spacing by derivedStateOf { presentedPartial?.partial?.spacing?.dp ?: style.spacing }

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
