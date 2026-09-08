package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PartialCountdownComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCountdownPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Only `visible` is overridable on countdown (its child stacks handle their own overrides); these tests
 * exercise the wiring from [CountdownComponentStyle.overrides] through [CountdownComponentState.visible].
 */
@RunWith(AndroidJUnit4::class)
class CountdownComponentOverridesStateTest {

    private fun styleWithOverrides(
        visible: Boolean,
        overrides: List<PresentedOverride<PresentedCountdownPartial>>,
        rcPackage: Package? = null,
    ) = CountdownComponentStyle(
        date = Date(),
        countFrom = CountdownComponent.CountFrom.DAYS,
        countdownStackComponentStyle = previewStackComponentStyle(children = emptyList()),
        endStackComponentStyle = null,
        fallbackStackComponentStyle = null,
        visible = visible,
        overrides = overrides,
        rcPackage = rcPackage,
        tabIndex = null,
    )

    private fun state(
        style: CountdownComponentStyle,
        windowSize: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
        selectedPackageInfo: PaywallState.Loaded.Components.SelectedPackageInfo? = null,
    ) = CountdownComponentState(
        initialWindowSize = windowSize,
        style = style,
        selectedPackageInfoProvider = { selectedPackageInfo },
        selectedTabIndexProvider = { 0 },
        selectedOfferEligibilityProvider = { OfferEligibility.Ineligible },
        customVariablesProvider = { emptyMap() },
        stateStoreProvider = { PaywallStateStore(emptyMap()) },
    )

    @Test
    fun `falls back to the base component's visible when no override applies`() {
        val style = styleWithOverrides(visible = true, overrides = emptyList())

        assertThat(state(style).visible).isTrue()
        assertThat(state(styleWithOverrides(visible = false, overrides = emptyList())).visible).isFalse()
    }

    @Test
    fun `hides a base-visible countdown under a matching expanded override`() {
        val overrides = listOf(
            PresentedOverride(
                conditions = listOf(ComponentOverride.Condition.Expanded),
                properties = PresentedCountdownPartial(PartialCountdownComponent(visible = false)),
            ),
        )
        val style = styleWithOverrides(visible = true, overrides = overrides)

        assertThat(state(style, windowSize = WindowWidthSizeClass.COMPACT).visible).isTrue()
        assertThat(state(style, windowSize = WindowWidthSizeClass.EXPANDED).visible).isFalse()
    }

    @Test
    fun `shows a base-hidden countdown under a matching selected override`() {
        val rcPackage = TestData.Packages.monthly
        val overrides = listOf(
            PresentedOverride(
                conditions = listOf(ComponentOverride.Condition.Selected),
                properties = PresentedCountdownPartial(PartialCountdownComponent(visible = true)),
            ),
        )
        val style = styleWithOverrides(visible = false, overrides = overrides, rcPackage = rcPackage)
        val selectedPackageInfo = PaywallState.Loaded.Components.SelectedPackageInfo(
            rcPackage = rcPackage,
            uniqueId = rcPackage.identifier,
            offerEligibility = OfferEligibility.Ineligible,
        )

        // Not selected: keeps the base (hidden) value.
        assertThat(state(style, selectedPackageInfo = null).visible).isFalse()
        // Selected: the override applies.
        assertThat(state(style, selectedPackageInfo = selectedPackageInfo).visible).isTrue()
    }
}
