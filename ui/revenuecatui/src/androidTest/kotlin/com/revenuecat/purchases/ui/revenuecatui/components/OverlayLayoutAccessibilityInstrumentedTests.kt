package com.revenuecat.purchases.ui.revenuecatui.components

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.StackComponent
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
internal class OverlayLayoutAccessibilityInstrumentedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun platformAccessibilityTraversalMovesFromHeaderToBody(): Unit = with(composeTestRule) {
        val state = accessibilityTestState()
        setContent {
            PaywallComponentsScaffold(
                state = state,
                modifier = Modifier.fillMaxSize(),
                background = null,
                headerContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    ) {
                        AccessibilityNode(
                            contentDescription = "Header",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.TopEnd),
                        )
                    }
                },
                footerContent = {
                    AccessibilityNode(
                        contentDescription = "Footer",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                    )
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AccessibilityNode(
                        contentDescription = "Body",
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        traversalOrder = -0.5f,
                    )
                }
            }
        }

        val headerNode = onNodeWithContentDescription("Header", useUnmergedTree = true)
            .fetchSemanticsNode()
        val root: RootForTest = requireNotNull(headerNode.root)
        val bodyNode = onNodeWithContentDescription("Body", useUnmergedTree = true)
            .fetchSemanticsNode()
        val bodyId = bodyNode.id
        val headerTraversalBeforeId = runOnIdle {
            root.forceAccessibilityForTesting(true)
            val provider = activity.window.decorView.findAccessibilityNodeProvider()
            provider.traversalBeforeId(headerNode.id)
        }

        assertThat(headerTraversalBeforeId).isEqualTo(bodyId)
    }

    @Composable
    private fun AccessibilityNode(
        contentDescription: String,
        modifier: Modifier = Modifier,
        traversalOrder: Float? = null,
    ) {
        Box(
            modifier = Modifier
                .semantics {
                    this.contentDescription = contentDescription
                    traversalOrder?.let { traversalIndex = it }
                }
                .then(modifier),
        )
    }

    private fun accessibilityTestState(): PaywallState.Loaded.Components {
        val locale = LocaleId("en_US")
        val componentsData = PaywallComponentsData(
            id = "accessibility_test_paywall",
            templateName = "accessibility_test_template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = emptyList()),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()))),
                ),
            ),
            componentsLocalizations = nonEmptyMapOf(
                locale to nonEmptyMapOf(
                    LocalizationKey("accessibility_test_key") to LocalizationData.Text("accessibility_test"),
                ) as LocalizationDictionary,
            ),
            defaultLocaleIdentifier = locale,
        )
        val offering = Offering(
            identifier = "accessibility_test_offering",
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(
                UiConfig(
                    app = UiConfig.AppConfig(colors = emptyMap(), fonts = emptyMap()),
                    localizations = mapOf(locale to mapOf(VariableLocalizationKey.DAY to "day")),
                    variableConfig = UiConfig.VariableConfig(
                        variableCompatibilityMap = emptyMap(),
                        functionCompatibilityMap = emptyMap(),
                    ),
                ),
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

    private fun AccessibilityNodeProvider.nodeInfoFor(semanticsId: Int): AccessibilityNodeInfo {
        return requireNotNull(createAccessibilityNodeInfo(semanticsId)) {
            "Expected an accessibility node for semantics ID $semanticsId"
        }
    }

    private fun AccessibilityNodeProvider.traversalBeforeId(semanticsId: Int): Int {
        val info = nodeInfoFor(semanticsId)
        return info.extras.getInt(EXTRA_DATA_TEST_TRAVERSAL_BEFORE, NO_TRAVERSAL_NODE)
    }

    private fun View.findAccessibilityNodeProvider(): AccessibilityNodeProvider =
        accessibilityNodeProvider
            ?: (this as? ViewGroup)
                ?.findChildAccessibilityNodeProvider()
            ?: error("Expected the Compose view to provide accessibility nodes")

    private fun View.findAccessibilityNodeProviderOrNull(): AccessibilityNodeProvider? =
        accessibilityNodeProvider
            ?: (this as? ViewGroup)
                ?.findChildAccessibilityNodeProvider()

    private fun ViewGroup.findChildAccessibilityNodeProvider(): AccessibilityNodeProvider? =
        (0 until childCount).firstNotNullOfOrNull { index ->
            getChildAt(index).findAccessibilityNodeProviderOrNull()
        }

    private companion object {
        // Compose's platform accessibility delegate exposes its traversal map through this test-only extra.
        const val EXTRA_DATA_TEST_TRAVERSAL_BEFORE =
            "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"
        const val NO_TRAVERSAL_NODE = -1
    }
}
