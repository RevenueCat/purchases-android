package com.revenuecat.purchases.ui.revenuecatui.components.text

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
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
internal class TextComponentSpokenTextInstrumentedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun platformAccessibilityReadsPricesAsWords(): Unit = with(composeTestRule) {
        val state = paywallState(priceText = "{{ product.price_per_period_abbreviated }}")
        setContent {
            LoadedPaywallComponents(state = state, clickHandler = { }, modifier = Modifier.fillMaxSize())
        }
        val price = onNode(hasText("$1.00/mo", substring = false) or hasContentDescription("$1.00 monthly"))
            .fetchSemanticsNode()
        val root: RootForTest = requireNotNull(price.root)

        val info = root.nodeInfo(price.id)

        assertThat((info.contentDescription ?: info.text)?.toString()).isEqualTo("$1.00 monthly")
    }

    /**
     * Package rows merge their texts into one node, which is where most prices live. TalkBack reads a node's content
     * description instead of its text when it has one, so the row has to keep both the title and the spoken price.
     */
    @Test
    fun platformAccessibilityReadsPricesAsWordsInsideAPackageRow(): Unit = with(composeTestRule) {
        val state = paywallState(priceText = "{{ product.price_per_period_abbreviated }}", priceInsidePackage = true)
        setContent {
            LoadedPaywallComponents(state = state, clickHandler = { }, modifier = Modifier.fillMaxSize())
        }
        val row = onNode(hasClickAction(), useUnmergedTree = true).fetchSemanticsNode()
        val root: RootForTest = requireNotNull(row.root)

        val spoken = root.spokenText(row)

        assertThat(spoken).contains("Monthly").contains("$1.00 monthly").doesNotContain("/mo")
    }

    /**
     * Roughly what TalkBack reads for a focused node: its content description, or else its text followed by its
     * children's.
     */
    private fun RootForTest.spokenText(node: SemanticsNode): String = composeTestRule.runOnIdle {
        forceAccessibilityForTesting(true)
        val provider = composeTestRule.activity.window.decorView.findAccessibilityNodeProvider()
        fun SemanticsNode.spoken(): String {
            val info = provider.createAccessibilityNodeInfo(id)
            return info?.contentDescription?.toString()
                ?: (listOfNotNull(info?.text?.toString()) + children.map { it.spoken() })
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
        }
        val spoken = node.spoken()
        forceAccessibilityForTesting(false)
        spoken
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

    private fun paywallState(priceText: String, priceInsidePackage: Boolean = false): PaywallState.Loaded.Components {
        val locale = LocaleId("en_US")
        val offeringId = "offering"
        val monthlyPackage = testPackage("monthly", PackageType.MONTHLY, offeringId, Period.create("P1M"))
        val priceKey = LocalizationKey("price")
        val monthlyKey = LocalizationKey("monthly")
        val componentsData = PaywallComponentsData(
            id = "paywall",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = if (priceInsidePackage) {
                            listOf(packageComponent(monthlyPackage, listOf(monthlyKey, priceKey)))
                        } else {
                            listOf(text(priceKey), packageComponent(monthlyPackage, listOf(monthlyKey)))
                        },
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()))),
                ),
            ),
            componentsLocalizations = nonEmptyMapOf(
                locale to nonEmptyMapOf(
                    priceKey to LocalizationData.Text(priceText),
                    monthlyKey to LocalizationData.Text("Monthly"),
                ) as LocalizationDictionary,
            ),
            defaultLocaleIdentifier = locale,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(monthlyPackage),
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

    private fun packageComponent(rcPackage: Package, textKeys: List<LocalizationKey>) = PackageComponent(
        packageId = rcPackage.identifier,
        isSelectedByDefault = true,
        stack = StackComponent(textKeys.map { text(it) }),
    )

    private fun text(key: LocalizationKey) = TextComponent(
        text = key,
        color = ColorScheme(ColorInfo.Hex(0xFF000000.toInt())),
    )

    private fun uiConfig(locale: LocaleId) = UiConfig(
        app = UiConfig.AppConfig(colors = emptyMap(), fonts = emptyMap()),
        localizations = mapOf(
            locale to mapOf(
                VariableLocalizationKey.MONTH_SHORT to "mo",
                VariableLocalizationKey.MONTH to "month",
                VariableLocalizationKey.MONTHLY to "monthly",
            ),
        ),
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
