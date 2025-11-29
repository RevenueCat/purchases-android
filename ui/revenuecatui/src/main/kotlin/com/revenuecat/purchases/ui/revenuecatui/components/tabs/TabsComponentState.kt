@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.IntroOfferAvailability
import com.revenuecat.purchases.ui.revenuecatui.components.IntroOfferSnapshot
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenConditionSnapshot
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTabsComponentState(
    style: TabsComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): TabsComponentState =
    rememberUpdatedTabsComponentState(
        style = style,
        selectedPackageProvider = { paywallState.selectedPackageInfo?.rcPackage },
        screenConditionProvider = { paywallState.screenConditionSnapshot },
        introOfferAvailability = IntroOfferAvailability(
            hasAnyIntroOfferEligiblePackage = paywallState.hasAnyIntroOfferEligiblePackage,
            hasAnyMultipleIntroOffersEligiblePackage = paywallState.hasAnyMultipleIntroOffersEligiblePackage,
        ),
    )

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedTabsComponentState(
    style: TabsComponentStyle,
    selectedPackageProvider: () -> Package?,
    screenConditionProvider: () -> ScreenConditionSnapshot,
    introOfferAvailability: IntroOfferAvailability = IntroOfferAvailability(),
): TabsComponentState {
    val screenCondition = screenConditionProvider()

    return remember(style) {
        TabsComponentState(
            initialScreenCondition = screenCondition,
            style = style,
            selectedPackageProvider = selectedPackageProvider,
            introOfferAvailability = introOfferAvailability,
        )
    }.apply {
        update(
            screenCondition = screenCondition,
        )
    }
}

@Stable
internal class TabsComponentState(
    initialScreenCondition: ScreenConditionSnapshot,
    private val style: TabsComponentStyle,
    private val selectedPackageProvider: () -> Package?,
    private val introOfferAvailability: IntroOfferAvailability,
) {
    private var screenConditionSnapshot by mutableStateOf(initialScreenCondition)

    /**
     * The package to consider for intro offer eligibility.
     */
    private val applicablePackage by derivedStateOf { selectedPackageProvider() }
    private val presentedPartial by derivedStateOf {
        val componentState = ComponentViewState.DEFAULT // A TabsComponent is never selected.
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
        )
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @get:JvmSynthetic
    val tabs = style.tabs

    @get:JvmSynthetic
    val size by derivedStateOf { presentedPartial?.partial?.size ?: style.size }

    @get:JvmSynthetic
    val background by derivedStateOf { presentedPartial?.backgroundStyles ?: style.background }

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
        screenCondition: ScreenConditionSnapshot? = null,
    ) {
        if (screenCondition != null) this.screenConditionSnapshot = screenCondition
    }
}
