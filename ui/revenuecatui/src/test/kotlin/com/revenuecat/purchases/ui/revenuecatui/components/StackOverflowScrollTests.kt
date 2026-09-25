package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.Date

/**
 * Unit-tier twin of `StackOverflowScrollInstrumentedTests` for the partial-override path, so the
 * behavior gates per-PR unit runs and not only the emulator job.
 */
@RunWith(AndroidJUnit4::class)
internal class StackOverflowScrollTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val verticallyScrollable =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    @Test
    fun `base scroll overflow keeps the stack scrollable when a partial leaves overflow absent`() {
        setPaywallContent(
            rootStack(
                overflow = StackComponent.Overflow.SCROLL,
                overrides = listOf(matchingWindowOverride(PartialStackComponent())),
            ),
        )

        assertThat(verticallyScrollableNodeCount()).isGreaterThan(0)
    }

    @Test
    fun `window size rule partial with NONE disables the base scroll`() {
        setPaywallContent(
            rootStack(
                overflow = StackComponent.Overflow.SCROLL,
                overrides = listOf(
                    matchingWindowOverride(PartialStackComponent(overflow = StackComponent.Overflow.NONE)),
                ),
            ),
        )

        assertThat(verticallyScrollableNodeCount()).isZero()
    }

    private fun matchingWindowOverride(
        properties: PartialStackComponent,
    ): ComponentOverride<PartialStackComponent> = ComponentOverride(
        conditions = listOf(
            ComponentOverride.Condition.WindowWidthRule(
                operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                value = 100.0,
            ),
        ),
        properties = properties,
    )

    private fun verticallyScrollableNodeCount(): Int {
        composeTestRule.waitForIdle()
        return composeTestRule.onAllNodes(verticallyScrollable).fetchSemanticsNodes().size
    }

    private fun setPaywallContent(rootStack: StackComponent) {
        composeTestRule.setContent {
            LoadedPaywallComponents(
                state = paywallState(rootStack),
                clickHandler = { },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun rootStack(
        overflow: StackComponent.Overflow?,
        overrides: List<ComponentOverride<PartialStackComponent>> = emptyList(),
    ): StackComponent = StackComponent(
        components = listOf(
            // Taller than the test window, so there is always something to scroll to.
            StackComponent(
                components = emptyList(),
                size = Size(width = SizeConstraint.Fill(), height = SizeConstraint.Fixed(4000u)),
            ),
        ),
        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
        size = Size(width = SizeConstraint.Fill(), height = SizeConstraint.Fill()),
        overflow = overflow,
        overrides = overrides,
    )

    private fun paywallState(rootStack: StackComponent): PaywallState.Loaded.Components {
        val locale = LocaleId("en_US")
        val componentsData = PaywallComponentsData(
            id = "overflow_partial_test_paywall",
            templateName = "overflow_partial_test_template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = rootStack,
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()))),
                ),
            ),
            componentsLocalizations = nonEmptyMapOf(
                locale to nonEmptyMapOf(
                    LocalizationKey("overflow_partial_test_key") to LocalizationData.Text("overflow_partial_test"),
                ) as LocalizationDictionary,
            ),
            defaultLocaleIdentifier = locale,
        )
        val offering = Offering(
            identifier = "overflow_partial_test_offering",
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
        val validation = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()
            ?: error("Expected Components paywall validation to succeed")
        return offering.toComponentsPaywallState(
            validationResult = validation,
            storefrontCountryCode = null,
            dateProvider = { Date() },
            purchases = MockPurchasesType(),
        )
    }
}
