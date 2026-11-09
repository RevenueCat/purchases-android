package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/** Keeps a split column's content clear of the window edge without padding its shared edge. */
internal fun Modifier.consumeSplitInnerHorizontalInset(
    layout: StickyFooterComponent.Layout,
    isFooter: Boolean,
    windowInsets: WindowInsets,
): Modifier {
    val innerSide = when (layout) {
        StickyFooterComponent.Layout.SPLIT_LEADING -> if (isFooter) WindowInsetsSides.End else WindowInsetsSides.Start
        StickyFooterComponent.Layout.SPLIT_TRAILING -> if (isFooter) WindowInsetsSides.Start else WindowInsetsSides.End
        else -> return this
    }
    return consumeWindowInsets(windowInsets.only(innerSide))
}
