package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL
import java.util.Date

/** Layout guards for the bounded no-scroll path and the legacy outer-scroll path. */
@RunWith(Parameterized::class)
internal class StackOverflowSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {

    @Test
    fun absentOverflowPreservesLegacyLayout() {
        snapshot(rootStack(overflow = null))
    }

    @Test
    fun explicitNoneKeepsTheRootBounded() {
        snapshot(rootStack(overflow = StackComponent.Overflow.NONE))
    }

    @Test
    fun windowRuleEnablesRootScrollOnFirstFrame() {
        snapshot(
            rootStack(
                overflow = null,
                overrides = listOf(windowOverride(StackComponent.Overflow.SCROLL, minWidth = 100.0)),
            ),
        )
    }

    @Test
    fun windowRuleDisablesLegacyScrollOnTablet() {
        snapshot(
            rootStack(
                overflow = null,
                overrides = listOf(windowOverride(StackComponent.Overflow.NONE, minWidth = 700.0)),
            ),
        )
    }

    @Test
    fun noScrollRootKeepsItsScrollableChildBounded() {
        snapshot(
            rootStack(
                overflow = StackComponent.Overflow.NONE,
                children = listOf(
                    StackComponent(
                        components = listOf(band(0xFF3366CC.toInt(), height = 1200u)),
                        size = Size(width = SizeConstraint.Fill(), height = SizeConstraint.Fixed(180u)),
                        overflow = StackComponent.Overflow.SCROLL,
                    ),
                    band(0xFFCC6633.toInt(), height = 80u),
                ),
            ),
        )
    }

    private fun snapshot(rootStack: StackComponent) {
        val state = paywallState(rootStack)
        screenshotTest {
            LoadedPaywallComponents(state = state, clickHandler = { }, modifier = Modifier.fillMaxSize())
        }
    }

    private fun band(color: Int, height: UInt): StackComponent = StackComponent(
        components = emptyList(),
        size = Size(width = SizeConstraint.Fill(), height = SizeConstraint.Fixed(height)),
        backgroundColor = ColorScheme(light = ColorInfo.Hex(color)),
    )

    private fun rootStack(
        overflow: StackComponent.Overflow?,
        overrides: List<ComponentOverride<PartialStackComponent>> = emptyList(),
        children: List<StackComponent> = listOf(
            band(0xFF3366CC.toInt(), height = 180u),
            band(0xFFCC6633.toInt(), height = 80u),
        ),
    ): StackComponent = StackComponent(
        components = children,
        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.SPACE_BETWEEN),
        size = Size(width = SizeConstraint.Fill(), height = SizeConstraint.Fill()),
        overflow = overflow,
        overrides = overrides,
    )

    private fun windowOverride(
        overflow: StackComponent.Overflow,
        minWidth: Double,
    ): ComponentOverride<PartialStackComponent> = ComponentOverride(
        conditions = listOf(
            ComponentOverride.Condition.WindowWidthRule(
                operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                value = minWidth,
            ),
        ),
        properties = PartialStackComponent(overflow = overflow),
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

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = testConfigs
            .filter { it.name in setOf("pixel6", "nexus10") }
            .map { arrayOf<Any>(it) }
    }
}
