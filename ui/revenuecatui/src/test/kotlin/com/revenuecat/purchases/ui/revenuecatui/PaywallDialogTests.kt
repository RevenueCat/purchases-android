package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class PaywallDialogTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultLocale = LocaleId("en_US")
    private val mockPurchases = mockk<Purchases>()

    @Before
    fun setUp() {
        mockkObject(Purchases)
        every { Purchases.sharedInstance } returns mockPurchases
        every { mockPurchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
        every { mockPurchases.storefrontCountryCode } returns "US"
        every { mockPurchases.preferredUILocaleOverride } returns null
        every { mockPurchases.preferredUILocaleOverrideHonorsLayoutDirection } returns false
        every { mockPurchases.addPreferredUILocaleOverrideChangeListener(any()) } returns {}
        every { mockPurchases.track(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `PaywallDialog dismisses after toggling sticky footer control on first presentation`() {
        var dismissCount = 0

        composeTestRule.setContent {
            PaywallDialog(
                PaywallDialogOptions.Builder()
                    .setOffering(fakeOffering())
                    .setDismissRequest { dismissCount++ }
                    .build(),
            )
        }

        with(composeTestRule) {
            onNode(isToggleable())
                .assertIsOn()
                .performClick()
                .assertIsOff()

            onNodeWithText("Close")
                .performClick()
        }

        composeTestRule.waitForIdle()

        assertThat(dismissCount).isEqualTo(1)
    }

    private fun fakeOffering(): Offering {
        val closeKey = LocalizationKey("close")

        val monthly = TestData.Packages.monthly
        val annual = TestData.Packages.annual

        val rootStack = StackComponent(
            components = listOf(
                ButtonComponent(
                    action = ButtonComponent.Action.NavigateBack,
                    stack = StackComponent(
                        components = listOf(
                            textComponent(closeKey),
                        ),
                    ),
                ),
            ),
        )

        val tabs = TabsComponent(
            tabs = listOf(
                tab(id = "monthly", pkg = monthly),
                tab(id = "annual", pkg = annual),
            ),
            control = TabsComponent.TabControl.Toggle(
                stack = StackComponent(
                    components = listOf(
                        TabControlToggleComponent(
                            defaultValue = true,
                            thumbColorOn = solidColorScheme(Color.White),
                            thumbColorOff = solidColorScheme(Color.White),
                            trackColorOn = solidColorScheme(Color(0xFF48BF21)),
                            trackColorOff = solidColorScheme(Color(0xFF9294AB)),
                        ),
                    ),
                ),
            ),
            defaultTabId = "annual",
        )

        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = rootStack,
                    background = Background.Color(solidColorScheme(Color.White)),
                    stickyFooter = StickyFooterComponent(stack = StackComponent(components = listOf(tabs))),
                ),
            ),
            componentsLocalizations = nonEmptyMapOf(
                defaultLocale to nonEmptyMapOf(
                    closeKey to LocalizationData.Text("Close"),
                    monthly.unselectedLocalizationKey to monthly.unselectedLocalizedText,
                    monthly.selectedLocalizationKey to monthly.selectedLocalizedText,
                    annual.unselectedLocalizationKey to annual.unselectedLocalizedText,
                    annual.selectedLocalizationKey to annual.selectedLocalizedText,
                ),
            ),
            defaultLocaleIdentifier = defaultLocale,
        )

        return Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(monthly, annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
    }

    private fun tab(id: String, pkg: Package): TabsComponent.Tab =
        TabsComponent.Tab(
            id = id,
            stack = StackComponent(
                components = listOf(
                    TabControlComponent,
                    packageComponent(pkg = pkg),
                ),
            ),
        )

    private fun packageComponent(pkg: Package): PackageComponent =
        PackageComponent(
            packageId = pkg.identifier,
            isSelectedByDefault = true,
            stack = StackComponent(
                components = listOf(
                    textComponent(
                        text = pkg.unselectedLocalizationKey,
                        selectedText = pkg.selectedLocalizationKey,
                    ),
                ),
            ),
        )

    private fun textComponent(
        text: LocalizationKey,
        selectedText: LocalizationKey? = null,
    ): TextComponent =
        TextComponent(
            text = text,
            color = solidColorScheme(Color.Black),
            overrides = listOfNotNull(
                selectedText?.let { selected ->
                    ComponentOverride(
                        conditions = listOf(ComponentOverride.Condition.Selected),
                        properties = PartialTextComponent(text = selected),
                    )
                },
            ),
        )

    private fun solidColorScheme(color: Color): ColorScheme =
        ColorScheme(ColorInfo.Hex(color.toArgb()))

    private val Package.selectedLocalizedText: LocalizationData.Text
        get() = LocalizationData.Text("${identifier}_selected")

    private val Package.selectedLocalizationKey: LocalizationKey
        get() = LocalizationKey("${identifier}_selected")

    private val Package.unselectedLocalizedText: LocalizationData.Text
        get() = LocalizationData.Text(identifier)

    private val Package.unselectedLocalizationKey: LocalizationKey
        get() = LocalizationKey(identifier)
}
