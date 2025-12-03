@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.text

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
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.IntroOfferAvailability
import com.revenuecat.purchases.ui.revenuecatui.components.IntroOfferSnapshot
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
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

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
        screenConditionProvider = { paywallState.screenCondition },
        introOfferAvailability = IntroOfferAvailability(
            hasAnyIntroOfferEligiblePackage = paywallState.hasAnyIntroOfferEligiblePackage,
            hasAnyMultipleIntroOffersEligiblePackage = paywallState.hasAnyMultipleIntroOffersEligiblePackage,
        ),
        appVersionIntProvider = { paywallState.appVersionInt },
    )
}

@Stable
@JvmSynthetic
@Composable
@Suppress("LongParameterList")
internal fun rememberUpdatedTextComponentState(
    style: TextComponentStyle,
    localeProvider: () -> Locale,
    selectedPackageProvider: () -> Package?,
    selectedTabIndexProvider: () -> Int,
    screenConditionProvider: () -> ScreenCondition,
    introOfferAvailability: IntroOfferAvailability = IntroOfferAvailability(),
    appVersionIntProvider: () -> Int? = { null },
): TextComponentState {
    val screenCondition = screenConditionProvider()

    // Create countdown state once if this text is inside a countdown component
    val countdownState = style.countdownDate?.let { date ->
        rememberCountdownState(date)
    }

    return remember(style) {
        TextComponentState(
            initialScreenCondition = screenCondition,
            style = style,
            localeProvider = localeProvider,
            selectedPackageProvider = selectedPackageProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            introOfferAvailability = introOfferAvailability,
            appVersionIntProvider = appVersionIntProvider,
        )
    }.apply {
        update(
            countdownTime = countdownState?.countdownTime,
            screenCondition = screenCondition,
        )
    }
}

@Stable
internal class TextComponentState(
    initialScreenCondition: ScreenCondition,
    private val style: TextComponentStyle,
    private val localeProvider: () -> Locale,
    private val selectedPackageProvider: () -> Package?,
    private val selectedTabIndexProvider: () -> Int,
    private val introOfferAvailability: IntroOfferAvailability,
    private val appVersionIntProvider: () -> Int?,
) {
    private var screenConditionSnapshot by mutableStateOf(initialScreenCondition)

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

    /**
     * How countdown variables should be displayed (component hours vs total hours).
     */
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom
        get() = style.countFrom

    private val presentedPartial by derivedStateOf {
        val componentState = if (selected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT
        val introOfferEligibility = applicablePackage?.introEligibility ?: IntroOfferEligibility.INELIGIBLE
        val introOfferSnapshot = IntroOfferSnapshot(
            eligibility = introOfferEligibility,
            availability = introOfferAvailability,
        )

        style.overrides.buildPresentedPartial(
            screenCondition = screenConditionSnapshot,
            introOfferSnapshot = introOfferSnapshot,
            state = componentState,
            selectedPackageIdentifier = applicablePackage?.identifier,
            appVersion = appVersionIntProvider(),
        )
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
        countdownTime: CountdownTime? = this.countdownTime,
        screenCondition: ScreenCondition? = null,
    ) {
        if (screenCondition != null) this.screenConditionSnapshot = screenCondition
        this.countdownTime = countdownTime
    }
}
