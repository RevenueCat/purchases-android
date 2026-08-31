@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ConditionContext
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageAwareDelegate
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedWebViewComponentState(
    style: WebViewComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): WebViewComponentState = rememberUpdatedWebViewComponentState(
    style = style,
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
private fun rememberUpdatedWebViewComponentState(
    style: WebViewComponentStyle,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
    customVariablesProvider: () -> Map<String, CustomVariableValue>,
    stateStoreProvider: () -> PaywallStateStore,
): WebViewComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        WebViewComponentState(
            initialWindowSize = windowSize,
            style = style,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
            customVariablesProvider = customVariablesProvider,
            stateStoreProvider = stateStoreProvider,
        )
    }.apply {
        update(windowSize = windowSize)
    }
}

@Suppress("LongParameterList")
@Stable
internal class WebViewComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: WebViewComponentStyle,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
    private val customVariablesProvider: () -> Map<String, CustomVariableValue> = { emptyMap() },
    private val stateStoreProvider: () -> PaywallStateStore = { PaywallStateStore(emptyMap()) },
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
            conditionContext = ConditionContext(
                selectedPackageId = selectedPackageInfoProvider()?.rcPackage?.identifier,
                customVariables = customVariablesProvider(),
                stateReader = stateStoreProvider()::currentValueOrDefault,
            ),
        )
    }

    // url/componentId/size always come from the base component; only visible is overridable.
    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @JvmSynthetic
    fun update(windowSize: WindowWidthSizeClass? = null) {
        if (windowSize != null) this.windowSize = windowSize
    }
}
