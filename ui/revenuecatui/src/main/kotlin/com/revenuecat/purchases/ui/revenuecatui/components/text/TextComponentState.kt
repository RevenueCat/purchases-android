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
import com.revenuecat.purchases.Package
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
import com.revenuecat.purchases.ui.revenuecatui.extensions.offerEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TextComponentState {
    return rememberUpdatedTextComponentState(
        style = style,
        localeProvider = { paywallState.locale },
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        selectedTabIndexProvider = { paywallState.selectedTabIndex },
        selectedSubscriptionOptionProvider = { paywallState.selectedPackageInfo?.resolvedOffer?.subscriptionOption },
        selectedPackageUniqueIdProvider = { paywallState.selectedPackageInfo?.uniqueId },
        selectedOfferEligibilityProvider = {
            paywallState.selectedPackageInfo?.resolvedOffer?.offerEligibility ?: OfferEligibility.Ineligible
        },
    )
}

@Suppress("LongParameterList")
@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    localeProvider: () -> Locale,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
    selectedSubscriptionOptionProvider: () -> SubscriptionOption? = { null },
    selectedPackageUniqueIdProvider: () -> String? = { null },
    selectedOfferEligibilityProvider: () -> OfferEligibility = { OfferEligibility.Ineligible },
): TextComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    // Create countdown state once if this text is inside a countdown component
    val countdownState = style.countdownDate?.let { date ->
        rememberCountdownState(date)
    }

    return remember(style) {
        TextComponentState(
            initialWindowSize = windowSize,
            style = style,
            localeProvider = localeProvider,
            selectedPackageProvider = selectedPackageProvider,
            selectedSubscriptionOptionProvider = selectedSubscriptionOptionProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedPackageUniqueIdProvider = selectedPackageUniqueIdProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
        )
    }.apply {
        update(
            windowSize = windowSize,
            countdownTime = countdownState?.countdownTime,
        )
    }
}

@Suppress("LongParameterList")
@Stable
internal class TextComponentState(
    initialWindowSize: WindowWidthSizeClass,
    private val style: TextComponentStyle,
    private val localeProvider: () -> Locale,
    private val selectedPackageProvider: () -> Package?,
    private val selectedSubscriptionOptionProvider: () -> SubscriptionOption?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedPackageUniqueIdProvider: () -> String?,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
) {
    private var windowSize by mutableStateOf(initialWindowSize)

    /**
     * The current countdown time, if this text is inside a countdown component.
     *
     * Updated every second via [update] when countdown is active. Triggers recomposition
     * to update countdown variables (e.g., {{ count_hours_without_zero }}) in the text.
     * Null if this text is not inside a countdown component.
     */
    var countdownTime by mutableStateOf<CountdownTime?>(null)
        private set

    private val selected by derivedStateOf {
        if (style.packageUniqueId != null) {
            style.packageUniqueId == selectedPackageUniqueIdProvider()
        } else if (style.rcPackage != null) {
            style.rcPackage.identifier == selectedPackageProvider()?.identifier
        } else if (style.tabIndex != null) {
            style.tabIndex == selectedTabIndexProvider()
        } else {
            false
        }
    }
    private val localeId by derivedStateOf { localeProvider().toLocaleId() }

    /**
     * The package to take variable values from and to consider for offer eligibility.
     */
    val applicablePackage by derivedStateOf {
        style.rcPackage ?: selectedPackageProvider()
    }

    /**
     * The subscription option to use for offer variables (product.offer_*, product.secondary_offer_*).
     * If a specific Play Store offer is configured for this text's package, use that.
     * Otherwise, use the selected package's resolved subscription option.
     */
    val subscriptionOption by derivedStateOf {
        style.resolvedOffer?.subscriptionOption ?: selectedSubscriptionOptionProvider()
    }

    /**
     * How countdown variables should be displayed (component hours vs total hours).
     */
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom
        get() = style.countFrom

    /**
     * The offer eligibility for this component, encoding both offer type (intro/promo) and phase count.
     * If the style has its own package, calculates from the style's resolved offer.
     * Otherwise, uses the selected package's resolved offer eligibility.
     */
    private val offerEligibility by derivedStateOf {
        if (style.rcPackage != null) {
            style.resolvedOffer?.offerEligibility ?: style.rcPackage.offerEligibility
        } else {
            selectedOfferEligibilityProvider()
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
