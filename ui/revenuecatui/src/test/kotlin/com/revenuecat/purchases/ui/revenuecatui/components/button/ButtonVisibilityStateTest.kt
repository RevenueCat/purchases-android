package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.paywalls.components.PartialButtonComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedButtonPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [ButtonVisibilityState] directly, which is the cheapest way to cover condition types
 * that need a paywall state store. Rendering-level coverage lives in `VisibilityConditionTests`.
 */
@RunWith(AndroidJUnit4::class)
internal class ButtonVisibilityStateTest {

    private val stackStyle = previewStackComponentStyle(children = emptyList())

    private fun style(
        buttonVisible: Boolean? = null,
        overrides: List<PresentedOverride<PresentedButtonPartial>> = emptyList(),
    ) = ButtonComponentStyle(
        stackComponentStyle = stackStyle,
        action = ButtonComponentStyle.Action.NavigateBack,
        buttonVisible = buttonVisible,
        overrides = overrides,
    )

    private fun state(
        style: ButtonComponentStyle,
        stateStore: PaywallStateStore = PaywallStateStore(emptyMap()),
        selectedPackageInfo: PaywallState.Loaded.Components.SelectedPackageInfo? = null,
    ) = ButtonVisibilityState(
        initialWindowSize = WindowWidthSizeClass.COMPACT,
        initialWindowDpSize = DpSize(400.dp, 800.dp),
        style = style,
        selectedPackageInfoProvider = { selectedPackageInfo },
        selectedTabIndexProvider = { 0 },
        selectedOfferEligibilityProvider = { OfferEligibility.Ineligible },
        customVariablesProvider = { emptyMap() },
        stateStoreProvider = { stateStore },
    )

    private fun stateOverride(key: String, value: Boolean, visible: Boolean) = PresentedOverride(
        conditions = listOf(
            ComponentOverride.Condition.State(
                operator = ComponentOverride.EqualityOperator.EQUALS,
                name = key,
                value = JsonPrimitive(value),
            ),
        ),
        properties = PresentedButtonPartial(PartialButtonComponent(visible = visible)),
    )

    private fun storeWith(key: String, default: Boolean) = PaywallStateStore(
        mapOf(
            key to StateDeclaration(
                type = StateDeclaration.ValueType.BOOLEAN,
                defaultValue = JsonPrimitive(default),
            ),
        ),
    )

    @Test
    fun `visible by default when nothing is configured`() {
        assertThat(state(style()).visible).isTrue()
    }

    @Test
    fun `honors the button's own visible when no override applies`() {
        assertThat(state(style(buttonVisible = false)).visible).isFalse()
    }

    /**
     * A button is the component that writes paywall state via `state_updates`, so "hide this button
     * once the sheet it opened is open" is an expected rule. It has to actually be evaluated rather
     * than failing closed, which is the same silent-drop failure this port exists to fix.
     */
    @Test
    fun `hides the button when a state rule matches`() {
        val style = style(overrides = listOf(stateOverride("planComparisonOpen", value = true, visible = false)))

        val visible = state(style, stateStore = storeWith("planComparisonOpen", default = true)).visible

        assertThat(visible).isFalse()
    }

    @Test
    fun `leaves the button alone when the state rule does not match`() {
        val style = style(overrides = listOf(stateOverride("planComparisonOpen", value = true, visible = false)))

        val visible = state(style, stateStore = storeWith("planComparisonOpen", default = false)).visible

        assertThat(visible).isTrue()
    }

    @Test
    fun `leaves the button alone when the state key was never declared`() {
        val style = style(overrides = listOf(stateOverride("neverDeclared", value = true, visible = false)))

        assertThat(state(style).visible).isTrue()
    }
}
