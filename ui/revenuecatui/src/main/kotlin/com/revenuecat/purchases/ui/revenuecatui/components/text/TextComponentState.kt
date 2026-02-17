@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.CountdownTime
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.rememberCountdownState
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.getBestMatch
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.resolve
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageAwareDelegate
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TextComponentState = rememberUpdatedTextComponentState(
    style = style,
    localeProvider = { paywallState.locale },
    selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
    selectedTabIndexProvider = { paywallState.selectedTabIndex },
    selectedOfferEligibilityProvider = { paywallState.selectedOfferEligibility },
)

@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    localeProvider: () -> Locale,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
): TextComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val countdownState = style.countdownDate?.let { date ->
        rememberCountdownState(date)
    }

    return remember(style) {
        TextComponentState(
            initialWindowSize = windowSize,
            style = style,
            localeProvider = localeProvider,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
            countdownTime = countdownState?.countdownTime,
        )
    }
}

@Stable
internal class TextComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: TextComponentStyle,
    private val localeProvider: () -> Locale,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
) {
    private var windowSize by mutableStateOf(initialWindowSize)

    private val packageAwareDelegate = PackageAwareDelegate(
        style = style,
        selectedPackageInfoProvider = selectedPackageInfoProvider,
        selectedTabIndexProvider = selectedTabIndexProvider,
        selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
    )

    /**
     * The current countdown time, if this text is inside a countdown component.
     *
     * Updated every second via [update] when countdown is active. Triggers recomposition
     * to update countdown variables (e.g., {{ count_hours_without_zero }}) in the text.
     * Null if this text is not inside a countdown component.
     */
    var countdownTime by mutableStateOf<CountdownTime?>(null)
        private set

    private val localeId by derivedStateOf { localeProvider().toLocaleId() }

    /**
     * The package to take variable values from and to consider for intro offer eligibility.
     */
    val applicablePackage by derivedStateOf {
        style.rcPackage ?: selectedPackageInfoProvider()?.rcPackage
    }

    val subscriptionOption: SubscriptionOption? by derivedStateOf {
        style.resolvedOffer?.subscriptionOption ?: selectedPackageInfoProvider()?.resolvedOffer?.subscriptionOption
    }

    /**
     * How countdown variables should be displayed (component hours vs total hours).
     */
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom
        get() = style.countFrom

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState =
            if (packageAwareDelegate.isSelected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides.buildPresentedPartial(windowCondition, packageAwareDelegate.offerEligibility, componentState)
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
        fontSpec?.resolve(weight = fontWeight ?: FontWeight.Normal, style = FontStyle.Normal)
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
        countdownTime: CountdownTime? = this.countdownTime,
    ) {
        if (windowSize != null) this.windowSize = windowSize
        this.countdownTime = countdownTime
    }
}
