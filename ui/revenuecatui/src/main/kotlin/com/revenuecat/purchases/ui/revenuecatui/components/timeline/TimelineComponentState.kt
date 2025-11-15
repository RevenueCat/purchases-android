package com.revenuecat.purchases.ui.revenuecatui.components.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.LocalScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenConditionSnapshot
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTimelineComponentState(
    style: TimelineComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TimelineComponentState =
    rememberUpdatedTimelineComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        selectedTabIndexProvider = { paywallState.selectedTabIndex },
    )

@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedTimelineComponentState(
    style: TimelineComponentStyle,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
): TimelineComponentState {
    val screenCondition = LocalScreenCondition.current

    return remember(style) {
        TimelineComponentState(
            initialScreenCondition = screenCondition,
            style = style,
            selectedPackageProvider = selectedPackageProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
        )
    }.apply {
        update(
            screenCondition = screenCondition,
        )
    }
}

@Stable
internal class TimelineComponentState(
    initialScreenCondition: ScreenConditionSnapshot,
    private val style: TimelineComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
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
            state = componentState,
            selectedPackageIdentifier = applicablePackage?.identifier,
        )
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
        style.items.map {
            ItemState(
                initialScreenCondition = screenConditionSnapshot,
                style = it,
                selectedPackageProvider = selectedPackageProvider,
                selectedTabIndexProvider = selectedTabIndexProvider,
            )
        }
    }

    @JvmSynthetic
    fun update(
        screenCondition: ScreenConditionSnapshot? = null,
    ) {
        if (screenCondition != null) this.screenConditionSnapshot = screenCondition
    }

    @Stable
    class ItemState(
        initialScreenCondition: ScreenConditionSnapshot,
        private val style: TimelineComponentStyle.ItemStyle,
        private val selectedPackageProvider: () -> Package?,
        private val selectedTabIndexProvider: () -> Int,
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
                state = componentState,
                selectedPackageIdentifier = applicablePackage?.identifier,
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
            screenCondition: ScreenConditionSnapshot? = null,
        ) {
            if (screenCondition != null) this.screenConditionSnapshot = screenCondition
        }
    }
}
