package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.MeasurePaywallBounds
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class CountdownPackageSelectionTests {
    private val monthly = TestData.Packages.monthly
    private val annual = TestData.Packages.annual
    private val weekly = TestData.Packages.weekly
    private val color = ColorScheme(light = ColorInfo.Hex(0xFF000000.toInt()))
    private var nowMillis = 10_000L
    private val expiry = Date(12_000L)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `expiry selects a newly visible default when nothing was selected`() {
        val state = state(countdown(running = text("wait")))
        show(state)
        assertThat(state.selectedPackageInfo).isNull()

        expire()

        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `expiry replaces a selection that disappeared with the countdown stack`() {
        val state = state(countdown(running = pkg(annual, default = true)))
        show(state)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        expire()

        composeTestRule.onNodeWithText("Annual plan").assertDoesNotExist()
        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `expiry preserves a user's selection that remains visible`() {
        val state = state(countdown(running = pkg(annual, default = true)), pkg(weekly))
        show(state)
        composeTestRule.onNodeWithText("Weekly plan").performClick()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)

        expire()

        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `expiry without an end stack preserves the running content and selection`() {
        val state = state(CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "date", date = expiry),
            countdownStack = StackComponent(listOf(pkg(annual, default = true))),
        ))
        show(state)

        expire()

        composeTestRule.onNodeWithText("Annual plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `the final fractional second does not reveal or select the end stack early`() {
        val state = state(countdown(running = pkg(annual, default = true)))
        show(state)

        advanceTo(expiry.time - 500L)

        composeTestRule.onNodeWithText("Annual plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly plan").assertDoesNotExist()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        expire()

        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    private fun countdown(running: PaywallComponent) = CountdownComponent(
        style = CountdownComponent.CountdownStyle(type = "date", date = expiry),
        countdownStack = StackComponent(listOf(running)),
        endStack = StackComponent(listOf(pkg(monthly, default = true))),
    )

    private fun show(state: PaywallState.Loaded.Components) {
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(300.dp, 400.dp)) {
                ComponentView(state.stack, state, onClick = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun expire() = advanceTo(expiry.time)

    private fun advanceTo(time: Long) {
        composeTestRule.runOnIdle { nowMillis = time }
        composeTestRule.mainClock.advanceTimeBy(1_100)
        composeTestRule.waitForIdle()
    }

    private fun state(vararg components: PaywallComponent) = FakePaywallState(
        components = components.toList(),
        packages = listOf(monthly, annual, weekly),
        localizations = nonEmptyMapOf(LocaleId("en_US") to nonEmptyMapOf(
            LocalizationKey("wait") to LocalizationData.Text("Unlocks soon"),
            LocalizationKey(monthly.identifier) to LocalizationData.Text("Monthly plan"),
            LocalizationKey(annual.identifier) to LocalizationData.Text("Annual plan"),
            LocalizationKey(weekly.identifier) to LocalizationData.Text("Weekly plan"),
        )),
        addPackageComponents = false,
        dateProvider = { Date(nowMillis) },
    )

    private fun pkg(pkg: Package, default: Boolean = false) = PackageComponent(
        packageId = pkg.identifier,
        isSelectedByDefault = default,
        stack = StackComponent(listOf(text(pkg.identifier))),
    )

    private fun text(key: String) = TextComponent(LocalizationKey(key), color)
}
