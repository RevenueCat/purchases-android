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
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TextComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val countdownState = style.countdownDate?.let { date ->
        rememberCountdownState(date)
    }

    return remember(style) {
        TextComponentState(
            initialWindowSize = windowSize,
            style = style,
            localeProvider = { paywallState.locale },
            selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
            selectedTabIndexProvider = { paywallState.selectedTabIndex },
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
) {
    private var windowSize by mutableStateOf(initialWindowSize)

    var countdownTime by mutableStateOf<CountdownTime?>(null)
        private set

    private val selected by derivedStateOf {
        val selectedInfo = selectedPackageInfoProvider()
        when {
            style.packageUniqueId != null -> style.packageUniqueId == selectedInfo?.uniqueId
            style.rcPackage != null -> style.rcPackage.identifier == selectedInfo?.rcPackage?.identifier
            style.tabIndex != null -> style.tabIndex == selectedTabIndexProvider()
            else -> false
        }
    }

    private val localeId by derivedStateOf { localeProvider().toLocaleId() }

    val applicablePackage by derivedStateOf {
        style.rcPackage ?: selectedPackageInfoProvider()?.rcPackage
    }

    val subscriptionOption: SubscriptionOption? by derivedStateOf {
        style.resolvedOffer?.subscriptionOption ?: selectedPackageInfoProvider()?.resolvedOffer?.subscriptionOption
    }

    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom
        get() = style.countFrom

    private val offerEligibility by derivedStateOf {
        if (style.rcPackage != null) {
            calculateOfferEligibility(style.resolvedOffer, style.rcPackage)
        } else {
            selectedPackageInfoProvider()?.let {
                calculateOfferEligibility(it.resolvedOffer, it.rcPackage)
            } ?: OfferEligibility.Ineligible
        }
    }

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides.buildPresentedPartial(windowCondition, offerEligibility, componentState)
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
