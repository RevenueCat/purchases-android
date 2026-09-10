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
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ConditionContext
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageAwareDelegate
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

    private val packageAwareDelegate = PackageAwareDelegate(
        style = style.stackComponentStyle,
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
            conditionContext = ConditionContext(
                selectedPackageId = selectedPackageInfoProvider()?.rcPackage?.identifier,
                customVariables = customVariablesProvider(),
                stateReader = stateStoreProvider()::currentValueOrDefault,
                windowDpSize = windowDpSize,
            ),
        )
    }

    /**
     * Deliberately falls back to `true` rather than to the stack's visibility: the caller already
     * gates on the stack's own resolved state, which accounts for overrides on that stack. Reading
     * [ButtonComponentStyle.visible] here instead would reintroduce the stack's pre-override value
     * and hide a button whose stack an override had revealed.
     */
    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.buttonVisible ?: true }

    @JvmSynthetic
    fun update(windowSize: WindowWidthSizeClass, windowDpSize: DpSize?) {
        this.windowSize = windowSize
        if (windowDpSize != null) this.windowDpSize = windowDpSize
    }
}
