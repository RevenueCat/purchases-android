package com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.addMargin
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageAwareDelegate
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedIconComponentState(
    style: IconComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): IconComponentState = rememberUpdatedIconComponentState(
    style = style,
    selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
    selectedTabIndexProvider = { paywallState.selectedTabIndex },
    selectedOfferEligibilityProvider = { paywallState.selectedOfferEligibility },
)

@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedIconComponentState(
    style: IconComponentStyle,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
): IconComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val layoutDirection = LocalLayoutDirection.current

    return remember(style) {
        IconComponentState(
            initialWindowSize = windowSize,
            initialLayoutDirection = layoutDirection,
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
internal class IconComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialLayoutDirection: LayoutDirection,
    private val style: IconComponentStyle,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var layoutDirection by mutableStateOf(initialLayoutDirection)

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
    private val baseUrl: String by derivedStateOf {
        presentedPartial?.partial?.baseUrl ?: style.baseUrl
    }
    private val iconName: String by derivedStateOf {
        presentedPartial?.partial?.iconName ?: style.iconName
    }
    private val formats: IconComponent.Formats by derivedStateOf {
        presentedPartial?.partial?.formats ?: style.formats
    }
    private val iconBackground: IconComponentStyle.Background? by derivedStateOf {
        presentedPartial?.background ?: style.iconBackground
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @get:JvmSynthetic
    val url: String by derivedStateOf {
        "$baseUrl/${formats.webp}"
    }

    @get:JvmSynthetic
    val size: Size by derivedStateOf {
        presentedPartial?.partial?.size ?: style.size
    }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @get:JvmSynthetic
    val sizePlusMargin: Size by derivedStateOf {
        size.addMargin(margin, layoutDirection)
    }

    @get:JvmSynthetic
    val shape: Shape? by derivedStateOf { iconBackground?.shape?.toShape() }

    @get:JvmSynthetic
    val border by derivedStateOf { iconBackground?.border }

    @get:JvmSynthetic
    val shadow by derivedStateOf { iconBackground?.shadow }

    @get:JvmSynthetic
    val backgroundColorStyles by derivedStateOf { iconBackground?.color }

    val tintColor by derivedStateOf { presentedPartial?.colorStyles ?: style.color }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
    }
}
