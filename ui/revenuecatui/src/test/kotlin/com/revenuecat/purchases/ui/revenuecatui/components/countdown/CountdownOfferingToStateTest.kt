package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PartialCountdownComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * End-to-end activation check: a components config with `countdown` validates and lands as
 * [CountdownComponentStyle] in the paywall state (serializer + StyleFactory wiring).
 */
@RunWith(AndroidJUnit4::class)
class CountdownOfferingToStateTest {

    @Test
    fun `components config with countdown produces CountdownComponentStyle in paywall state`() {
        val date = Date()
        val override = ComponentOverride(
            conditions = listOf(
                ComponentOverride.Condition.IntroOfferRule(
                    operator = ComponentOverride.EqualityOperator.EQUALS,
                    value = true,
                ),
            ),
            properties = PartialCountdownComponent(visible = true),
        )

        val state = FakePaywallState(
            components = listOf(
                CountdownComponent(
                    style = CountdownComponent.CountdownStyle(type = "fixed", date = date),
                    visible = false,
                    countFrom = CountdownComponent.CountFrom.HOURS,
                    countdownStack = StackComponent(components = emptyList()),
                    overrides = listOf(override),
                ),
            ),
            packages = listOf(TestData.Packages.monthly),
        )

        val rootStack = state.stack as StackComponentStyle
        val countdownStyle = rootStack.children.filterIsInstance<CountdownComponentStyle>().single()
        assertThat(countdownStyle.date).isEqualTo(date)
        assertThat(countdownStyle.countFrom).isEqualTo(CountdownComponent.CountFrom.HOURS)
        assertThat(countdownStyle.visible).isFalse()
        assertThat(countdownStyle.overrides).hasSize(1)
        assertThat(countdownStyle.overrides.single().conditions).isEqualTo(override.conditions)
        assertThat(countdownStyle.overrides.single().properties.partial.visible).isTrue()
    }
}
