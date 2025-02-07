package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.ComponentStates
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class TabsComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Should properly update selected state of tab control button children`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")

        val unselectedKeyTab0 = LocalizationKey("unselected_tab0")
        val selectedKeyTab0 = LocalizationKey("selected_tab0")
        val unselectedKeyTab1 = LocalizationKey("unselected_tab1")
        val selectedKeyTab1 = LocalizationKey("selected_tab1")
        val unselectedKeyTab2 = LocalizationKey("unselected_tab2")
        val selectedKeyTab2 = LocalizationKey("selected_tab2")
        val unselectedTextTab0 = LocalizationData.Text("tab 0 unselected")
        val selectedTextTab0 = LocalizationData.Text("tab 0 selected")
        val unselectedTextTab1 = LocalizationData.Text("tab 1 unselected")
        val selectedTextTab1 = LocalizationData.Text("tab 1 selected")
        val unselectedTextTab2 = LocalizationData.Text("tab 2 unselected")
        val selectedTextTab2 = LocalizationData.Text("tab 2 selected")

        val tabControlButtons = listOf(
            unselectedKeyTab0 to selectedKeyTab0,
            unselectedKeyTab1 to selectedKeyTab1,
            unselectedKeyTab2 to selectedKeyTab2,
        ).mapIndexed() { index, (unselectedKey, selectedKey) ->
            TabControlButtonComponent(
                tabIndex = index,
                stack = StackComponent(
                    components = listOf(
                        TextComponent(
                            text = unselectedKey,
                            color = textColor,
                            overrides = ComponentOverrides(
                                states = ComponentStates(selected = PartialTextComponent(text = selectedKey))
                            )
                        )
                    ),
                ),
            )
        }

        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                unselectedKeyTab0 to unselectedTextTab0,
                selectedKeyTab0 to selectedTextTab0,
                unselectedKeyTab1 to unselectedTextTab1,
                selectedKeyTab1 to selectedTextTab1,
                unselectedKeyTab2 to unselectedTextTab2,
                selectedKeyTab2 to selectedTextTab2,
            )
        )

        val tabsComponent = TabsComponent(
            // Mapping tabControlButtons to make sure we have as many tabs as tab control buttons.
            tabs = tabControlButtons.map {
                TabsComponent.Tab(StackComponent(components = listOf(TabControlComponent)))
            },
            control = TabsComponent.TabControl.Buttons(stack = StackComponent(components = tabControlButtons))
        )

        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(tabsComponent)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val tabsComponentStyle = styleFactory.create(tabsComponent).getOrThrow() as TabsComponentStyle

        // Act
        setContent { TabsComponentView(style = tabsComponentStyle, state = state, clickHandler = { }) }

        // Assert
        // Tab 0 is selected
        onNodeWithText(selectedTextTab0.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab1.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab2.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab0.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextTab1.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextTab2.value)
            .assertIsNotDisplayed()

        // Select tab 1
        onNodeWithText(unselectedTextTab1.value)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(unselectedTextTab0.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextTab1.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab2.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextTab0.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextTab1.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextTab2.value)
            .assertIsNotDisplayed()

        // Select tab 2
        onNodeWithText(unselectedTextTab2.value)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(unselectedTextTab0.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab1.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextTab2.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextTab0.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextTab1.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextTab2.value)
            .assertIsNotDisplayed()
    }

    @Test
    fun `Should not update selected state of tab children`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")

        val tab0ControlKey = LocalizationKey("tab0_control")
        val tab1ControlKey = LocalizationKey("tab1_control")
        val tab2ControlKey = LocalizationKey("tab2_control")
        val tab0ControlText = LocalizationData.Text("tab 0 control")
        val tab1ControlText = LocalizationData.Text("tab 1 control")
        val tab2ControlText = LocalizationData.Text("tab 2 control")

        val unselectedKeyTab0 = LocalizationKey("unselected_tab0")
        val selectedKeyTab0 = LocalizationKey("selected_tab0")
        val unselectedKeyTab1 = LocalizationKey("unselected_tab1")
        val selectedKeyTab1 = LocalizationKey("selected_tab1")
        val unselectedKeyTab2 = LocalizationKey("unselected_tab2")
        val selectedKeyTab2 = LocalizationKey("selected_tab2")
        val unselectedTextTab0 = LocalizationData.Text("tab 0 unselected")
        val selectedTextTab0 = LocalizationData.Text("tab 0 selected")
        val unselectedTextTab1 = LocalizationData.Text("tab 1 unselected")
        val selectedTextTab1 = LocalizationData.Text("tab 1 selected")
        val unselectedTextTab2 = LocalizationData.Text("tab 2 unselected")
        val selectedTextTab2 = LocalizationData.Text("tab 2 selected")

        val tabs = listOf(
            unselectedKeyTab0 to selectedKeyTab0,
            unselectedKeyTab1 to selectedKeyTab1,
            unselectedKeyTab2 to selectedKeyTab2,
        ).map { (unselectedKey, selectedKey) ->
            TabsComponent.Tab(
                StackComponent(
                    components = listOf(
                        TabControlComponent,
                        TextComponent(
                            text = unselectedKey,
                            color = textColor,
                            overrides = ComponentOverrides(
                                states = ComponentStates(selected = PartialTextComponent(text = selectedKey))
                            )
                        )
                    )
                )
            )
        }

        val tabControl = listOf(
            tab0ControlKey,
            tab1ControlKey,
            tab2ControlKey,
        ).mapIndexed { index, tabControlKey ->
            TabControlButtonComponent(
                tabIndex = index,
                stack = StackComponent(
                    components = listOf(
                        TextComponent(
                            text = tabControlKey,
                            color = textColor,
                        )
                    ),
                ),
            )
        }

        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                unselectedKeyTab0 to unselectedTextTab0,
                selectedKeyTab0 to selectedTextTab0,
                unselectedKeyTab1 to unselectedTextTab1,
                selectedKeyTab1 to selectedTextTab1,
                unselectedKeyTab2 to unselectedTextTab2,
                selectedKeyTab2 to selectedTextTab2,
                tab0ControlKey to tab0ControlText,
                tab1ControlKey to tab1ControlText,
                tab2ControlKey to tab2ControlText,
            )
        )

        val tabsComponent = TabsComponent(
            tabs = tabs,
            control = TabsComponent.TabControl.Buttons(stack = StackComponent(components = tabControl))
        )

        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(tabsComponent)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val tabsComponentStyle = styleFactory.create(tabsComponent).getOrThrow() as TabsComponentStyle

        // Act
        setContent { TabsComponentView(style = tabsComponentStyle, state = state, clickHandler = { }) }

        // Assert
        fun assertNoSelectedStateDisplayed() {
            onNodeWithText(selectedTextTab0.value)
                .assertIsNotDisplayed()
            onNodeWithText(selectedTextTab1.value)
                .assertIsNotDisplayed()
            onNodeWithText(selectedTextTab2.value)
                .assertIsNotDisplayed()
        }


        // Tab 0 is selected
        onNodeWithText(unselectedTextTab0.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab1.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextTab2.value)
            .assertIsNotDisplayed()
        assertNoSelectedStateDisplayed()

        // Select tab 1
        onNodeWithText(tab1ControlText.value)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(unselectedTextTab0.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextTab1.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextTab2.value)
            .assertIsNotDisplayed()
        assertNoSelectedStateDisplayed()

        // Select tab 2
        onNodeWithText(tab2ControlText.value)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(unselectedTextTab0.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextTab1.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextTab2.value)
            .assertIsDisplayed()
        assertNoSelectedStateDisplayed()
    }

}
