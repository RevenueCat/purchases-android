@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.text

import android.content.res.AssetManager
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.getBestMatch
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.resolve
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TextComponentState =
    rememberUpdatedTextComponentState(
        style = style,
        localeProvider = { paywallState.locale },
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        selectedTabIndexProvider = { paywallState.selectedTabIndex },
    )

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    localeProvider: () -> Locale,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
): TextComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val context = LocalContext.current

    return remember(style) {
        TextComponentState(
            initialWindowSize = windowSize,
            style = style,
            localeProvider = localeProvider,
            selectedPackageProvider = selectedPackageProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            assets = context.resources.assets,
        )
    }.apply {
        update(
            windowSize = windowSize,
        )
    }
}

@Stable
internal class TextComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: TextComponentStyle,
    private val localeProvider: () -> Locale,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
    private val assets: AssetManager,
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
    private val localeId by derivedStateOf { localeProvider().toLocaleId() }

    /**
     * The package to take variable values from and to consider for intro offer eligibility.
     */
    val applicablePackage by derivedStateOf {
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
    val text by derivedStateOf {
        presentedPartial?.texts?.run { getOrDefault(localeId, entry.value) }
            ?: style.texts.run { getOrDefault(localeId, entry.value) }
    }

    @get:JvmSynthetic
    val localizedVariableKeys by derivedStateOf {
        // We use getBestMatch here, because the localeId in `texts` might be different from the one in
        // `variableLocalizations`. For instance, `texts` might have `de_DE` while `variableLocalizations` has `de`.
        style.variableLocalizations.run { getBestMatch(localeId) ?: entry.value }
    }

    @get:JvmSynthetic
    val color by derivedStateOf { presentedPartial?.color ?: style.color }

    @get:JvmSynthetic
    val fontSize by derivedStateOf { presentedPartial?.partial?.fontSize ?: style.fontSize }

    @get:JvmSynthetic
    val fontWeight by derivedStateOf {
        presentedPartial?.partial?.let { partial ->
            partial.fontWeightInt?.let { FontWeight(it) } ?: partial.fontWeight?.toFontWeight()
        } ?: style.fontWeight
    }

    private val fontSpec by derivedStateOf {
        presentedPartial?.fontSpec ?: style.fontSpec
    }

    @get:JvmSynthetic
    val fontFamily by derivedStateOf {
        fontSpec?.resolve(assets = assets, weight = fontWeight ?: FontWeight.Normal, style = FontStyle.Normal)
    }

    @get:JvmSynthetic
    val textAlign by derivedStateOf {
        presentedPartial?.partial?.horizontalAlignment?.toTextAlign() ?: style.textAlign
    }

    @get:JvmSynthetic
    val horizontalAlignment by derivedStateOf {
        presentedPartial?.partial?.horizontalAlignment?.toAlignment() ?: style.horizontalAlignment
    }

    @get:JvmSynthetic
    val backgroundColor by derivedStateOf { presentedPartial?.backgroundColor ?: style.backgroundColor }

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val padding by derivedStateOf { presentedPartial?.partial?.padding?.toPaddingValues() ?: style.padding }

    @get:JvmSynthetic
    val margin by derivedStateOf { presentedPartial?.partial?.margin?.toPaddingValues() ?: style.margin }

    @JvmSynthetic
    fun update(
        windowSize: WindowWidthSizeClass? = null,
    ) {
        if (windowSize != null) this.windowSize = windowSize
    }
}
