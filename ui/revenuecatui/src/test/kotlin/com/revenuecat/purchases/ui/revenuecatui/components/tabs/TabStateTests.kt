package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
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
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.serialization.json.JsonPrimitive
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
class TabStateTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val localeId = LocaleId("en_US")
    private val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
    private val assetBaseURL = URL("https://assets.pawwalls.com")
    private val stateKey = "selectedTab"

    @Test
    fun `Component re-resolves its state_condition override when the store value changes`(): Unit =
        with(composeTestRule) {
            val defaultTextKey = LocalizationKey("default_text")
            val annualTextKey = LocalizationKey("annual_text")
            val localizations = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    defaultTextKey to LocalizationData.Text("Default copy"),
                    annualTextKey to LocalizationData.Text("Annual copy"),
                ),
            )
            val component = TextComponent(
                text = defaultTextKey,
                color = textColor,
                overrides = listOf(
                    ComponentOverride(
                        conditions = listOf(
                            ComponentOverride.Condition.State(
                                operator = ComponentOverride.EqualityOperator.EQUALS,
                                name = stateKey,
                                value = JsonPrimitive("annual"),
                            ),
                        ),
                        properties = PartialTextComponent(text = annualTextKey),
                    ),
                ),
            )
            val state = FakePaywallState(
                localizations = localizations,
                defaultLocaleIdentifier = localeId,
                components = listOf(component),
                packages = listOf(TestData.Packages.monthly),
            )
            // Seed the declaration the override reads (default selection is "monthly").
            state.stateStore.registerDeclarations(
                mapOf(stateKey to StateDeclaration(type = StateDeclaration.ValueType.STRING, defaultValue = JsonPrimitive("monthly"))),
            )
            val styleFactory = StyleFactory(localizations = localizations)
            val style = styleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

            setContent { TextComponentView(style = style, state = state) }

            // Default selection: override does not apply.
            onNodeWithText("Default copy").assertExists()
            onNodeWithText("Annual copy").assertDoesNotExist()

            // Simulate another component publishing the "annual" tab selection.
            state.stateStore.applyUpdates(
                listOf(StateUpdate.Set(stateKey, StateUpdateValue.Literal(JsonPrimitive("annual")))),
            )
            waitForIdle()

            // The override now applies and the component recomposed.
            onNodeWithText("Annual copy").assertExists()
            onNodeWithText("Default copy").assertDoesNotExist()
        }

    @Test
    fun `Selecting a tab publishes its id into the state store`(): Unit = with(composeTestRule) {
        val tab0LabelKey = LocalizationKey("tab0_label")
        val tab1LabelKey = LocalizationKey("tab1_label")
        val localizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                tab0LabelKey to LocalizationData.Text("Monthly"),
                tab1LabelKey to LocalizationData.Text("Annual"),
            ),
        )
        val tabControlButtons = listOf(tab0LabelKey to "monthly", tab1LabelKey to "annual")
            .mapIndexed { index, (labelKey, _) ->
                TabControlButtonComponent(
                    tabIndex = index,
                    tabId = listOf("monthly", "annual")[index],
                    stack = StackComponent(components = listOf(TextComponent(text = labelKey, color = textColor))),
                )
            }
        val tabsComponent = TabsComponent(
            tabs = listOf("monthly", "annual").map { id ->
                TabsComponent.Tab(id = id, stack = StackComponent(components = listOf(TabControlComponent)))
            },
            control = TabsComponent.TabControl.Buttons(stack = StackComponent(components = tabControlButtons)),
            stateUpdates = listOf(StateUpdate.Set(stateKey, StateUpdateValue.PayloadReference)),
        )
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = assetBaseURL,
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(tabsComponent)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeId,
            stateDeclarations = mapOf(
                stateKey to StateDeclaration(type = StateDeclaration.ValueType.STRING, defaultValue = JsonPrimitive("monthly")),
            ),
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val styleFactory = StyleFactory(localizations = localizations, offering = offering)
        val style = styleFactory.create(tabsComponent).getOrThrow().componentStyle as TabsComponentStyle

        setContent { TabsComponentView(style = style, state = state, clickHandler = { }) }
        waitForIdle()

        // Seeded on first appearance with the default tab id.
        assertThat(state.stateStore.currentValueOrDefault(stateKey)).isEqualTo(JsonPrimitive("monthly"))

        onNodeWithText("Annual").assertHasClickAction().performClick()
        waitForIdle()

        // Selection republished the new tab id.
        assertThat(state.stateStore.currentValueOrDefault(stateKey)).isEqualTo(JsonPrimitive("annual"))
    }

    @Test
    fun `A hidden tabs component does not publish its selected tab id`(): Unit = with(composeTestRule) {
        val tab0LabelKey = LocalizationKey("tab0_label")
        val tab1LabelKey = LocalizationKey("tab1_label")
        val localizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                tab0LabelKey to LocalizationData.Text("Monthly"),
                tab1LabelKey to LocalizationData.Text("Annual"),
            ),
        )
        val tabControlButtons = listOf(tab0LabelKey, tab1LabelKey).mapIndexed { index, labelKey ->
            TabControlButtonComponent(
                tabIndex = index,
                tabId = listOf("monthly", "annual")[index],
                stack = StackComponent(components = listOf(TextComponent(text = labelKey, color = textColor))),
            )
        }
        val tabsComponent = TabsComponent(
            visible = false,
            tabs = listOf("monthly", "annual").map { id ->
                TabsComponent.Tab(id = id, stack = StackComponent(components = listOf(TabControlComponent)))
            },
            control = TabsComponent.TabControl.Buttons(stack = StackComponent(components = tabControlButtons)),
            defaultTabId = "annual",
            stateUpdates = listOf(StateUpdate.Set(stateKey, StateUpdateValue.PayloadReference)),
        )
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = assetBaseURL,
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(tabsComponent)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeId,
            stateDeclarations = mapOf(
                stateKey to StateDeclaration(type = StateDeclaration.ValueType.STRING, defaultValue = JsonPrimitive("monthly")),
            ),
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val styleFactory = StyleFactory(localizations = localizations, offering = offering)
        val style = styleFactory.create(tabsComponent).getOrThrow().componentStyle as TabsComponentStyle

        setContent { TabsComponentView(style = style, state = state, clickHandler = { }) }
        waitForIdle()

        assertThat(state.stateStore.currentValueOrDefault(stateKey)).isEqualTo(JsonPrimitive("monthly"))
    }
}
