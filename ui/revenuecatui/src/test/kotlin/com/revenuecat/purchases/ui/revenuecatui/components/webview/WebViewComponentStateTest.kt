package com.revenuecat.purchases.ui.revenuecatui.components.webview

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.PartialWebViewComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedWebViewPartial
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Only `visible` is overridable on web_view (url/size always come from the base style); these tests
 * exercise the wiring from [WebViewComponentStyle.overrides] through [WebViewComponentState.visible].
 */
@RunWith(AndroidJUnit4::class)
class WebViewComponentStateTest {

    private val size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit())

    private fun styleWithOverrides(
        visible: Boolean,
        overrides: List<PresentedOverride<PresentedWebViewPartial>>,
        rcPackage: Package? = null,
    ) = WebViewComponentStyle(
        url = "https://paywalls.revenuecat.com/index.html",
        visible = visible,
        size = size,
        componentId = "promo_web_view",
        overrides = overrides,
        rcPackage = rcPackage,
        tabIndex = null,
    )

    private fun state(
        style: WebViewComponentStyle,
        windowSize: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
        selectedPackageInfo: PaywallState.Loaded.Components.SelectedPackageInfo? = null,
    ) = WebViewComponentState(
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
    fun `hides a base-visible web_view under a matching expanded override`() {
        val overrides = listOf(
            PresentedOverride(
                conditions = listOf(ComponentOverride.Condition.Expanded),
                properties = PresentedWebViewPartial(PartialWebViewComponent(visible = false)),
            ),
        )
        val style = styleWithOverrides(visible = true, overrides = overrides)

        assertThat(state(style, windowSize = WindowWidthSizeClass.COMPACT).visible).isTrue()
        assertThat(state(style, windowSize = WindowWidthSizeClass.EXPANDED).visible).isFalse()
    }

    @Test
    fun `shows a base-hidden web_view under a matching selected override`() {
        val rcPackage = TestData.Packages.monthly
        val overrides = listOf(
            PresentedOverride(
                conditions = listOf(ComponentOverride.Condition.Selected),
                properties = PresentedWebViewPartial(PartialWebViewComponent(visible = true)),
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
