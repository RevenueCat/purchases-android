package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InternalPaywallLifecycleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Compose paywall resets to Loading only after leaving composition`(): Unit = with(composeTestRule) {
        // Loaded paywalls can contain animations that are unrelated to this lifecycle assertion.
        mainClock.autoAdvance = false
        var showPaywall by mutableStateOf(true)
        val options = PaywallOptions.Builder(dismissRequest = { showPaywall = false })
            .setOffering(TestData.template1Offering)
            .build()
        val viewModel = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = MockPurchasesType(storefrontCountryCode = "US"),
            options = options,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
        )

        setContent {
            if (showPaywall) {
                InternalPaywall(options, viewModel)
            }
        }
        waitUntil { viewModel.state.value is PaywallState.Loaded }

        runOnUiThread {
            viewModel.closePaywall()
            assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)
        }

        mainClock.advanceTimeBy(100)
        runOnIdle {
            assertThat(viewModel.state.value).isEqualTo(PaywallState.Loading)
        }
    }
}
