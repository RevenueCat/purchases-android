@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.toOrientation

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedStackComponentState(
    style: StackComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): StackComponentState =
    rememberUpdatedStackComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        selectedTabIndexProvider = { paywallState.selectedTabIndex },
    )

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedStackComponentState(
    style: StackComponentStyle,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
): StackComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val layoutDirection = LocalLayoutDirection.current

    return remember(style) {
        StackComponentState(
            initialWindowSize = windowSize,
            initialLayoutDirection = layoutDirection,
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
internal class StackComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialLayoutDirection: LayoutDirection,
    private val style: StackComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var layoutDirection by mutableStateOf(initialLayoutDirection)
    private val selected by derivedStateOf {
        if (style.rcPackage != null) {
            // Fallback to package identifier comparison for backwards compatibility
            style.rcPackage.identifier == selectedPackageProvider()?.identifier
        } else if (style.tabIndex != null) {
            style.tabIndex == selectedTabIndexProvider()
        } else {
            false
        }
    }

    /**
     * The package to consider for intro offer eligibility.
     */
    private val applicablePackage by derivedStateOf {
        style.rcPackage ?: selectedPackageProvider()
    }
    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT
        val introOfferEligibility = applicablePackage?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

        style.overrides.buildPresentedPartial(windowCondition, introOfferEligibility, componentState)
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @get:JvmSynthetic
    val children = style.children

    @get:JvmSynthetic
    val applyTopWindowInsets = style.applyTopWindowInsets

    @get:JvmSynthetic
    val applyBottomWindowInsets = style.applyBottomWindowInsets

    @get:JvmSynthetic
    val dimension by derivedStateOf { presentedPartial?.partial?.dimension ?: style.dimension }

    @get:JvmSynthetic
    val spacing by derivedStateOf { presentedPartial?.partial?.spacing?.dp ?: style.spacing }

    @get:JvmSynthetic
    val background by derivedStateOf { presentedPartial?.backgroundStyles ?: style.background }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val size by derivedStateOf {
        (presentedPartial?.partial?.size ?: style.size).adjustForMargin(margin, layoutDirection)
    }

    @get:JvmSynthetic
    val shape by derivedStateOf { presentedPartial?.partial?.shape ?: style.shape }

    @get:JvmSynthetic
    val border by derivedStateOf { presentedPartial?.borderStyles ?: style.border }

    @get:JvmSynthetic
    val shadow by derivedStateOf { presentedPartial?.shadowStyles ?: style.shadow }

    @get:JvmSynthetic
    val badge by derivedStateOf {
        presentedPartial?.badgeStyle ?: style.badge
    }

    @get:JvmSynthetic
    val scrollOrientation by derivedStateOf {
        presentedPartial?.partial?.overflow?.toOrientation(dimension) ?: style.scrollOrientation
    }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
        layoutDirection: LayoutDirection? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
        if (layoutDirection != null) this.layoutDirection = layoutDirection
    }

    /**
     * Since margin is not a thing in Compose, and we apply it as extra padding outside the border and background, we
     * need to make sure the Composable's size is large enough to contain the margin.
     */
    private fun Size.adjustForMargin(margin: PaddingValues, layoutDirection: LayoutDirection): Size {
        val adjustedWidth = when (val baseWidth = width) {
            is SizeConstraint.Fixed -> SizeConstraint.Fixed(
                baseWidth.value +
                    margin.calculateStartPadding(layoutDirection).value.toUInt() +
                    margin.calculateEndPadding(layoutDirection).value.toUInt(),
            )
            is SizeConstraint.Fill,
            is SizeConstraint.Fit,
            -> baseWidth
        }

        val adjustedHeight = when (val baseHeight = height) {
            is SizeConstraint.Fixed -> SizeConstraint.Fixed(
                baseHeight.value +
                    margin.calculateTopPadding().value.toUInt() +
                    margin.calculateBottomPadding().value.toUInt(),
            )
            is SizeConstraint.Fill,
            is SizeConstraint.Fit,
            -> baseHeight
        }

        return Size(width = adjustedWidth, height = adjustedHeight)
    }
}
