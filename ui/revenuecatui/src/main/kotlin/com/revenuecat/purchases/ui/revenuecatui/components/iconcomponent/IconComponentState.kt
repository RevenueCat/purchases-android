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
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@JvmSynthetic
@Composable
internal fun rememberUpdatedIconComponentState(
    style: IconComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): IconComponentState =
    rememberUpdatedIconComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        selectedTabIndexProvider = { paywallState.selectedTabIndex },
    )

@JvmSynthetic
@Composable
private fun rememberUpdatedIconComponentState(
    style: IconComponentStyle,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
): IconComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        IconComponentState(
            initialWindowSize = windowSize,
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
internal class IconComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: IconComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
) {
    private var windowSize by mutableStateOf(initialWindowSize)
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
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT
        val introOfferEligibility = applicablePackage?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

        style.overrides?.buildPresentedPartial(windowCondition, introOfferEligibility, componentState)
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
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: true }

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
