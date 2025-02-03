@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

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
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@JvmSynthetic
@Composable
internal fun rememberUpdatedTabsComponentState(
    style: TabsComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TabsComponentState =
    rememberUpdatedTabsComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
    )

@JvmSynthetic
@Composable
internal fun rememberUpdatedTabsComponentState(
    style: TabsComponentStyle,
    selectedPackageProvider: () -> Package?,
): TabsComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        TabsComponentState(
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
internal class TabsComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: TabsComponentStyle,
    private val selectedPackageProvider: () -> Package?,
) {
    private var windowSize by mutableStateOf(initialWindowSize)

    /**
     * The package to consider for intro offer eligibility.
     */
    private val applicablePackage by derivedStateOf { selectedPackageProvider() }
    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = ComponentViewState.DEFAULT // A TabsComponent is never selected.
        val introOfferEligibility = applicablePackage?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

        style.overrides?.buildPresentedPartial(windowCondition, introOfferEligibility, componentState)
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: true }

    @get:JvmSynthetic
    val tabs = style.tabs

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val backgroundColor by derivedStateOf { presentedPartial?.backgroundColorStyles ?: style.backgroundColor }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val shape by derivedStateOf { (presentedPartial?.partial?.shape ?: style.shape).toShape() }

    @get:JvmSynthetic
    val border by derivedStateOf { presentedPartial?.borderStyles ?: style.border }

    @get:JvmSynthetic
    val shadow by derivedStateOf { presentedPartial?.shadowStyles ?: style.shadow }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
    }
}
