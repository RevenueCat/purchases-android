package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import io.mockk.every
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class InternalPaywallSheetDismissalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(
            UiConfig(),
            PaywallComponentsData(
                id = "paywall_id",
                templateName = "template",
                assetBaseURL = URL("https://assets.pawwalls.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
                        background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                        stickyFooter = null,
                    ),
                ),
                componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
                defaultLocaleIdentifier = LocaleId("en_US"),
            ),
        ),
    )

    @Test
    fun `an open sheet is hidden when a purchase completes`(): Unit = with(composeTestRule) {
        // Loaded paywalls can contain animations that are unrelated to this assertion.
        mainClock.autoAdvance = false
        val options = PaywallOptions.Builder(dismissRequest = {}).setOffering(offering).build()
        val purchaseCompleted = mutableStateOf(false)
        val viewModel = spyk(
            PaywallViewModelImpl(
                resourceProvider = MockResourceProvider(),
                purchases = MockPurchasesType(storefrontCountryCode = "US"),
                options = options,
                colorScheme = TestData.Constants.currentColorScheme,
                isDarkMode = false,
                shouldDisplayBlock = null,
            ),
        )
        every { viewModel.purchaseCompleted } returns purchaseCompleted

        setContent { InternalPaywall(options, viewModel) }
        waitUntil { viewModel.state.value is PaywallState.Loaded.Components }
        val sheet = (viewModel.state.value as PaywallState.Loaded.Components).sheet

        runOnUiThread { sheet.show(backgroundBlur = false, content = { Text("sheet") }) }
        runOnIdle { assertThat(sheet.visible).isTrue() }

        runOnUiThread { purchaseCompleted.value = true }
        mainClock.advanceTimeBy(100)
        runOnIdle { assertThat(sheet.visible).isFalse() }
    }
}
