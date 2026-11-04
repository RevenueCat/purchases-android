@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentVisibilityContext
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedButtonVisibilityState(
    style: ButtonComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): ButtonVisibilityState = rememberUpdatedButtonVisibilityState(
    style = style,
    windowDpSize = paywallState.paywallBoundsDp,
    selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
    selectedTabIndexProvider = { paywallState.selectedTabIndex },
    selectedOfferEligibilityProvider = { paywallState.selectedOfferEligibility },
    customVariablesProvider = { paywallState.mergedCustomVariables },
    stateStoreProvider = { paywallState.stateStore },
)

@Suppress("LongParameterList")
@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedButtonVisibilityState(
    style: ButtonComponentStyle,
    windowDpSize: DpSize?,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
    customVariablesProvider: () -> Map<String, CustomVariableValue>,
    stateStoreProvider: () -> PaywallStateStore,
): ButtonVisibilityState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        ButtonVisibilityState(
            initialWindowSize = windowSize,
            initialWindowDpSize = windowDpSize,
            style = style,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
            customVariablesProvider = customVariablesProvider,
            stateStoreProvider = stateStoreProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
            windowDpSize = windowDpSize,
        )
    }
}

/**
 * Resolves a button's own visibility, which is separate from the visibility of the stack it wraps.
 * Selection and offer eligibility are read from that inner stack, since the button itself is not a
 * package context.
 */
@Suppress("LongParameterList")
@Stable
internal class ButtonVisibilityState(
    initialWindowSize: WindowWidthSizeClass,
    initialWindowDpSize: DpSize?,
    private val style: ButtonComponentStyle,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
    private val customVariablesProvider: () -> Map<String, CustomVariableValue> = { emptyMap() },
    private val stateStoreProvider: () -> PaywallStateStore = { PaywallStateStore(emptyMap()) },
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var windowDpSize by mutableStateOf(initialWindowDpSize)

    private val visibilityContext: ComponentVisibilityContext
        get() = ComponentVisibilityContext(
            screenCondition = ScreenCondition.from(windowSize),
            windowDpSize = windowDpSize,
            selectedPackage = selectedPackageInfoProvider,
            selectedTabIndex = selectedTabIndexProvider,
            customVariables = customVariablesProvider(),
            selectedOfferEligibility = selectedOfferEligibilityProvider,
            stateReader = { stateStoreProvider().currentValueOrDefault(it) },
        )

    @get:JvmSynthetic
    val visible by derivedStateOf { visibilityContext.isVisible(style) }

    @JvmSynthetic
    fun update(windowSize: WindowWidthSizeClass, windowDpSize: DpSize?) {
        this.windowSize = windowSize
        if (windowDpSize != null) this.windowDpSize = windowDpSize
    }
}
