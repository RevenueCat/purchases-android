package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.currentWindowWidthSizeClass

@Composable
internal fun resolvedFooterLayout(state: PaywallState.Loaded.Components): StickyFooterComponent.Layout {
    val footerStyle = state.stickyFooter as? StickyFooterComponentStyle
    val windowSize = currentWindowWidthSizeClass()
    return footerStyle?.overrides?.buildPresentedPartial(
        windowSize = ScreenCondition.from(windowSize),
        offerEligibility = { state.selectedOfferEligibility },
        state = { ComponentViewState.DEFAULT },
        conditionContext = ConditionContext(
            selectedPackageId = { state.selectedPackageInfo?.rcPackage?.identifier },
            customVariables = state.mergedCustomVariables,
            stateReader = state.stateStore::currentValueOrDefault,
            windowDpSize = state.paywallBoundsDp,
        ),
    )?.layout ?: footerStyle?.layout ?: StickyFooterComponent.Layout.NORMAL
}

@Composable
internal fun footerBodyBottomInsets(state: PaywallState.Loaded.Components): WindowInsets =
    if (state.stickyFooter != null && resolvedFooterLayout(state) != StickyFooterComponent.Layout.NORMAL) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    } else {
        WindowInsets(0)
    }
