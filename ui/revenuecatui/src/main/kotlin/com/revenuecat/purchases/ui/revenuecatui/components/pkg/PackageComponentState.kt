@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.pkg

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
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedPackageComponentState(
    style: PackageComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): PackageComponentState = rememberUpdatedPackageComponentState(
    style = style,
    selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
    selectedTabIndexProvider = { paywallState.selectedTabIndex },
    selectedOfferEligibilityProvider = { paywallState.selectedOfferEligibility },
    customVariablesProvider = { paywallState.mergedCustomVariables },
)

@Suppress("LongParameterList")
@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedPackageComponentState(
    style: PackageComponentStyle,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
    customVariablesProvider: () -> Map<String, CustomVariableValue>,
): PackageComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        PackageComponentState(
            initialWindowSize = windowSize,
            style = style,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
            customVariablesProvider = customVariablesProvider,
        )
    }.apply {
        update(windowSize = windowSize)
    }
}

@Suppress("LongParameterList")
@Stable
internal class PackageComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: PackageComponentStyle,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
    private val customVariablesProvider: () -> Map<String, CustomVariableValue> = { emptyMap() },
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
            ),
        )
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @JvmSynthetic
    fun update(windowSize: WindowWidthSizeClass) {
        this.windowSize = windowSize
    }
}
