package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@JvmSynthetic
@Composable
internal fun rememberUpdatedTimelineComponentState(
    style: TimelineComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TimelineComponentState =
    rememberUpdatedTimelineComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
    )

@JvmSynthetic
@Composable
private fun rememberUpdatedTimelineComponentState(
    style: TimelineComponentStyle,
    selectedPackageProvider: () -> Package?,
): TimelineComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        TimelineComponentState(
            initialWindowSize = windowSize,
            style = style,
            selectedPackageProvider = selectedPackageProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
        )
    }
}

@Stable
internal class TimelineComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: TimelineComponentStyle,
    private val selectedPackageProvider: () -> Package?,
) {

    private var windowSize by mutableStateOf(initialWindowSize)
    private val selected by derivedStateOf {
        if (style.rcPackage != null) style.rcPackage.identifier == selectedPackageProvider()?.identifier else false
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
    val itemSpacing by derivedStateOf { presentedPartial?.partial?.itemSpacing ?: style.itemSpacing }

    @get:JvmSynthetic
    val textSpacing by derivedStateOf { presentedPartial?.partial?.textSpacing ?: style.textSpacing }

    @get:JvmSynthetic
    val columnGutter by derivedStateOf { presentedPartial?.partial?.columnGutter ?: style.columnGutter }

    @get:JvmSynthetic
    val iconAlignment by derivedStateOf { presentedPartial?.partial?.iconAlignment ?: style.iconAlignment }

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val items by derivedStateOf { style.items.map { ItemState(initialWindowSize, it, selectedPackageProvider) } }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
    }

    @Stable
    class ItemState(
        initialWindowSize: WindowWidthSizeClass,
        private val style: TimelineComponentStyle.ItemStyle,
        private val selectedPackageProvider: () -> Package?,
    ) {

        private var windowSize by mutableStateOf(initialWindowSize)
        private val selected by derivedStateOf {
            if (style.rcPackage != null) style.rcPackage.identifier == selectedPackageProvider()?.identifier else false
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
        val title by derivedStateOf { style.title }

        @get:JvmSynthetic
        val description by derivedStateOf { style.description }

        @get:JvmSynthetic
        val icon by derivedStateOf { style.icon }

        @get:JvmSynthetic
        val connector by derivedStateOf { presentedPartial?.connectorStyle ?: style.connector }

        @JvmSynthetic
        fun update(
            windowSize: WindowWidthSizeClass? = null,
        ) {
            if (windowSize != null) this.windowSize = windowSize
        }
    }
}
