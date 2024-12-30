package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class PaywallActionTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Should pass the PaywallAction to the ViewModel`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocale = LocaleId("en_US")
        val localizationKeyRestore = LocalizationKey("restore")
        val localizationKeyBack = LocalizationKey("back")
        val localizationKeyPurchase = LocalizationKey("purchase")
        val localizationKeyNavigate = LocalizationKey("navigate")
        val localizationDataRestore = LocalizationData.Text("restore")
        val localizationDataBack = LocalizationData.Text("back")
        val localizationDataPurchase = LocalizationData.Text("purchase")
        val localizationDataNavigate = LocalizationData.Text("navigate")
        val localizations = nonEmptyMapOf(
            defaultLocale to nonEmptyMapOf(
                localizationKeyRestore to localizationDataRestore,
                localizationKeyBack to localizationDataBack,
                localizationKeyPurchase to localizationDataPurchase,
                localizationKeyNavigate to localizationDataNavigate,
            )
        )
        // Bit of a convoluted way to create components, to ensure we use an an exhaustive when, forcing ourselves to
        // revisit this when we add any new PaywallActions.
        val components = listOf(
            PaywallAction.RestorePurchases to localizationKeyRestore,
            PaywallAction.NavigateBack to localizationKeyBack,
            PaywallAction.PurchasePackage to localizationKeyPurchase,
            PaywallAction.NavigateTo(ButtonComponent.Destination.CustomerCenter) to localizationKeyNavigate,
        ).map { (action, key) ->
            when (action) {
                is PaywallAction.RestorePurchases,
                is PaywallAction.NavigateBack,
                is PaywallAction.NavigateTo,
                    -> ButtonComponent(
                    action = action.toButtonAction(),
                    stack = StackComponent(components = listOf(TextComponent(text = key, color = textColor)))
                )

                is PaywallAction.PurchasePackage -> PurchaseButtonComponent(
                    stack = StackComponent(components = listOf(TextComponent(text = key, color = textColor)))
                )
            }
        }
        val offering = FakeOffering(components, localizations)
        val options = PaywallOptions.Builder(dismissRequest = { })
            .setOffering(offering)
            .build()
        val viewModel = MockViewModel(offering = offering)

        // Act
        setContent { InternalPaywall(options, viewModel) }
        // We expect 2 of each button, once in the main content and once in the sticky footer.
        clickButtonsWithText(localizationDataRestore, expectedCount = 2)
        clickButtonsWithText(localizationDataBack, expectedCount = 2)
        clickButtonsWithText(localizationDataPurchase, expectedCount = 2)
        clickButtonsWithText(localizationDataNavigate, expectedCount = 2)

        // Assert
        viewModel.clickActions.forEachIndexed { index, action ->
            when (index) {
                0 -> assert(action is PaywallAction.RestorePurchases)
                1 -> assert(action is PaywallAction.RestorePurchases)
                2 -> assert(action is PaywallAction.NavigateBack)
                3 -> assert(action is PaywallAction.NavigateBack)
                4 -> assert(action is PaywallAction.PurchasePackage)
                5 -> assert(action is PaywallAction.PurchasePackage)
                6 -> assert(action is PaywallAction.NavigateTo)
                7 -> assert(action is PaywallAction.NavigateTo)
                else -> error("Unexpected PaywallAction at index $index: $action")
            }
        }
    }

    private fun SemanticsNodeInteractionsProvider.clickButtonsWithText(
        text: LocalizationData.Text,
        expectedCount: Int,
    ) {
        onAllNodesWithText(text.value)
            .assertCountEquals(expectedCount)
            .run {
                for (i in 0 until expectedCount) {
                    get(i)
                        .assertIsDisplayed()
                        .assertHasClickAction()
                        .performClick()
                }
            }
    }

    private fun PaywallAction.toButtonAction(): ButtonComponent.Action =
        when (this) {
            is PaywallAction.NavigateBack -> ButtonComponent.Action.NavigateBack
            is PaywallAction.NavigateTo -> ButtonComponent.Action.NavigateTo(destination)
            is PaywallAction.RestorePurchases -> ButtonComponent.Action.NavigateBack
            is PaywallAction.PurchasePackage -> error(
                "PurchasePackage is not a ButtonComponent.Action. It is handled by PurchaseButtonComponent instead."
            )
        }

    @Suppress("TestFunctionName")
    private fun FakePaywallData(
        components: List<PaywallComponent>,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>>,
        defaultLocale: LocaleId,
    ): PaywallComponentsData = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(components = components),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = StickyFooterComponent(stack = StackComponent(components = components)),
            ),
        ),
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocale,
    )

    @Suppress("TestFunctionName")
    private fun FakeOffering(data: PaywallComponentsData): Offering =
        Offering(
            identifier = "identifier",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = data,
        )

    @Suppress("TestFunctionName")
    private fun FakeOffering(
        components: List<PaywallComponent>,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>>,
    ): Offering = FakeOffering(
        data = FakePaywallData(
            components = components,
            localizations = localizations,
            defaultLocale = localizations.keys.first()
        )
    )
}
