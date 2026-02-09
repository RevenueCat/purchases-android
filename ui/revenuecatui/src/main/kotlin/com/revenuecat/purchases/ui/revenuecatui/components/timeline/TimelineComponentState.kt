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
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageAwareDelegate
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTimelineComponentState(
    style: TimelineComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TimelineComponentState = rememberUpdatedTimelineComponentState(
    style = style,
    selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
    selectedTabIndexProvider = { paywallState.selectedTabIndex },
    selectedOfferEligibilityProvider = { paywallState.selectedOfferEligibility },
)

@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedTimelineComponentState(
    style: TimelineComponentStyle,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
): TimelineComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        TimelineComponentState(
            initialWindowSize = windowSize,
            style = style,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
        )
    }.apply {
        update(windowSize = windowSize)
    }
}

@Stable
internal class TimelineComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: TimelineComponentStyle,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
) {

    private var windowSize by mutableStateOf(initialWindowSize)

    private val packageAwareDelegate = PackageAwareDelegate(
        style = style,
        selectedPackageInfoProvider = selectedPackageInfoProvider,
        selectedTabIndexProvider = selectedTabIndexProvider,
        selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
    )

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState =
            if (packageAwareDelegate.isSelected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides.buildPresentedPartial(windowCondition, packageAwareDelegate.offerEligibility, componentState)
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

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
    val items by derivedStateOf {
        style.items.map { itemStyle ->
            ItemState(
                initialWindowSize = windowSize,
                style = itemStyle,
                selectedPackageInfoProvider = selectedPackageInfoProvider,
                selectedTabIndexProvider = selectedTabIndexProvider,
                selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
            )
        }
    }

    @JvmSynthetic
    fun update(windowSize: WindowWidthSizeClass? = null) {
        if (windowSize != null) this.windowSize = windowSize
    }

    @Stable
    class ItemState(
        initialWindowSize: WindowWidthSizeClass,
        private val style: TimelineComponentStyle.ItemStyle,
        private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
        private val selectedTabIndexProvider: () -> Int,
        private val selectedOfferEligibilityProvider: () -> OfferEligibility,
    ) {

        private var windowSize by mutableStateOf(initialWindowSize)

        private val packageAwareDelegate = PackageAwareDelegate(
            style = style,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
        )

        private val presentedPartial by derivedStateOf {
            val windowCondition = ScreenCondition.from(windowSize)
            val componentState =
                if (packageAwareDelegate.isSelected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

            style.overrides.buildPresentedPartial(
                windowCondition,
                packageAwareDelegate.offerEligibility,
                componentState,
            )
        }

        @get:JvmSynthetic
        val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

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
