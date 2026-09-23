package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.Date

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
internal class PackageComponentAccessibilityInstrumentedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun platformAccessibilityReportsWhichPackageIsSelected(): Unit = with(composeTestRule) {
        val state = twoPackagePaywallState()
        setContent {
            LoadedPaywallComponents(state = state, clickHandler = { }, modifier = Modifier.fillMaxSize())
        }
        val yearly = onNode(hasClickAction() and hasText("Yearly")).fetchSemanticsNode()
        val monthly = onNode(hasClickAction() and hasText("Monthly")).fetchSemanticsNode()
        val root: RootForTest = requireNotNull(yearly.root)

        val yearlyInfo = root.nodeInfo(yearly.id)
        val monthlyInfo = root.nodeInfo(monthly.id)

        assertThat(yearlyInfo.isEnabled).isTrue()
        assertThat(yearlyInfo.isCheckable).isTrue()
        assertThat(yearlyInfo.isChecked).isTrue()
        assertThat(yearlyInfo.stateDescription?.toString()).isEqualTo("Selected")
        assertThat(monthlyInfo.isEnabled).isTrue()
        assertThat(monthlyInfo.isChecked).isFalse()
        assertThat(monthlyInfo.stateDescription?.toString()).isEqualTo("Not selected")
    }

    private fun RootForTest.nodeInfo(semanticsId: Int): AccessibilityNodeInfo = composeTestRule.runOnIdle {
        forceAccessibilityForTesting(true)
        val info = composeTestRule.activity.window.decorView.findAccessibilityNodeProvider()
            .createAccessibilityNodeInfo(semanticsId)
        forceAccessibilityForTesting(false)
        requireNotNull(info) { "Expected an accessibility node for semantics ID $semanticsId" }
    }

    private fun View.findAccessibilityNodeProvider(): AccessibilityNodeProvider =
        findAccessibilityNodeProviderOrNull() ?: error("Expected the Compose view to provide accessibility nodes")

    private fun View.findAccessibilityNodeProviderOrNull(): AccessibilityNodeProvider? =
        accessibilityNodeProvider
            ?: (this as? ViewGroup)?.let { group ->
                (0 until group.childCount).firstNotNullOfOrNull { index ->
                    group.getChildAt(index).findAccessibilityNodeProviderOrNull()
                }
            }

    private fun twoPackagePaywallState(): PaywallState.Loaded.Components {
        val locale = LocaleId("en_US")
        val offeringId = "offering"
        val yearlyPackage = testPackage("yearly", PackageType.ANNUAL, offeringId, Period.create("P1Y"))
        val monthlyPackage = testPackage("monthly", PackageType.MONTHLY, offeringId, Period.create("P1M"))
        val yearlyKey = LocalizationKey("yearly")
        val monthlyKey = LocalizationKey("monthly")
        val componentsData = PaywallComponentsData(
            id = "paywall",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            packageComponent(yearlyPackage, yearlyKey, isSelectedByDefault = true),
                            packageComponent(monthlyPackage, monthlyKey, isSelectedByDefault = false),
                        ),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()))),
                ),
            ),
            componentsLocalizations = nonEmptyMapOf(
                locale to nonEmptyMapOf(
                    yearlyKey to LocalizationData.Text("Yearly"),
                    monthlyKey to LocalizationData.Text("Monthly"),
                ) as LocalizationDictionary,
            ),
            defaultLocaleIdentifier = locale,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(yearlyPackage, monthlyPackage),
            paywallComponents = Offering.PaywallComponents(
                uiConfig(locale),
                componentsData,
            ),
        )
        val validation = offering
            .validatePaywallComponentsDataOrNull(PaywallResourceProvider(composeTestRule.activity))
            ?.getOrThrow()
            ?: error("Expected Components paywall validation to succeed")
        return offering.toComponentsPaywallState(
            validationResult = validation,
            storefrontCountryCode = null,
            dateProvider = { Date() },
            purchases = MockPurchasesType(),
        )
    }

    private fun packageComponent(rcPackage: Package, textKey: LocalizationKey, isSelectedByDefault: Boolean) =
        PackageComponent(
            packageId = rcPackage.identifier,
            isSelectedByDefault = isSelectedByDefault,
            stack = StackComponent(
                listOf(TextComponent(text = textKey, color = ColorScheme(ColorInfo.Hex(0xFF000000.toInt())))),
            ),
        )

    private fun uiConfig(locale: LocaleId) = UiConfig(
        app = UiConfig.AppConfig(colors = emptyMap(), fonts = emptyMap()),
        localizations = mapOf(locale to mapOf(VariableLocalizationKey.DAY to "day")),
        variableConfig = UiConfig.VariableConfig(
            variableCompatibilityMap = emptyMap(),
            functionCompatibilityMap = emptyMap(),
        ),
    )

    @Suppress("DEPRECATION")
    private fun testPackage(id: String, type: PackageType, offeringId: String, period: Period) = Package(
        identifier = id,
        packageType = type,
        offering = offeringId,
        product = TestStoreProduct(
            id = "product_$id",
            name = id,
            title = id,
            description = id,
            price = Price(formatted = "$1.00", amountMicros = 1_000_000, currencyCode = "USD"),
            period = period,
        ),
    )
}
