package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
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
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toNonEmptyMapOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class TabsComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultLocaleIdentifier = LocaleId("en_US")

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

    @Test
    fun `Should select the correct default package when switching tabs`(): Unit = with(composeTestRule) {
        // Arrange
        val defaultPackageOnTabZero = TestData.Packages.annual
        val otherPackageOnTabZero = TestData.Packages.monthly
        val defaultPackageOnTabOne = TestData.Packages.weekly
        val otherPackageOnTabOne = TestData.Packages.semester
        val packages = listOf(
            defaultPackageOnTabZero,
            otherPackageOnTabZero,
            defaultPackageOnTabOne,
            otherPackageOnTabOne,
        )
        val rootStack = StackComponent(
            components = listOf(
                TabsComponentWithToggleControl(
                    // Tab 1 is selected by default
                    defaultToggleValue = true,
                    // On tab 0, index 1 is selected by default. On tab 1, index 0 is selected by default.
                    1 to listOf(otherPackageOnTabZero, defaultPackageOnTabZero),
                    0 to listOf(defaultPackageOnTabOne, otherPackageOnTabOne)
                )
            )
        )
        val offering = Offering(
            rootStack = rootStack,
            packages = packages,
        )
        val styleFactory = StyleFactory(offering)
        val rootStackStyle = styleFactory.create(rootStack).getOrThrow() as StackComponentStyle
        val state = paywallComponentsState(offering)

        // Act
        setContent { StackComponentView(style = rootStackStyle, state = state, clickHandler = { }) }

        // Assert
        // Our tab control switch is checked, meaning tab index 1 is selected.
        onNode(isToggleable())
            .assertIsOn()

        // defaultPackageOnTabOne is selected.
        assertPackageIsSelected(selected = defaultPackageOnTabOne, all = packages)

        // Select otherPackageOnTabOne
        onNodeWithText(otherPackageOnTabOne.unselectedLocalizedText.value)
            .performClick()
        assertPackageIsSelected(selected = otherPackageOnTabOne, all = packages)

        // Switch to tab 0
        onNode(isToggleable())
            .performClick()
            .assertIsOff()

        // defaultPackageOnTabZero is selected.
        assertPackageIsSelected(selected = defaultPackageOnTabZero, all = packages)

        // Switch to tab 1
        onNode(isToggleable())
            .performClick()
            .assertIsOn()

        // otherPackageOnTabOne is still selected.
        assertPackageIsSelected(selected = otherPackageOnTabOne, all = packages)
    }

    @Test
    fun `Should not select the default package when switching tabs if the selected package is global`(): Unit =
        with(composeTestRule) {
            // Arrange
            val defaultPackageOnTabZero = TestData.Packages.annual.copy(identifier = "default-package-on-tab-zero")
            val otherPackageOnTabZero = TestData.Packages.monthly.copy(identifier = "other-package-on-tab-zero")
            val defaultPackageOnTabOne = TestData.Packages.weekly.copy(identifier = "default-package-on-tab-one")
            val otherPackageOnTabOne = TestData.Packages.semester.copy(identifier = "other-package-on-tab-one")
            val defaultGlobalPackageAndOnTabOne = TestData.Packages.quarterly
                .copy(identifier = "default-global-and-on-tab-one")

            val packages = listOf(
                defaultPackageOnTabZero,
                otherPackageOnTabZero,
                defaultPackageOnTabOne,
                otherPackageOnTabOne,
                defaultGlobalPackageAndOnTabOne,
            )
            val rootStack = StackComponent(
                components = listOf(
                    TabsComponentWithToggleControl(
                        // Tab 1 is selected by default
                        defaultToggleValue = true,
                        // On tab 0, index 1 is selected by default. On tab 1, index 0 is selected by default.
                        1 to listOf(otherPackageOnTabZero, defaultPackageOnTabZero),
                        0 to listOf(defaultPackageOnTabOne, otherPackageOnTabOne, defaultGlobalPackageAndOnTabOne),
                    ),
                    // We expect this package to be the default selected package, as it (also) exists globally.
                    simplePackageComponent(defaultGlobalPackageAndOnTabOne, isSelectedByDefault = true)
                )
            )
            val offering = Offering(
                rootStack = rootStack,
                packages = packages,
            )
            val styleFactory = StyleFactory(offering)
            val rootStackStyle = styleFactory.create(rootStack).getOrThrow() as StackComponentStyle
            val state = paywallComponentsState(offering)

            // Act
            setContent { StackComponentView(style = rootStackStyle, state = state, clickHandler = { }) }

            // Assert
            // Our tab control switch is checked, meaning tab index 1 is selected.
            onNode(isToggleable())
                .assertIsOn()

            // defaultGlobalPackageAndOnTabOne is selected.
            assertPackageIsSelected(selected = defaultGlobalPackageAndOnTabOne, all = packages)

            // Switch to tab 0
            onNode(isToggleable())
                .performClick()
                .assertIsOff()

            // defaultGlobalPackageAndOnTabOne is still selected.
            assertPackageIsSelected(selected = defaultGlobalPackageAndOnTabOne, all = packages)

            // Select otherPackageOnTabZero
            onNodeWithText(otherPackageOnTabZero.unselectedLocalizedText.value)
                .performClick()
            assertPackageIsSelected(selected = otherPackageOnTabZero, all = packages)

            // Switch to tab 1
            onNode(isToggleable())
                .performClick()
                .assertIsOn()

            // defaultPackageOnTabOne is selected.
            assertPackageIsSelected(selected = defaultPackageOnTabOne, all = packages)
        }

    @Test
    fun `Should have no selected package when switching to a tab without packages`(): Unit = with(composeTestRule) {
        // Arrange
        val defaultPackageOnTabZero = TestData.Packages.annual.copy(identifier = "default-package-on-tab-zero")
        val otherPackageOnTabZero = TestData.Packages.monthly.copy(identifier = "other-package-on-tab-zero")
        val defaultPackageOnTabOne = TestData.Packages.weekly.copy(identifier = "default-package-on-tab-one")
        val otherPackageOnTabOne = TestData.Packages.semester.copy(identifier = "other-package-on-tab-one")

        val packages = listOf(
            defaultPackageOnTabZero,
            otherPackageOnTabZero,
            defaultPackageOnTabOne,
            otherPackageOnTabOne,
        )

        val tabs = tabs(
            // On tab 0, index 1 is selected by default. On tab 1, index 0 is selected by default.
            1 to listOf(otherPackageOnTabZero, defaultPackageOnTabZero),
            0 to listOf(defaultPackageOnTabOne, otherPackageOnTabOne),
        ) +
            // Adding an extra tab without any packages
            Tab(components = listOf())
        val tabControlButtonKeys = List(tabs.size) { index ->
            LocalizationKey("tab_control_button_$index")
        }
        val tabsComponent = TabsComponentWithButtonsControl(
            tabs,
            List(tabs.size) { index ->
                TabControlButtonComponent(
                    tabIndex = index,
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = tabControlButtonKeys[index],
                                color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
                            )
                        )
                    )
                )
            }
        )
        val rootStack = StackComponent(components = listOf(tabsComponent))
        val offering = Offering(
            rootStack = rootStack,
            packages = packages,
        )
        val styleFactory = StyleFactory(offering)
        val rootStackStyle = styleFactory.create(rootStack).getOrThrow() as StackComponentStyle
        val state = paywallComponentsState(offering)

        // Act
        setContent { StackComponentView(style = rootStackStyle, state = state, clickHandler = { }) }

        // Assert
        assertPackageIsSelected(selected = defaultPackageOnTabZero, all = packages)
        // Tab zero is selected
        onNodeWithText(otherPackageOnTabZero.unselectedLocalizedText.value)
            .assertIsDisplayed()

        // Select otherPackageOnTabZero
        onNodeWithText(otherPackageOnTabZero.unselectedLocalizedText.value)
            .performClick()
        assertPackageIsSelected(selected = otherPackageOnTabZero, all = packages)

        // Switch to tab 2, which doesn't have any packages
        onNodeWithText(tabControlButtonKeys[2].asText().value)
            .performClick()

        // Ensure no package is currently selected
        assert(state.selectedPackageInfo == null)

        // Switch back to tab 0
        onNodeWithText(tabControlButtonKeys[0].asText().value)
            .performClick()

        // Our previous selection should still exist
        assertPackageIsSelected(selected = otherPackageOnTabZero, all = packages)
    }

    @Test
    fun `Should select available global package when switching to a tab without packages`(): Unit =
        with(composeTestRule) {
            // Arrange
            val defaultPackageOnTabZero = TestData.Packages.annual.copy(identifier = "default-package-on-tab-zero")
            val otherPackageOnTabZero = TestData.Packages.monthly.copy(identifier = "other-package-on-tab-zero")
            val defaultPackageOnTabOne = TestData.Packages.weekly.copy(identifier = "default-package-on-tab-one")
            val otherPackageOnTabOne = TestData.Packages.semester.copy(identifier = "other-package-on-tab-one")
            val defaultGlobalPackageAndOnTabOne = TestData.Packages.quarterly
                .copy(identifier = "default-global-and-on-tab-one")
            val otherGlobalPackage = TestData.Packages.bimonthly.copy(identifier = "other-global-package")

            val packages = listOf(
                defaultPackageOnTabZero,
                otherPackageOnTabZero,
                defaultPackageOnTabOne,
                otherPackageOnTabOne,
                defaultGlobalPackageAndOnTabOne,
                otherGlobalPackage,
            )

            val tabs = tabs(
                // On tab 0, index 1 is selected by default. On tab 1, index 0 is selected by default.
                1 to listOf(otherPackageOnTabZero, defaultPackageOnTabZero),
                0 to listOf(defaultPackageOnTabOne, otherPackageOnTabOne, defaultGlobalPackageAndOnTabOne),
            ) +
                // Adding an extra tab without any packages
                Tab(components = listOf())
            val tabControlButtonKeys = List(tabs.size) { index ->
                LocalizationKey("tab_control_button_$index")
            }
            val tabsComponent = TabsComponentWithButtonsControl(
                tabs,
                List(tabs.size) { index ->
                    TabControlButtonComponent(
                        tabIndex = index,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = tabControlButtonKeys[index],
                                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
                                )
                            )
                        )
                    )
                }
            )
            val rootStack = StackComponent(
                components = listOf(
                    tabsComponent,
                    simplePackageComponent(otherGlobalPackage, isSelectedByDefault = false),
                    // We expect this package to be the default selected package, as it (also) exists globally.
                    simplePackageComponent(defaultGlobalPackageAndOnTabOne, isSelectedByDefault = true),
                )
            )
            val offering = Offering(
                rootStack = rootStack,
                packages = packages,
            )
            val styleFactory = StyleFactory(offering)
            val rootStackStyle = styleFactory.create(rootStack).getOrThrow() as StackComponentStyle
            val state = paywallComponentsState(offering)

            // Act
            setContent { StackComponentView(style = rootStackStyle, state = state, clickHandler = { }) }

            // Assert
            assertPackageIsSelected(selected = defaultGlobalPackageAndOnTabOne, all = packages)
            // Tab zero is selected
            onNodeWithText(defaultPackageOnTabZero.unselectedLocalizedText.value)
                .assertIsDisplayed()
            onNodeWithText(otherPackageOnTabZero.unselectedLocalizedText.value)
                .assertIsDisplayed()

            // Select otherGlobalPackage
            onNodeWithText(otherGlobalPackage.unselectedLocalizedText.value)
                .performClick()
            assertPackageIsSelected(selected = otherGlobalPackage, all = packages)

            // Select otherPackageOnTabZero
            onNodeWithText(otherPackageOnTabZero.unselectedLocalizedText.value)
                .performClick()
            assertPackageIsSelected(selected = otherPackageOnTabZero, all = packages)

            // Switch to tab 2, which doesn't have any packages
            onNodeWithText(tabControlButtonKeys[2].asText().value)
                .performClick()

            // We expect the defaultGlobalPackage to be selected again, not the otherGlobalPackage.
            assertPackageIsSelected(selected = defaultGlobalPackageAndOnTabOne, all = packages)
        }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeTestRule.assertPackageIsSelected(selected: Package, all: List<Package>) {
        waitUntilAtLeastOneExists(hasText(selected.selectedLocalizedText.value))
        onNodeWithText(selected.unselectedLocalizedText.value)
            .assertIsNotDisplayed()
        all.filterNot { it == selected }
            .forEach { onNodeWithText(it.selectedLocalizedText.value).assertIsNotDisplayed() }
    }

    private fun simplePackageComponent(
        pkg: Package,
        isSelectedByDefault: Boolean,
    ): PackageComponent =
        PackageComponent(
            packageId = pkg.identifier,
            isSelectedByDefault = isSelectedByDefault,
            stack = StackComponent(
                listOf(
                    TextComponent(
                        text = pkg.unselectedLocalizationKey,
                        color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                        overrides = ComponentOverrides(
                            states = ComponentStates(
                                selected = PartialTextComponent(text = pkg.selectedLocalizationKey)
                            )
                        )
                    )
                )
            )
        )

    private fun tabs(vararg tabsBySelectedByDefaultIndex: Pair<Int?, List<Package>>): List<TabsComponent.Tab> =
        tabsBySelectedByDefaultIndex.map { (selectedByDefaultIndex, packagesOnThisTab) ->
            TabsComponent.Tab(
                StackComponent(
                    components = listOf(
                        TabControlComponent
                    ) + packagesOnThisTab.mapIndexed { index, pkg ->
                        simplePackageComponent(
                            pkg = pkg,
                            isSelectedByDefault = (selectedByDefaultIndex == index).also {
                                println("TESTING selecting ${pkg.identifier} by default: $it")
                            },
                        )
                    }
                )
            )
        }

    @Suppress("TestFunctionName")
    private fun Tab(components: List<PaywallComponent>): TabsComponent.Tab =
        TabsComponent.Tab(
            StackComponent(
                components = listOf(TabControlComponent) + components
            )
        )

    @Suppress("TestFunctionName")
    private fun TabControlToggleComponent(defaultValue: Boolean): TabControlToggleComponent =
        TabControlToggleComponent(
            defaultValue = defaultValue,
            thumbColorOn = ColorScheme(ColorInfo.Hex(Color.Blue.toArgb())),
            thumbColorOff = ColorScheme(ColorInfo.Hex(Color.Red.toArgb())),
            trackColorOn = ColorScheme(ColorInfo.Hex(Color.Green.toArgb())),
            trackColorOff = ColorScheme(ColorInfo.Hex(Color.Yellow.toArgb())),
        )

    @Suppress("TestFunctionName")
    private fun TabsComponentWithButtonsControl(
        tabs: List<TabsComponent.Tab>,
        tabControlButtons: List<TabControlButtonComponent>,
    ): TabsComponent =
        TabsComponent(
            tabs = tabs,
            control = TabsComponent.TabControl.Buttons(stack = StackComponent(components = tabControlButtons))
        )

    @Suppress("TestFunctionName")
    private fun TabsComponentWithToggleControl(
        tabs: List<TabsComponent.Tab>,
        tabControl: TabControlToggleComponent
    ): TabsComponent =
        TabsComponent(
            tabs = tabs,
            control = TabsComponent.TabControl.Toggle(stack = StackComponent(components = listOf(tabControl)))
        )

    @Suppress("TestFunctionName")
    private fun TabsComponentWithToggleControl(
        defaultToggleValue: Boolean,
        vararg tabsBySelectedByDefaultIndex: Pair<Int?, List<Package>>
    ): TabsComponent {
        val tabs = tabs(*tabsBySelectedByDefaultIndex)
        val tabControl = TabControlToggleComponent(defaultValue = defaultToggleValue)
        return TabsComponentWithToggleControl(tabs, tabControl)
    }

    @Suppress("TestFunctionName")
    private fun PaywallComponentsData(
        rootStack: StackComponent,
        defaultLocaleId: LocaleId,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>>,
    ): PaywallComponentsData =
        PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = rootStack,
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleId,
        )

    @Suppress("TestFunctionName")
    private fun Offering(
        packages: List<Package>,
        rootStack: StackComponent,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>> = nonEmptyMapOf(
            defaultLocaleIdentifier to rootStack.buildLocalizationNonEmptyMapOrThrow()
        ),
        defaultLocaleId: LocaleId = defaultLocaleIdentifier,
    ): Offering =
        Offering(
            packages = packages,
            paywallComponentsData = PaywallComponentsData(rootStack, defaultLocaleId, localizations)
        )

    @Suppress("TestFunctionName")
    private fun Offering(packages: List<Package>, paywallComponentsData: PaywallComponentsData): Offering =
        Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = packages,
            paywallComponents = Offering.PaywallComponents(UiConfig(), paywallComponentsData),
        )

    @Suppress("TestFunctionName")
    private fun StyleFactory(offering: Offering) =
        StyleFactory(
            offering = offering,
            localizations = offering.paywallComponents
                ?.data
                ?.componentsLocalizations
                ?.toNonEmptyMapOrNull()!!.mapValues { it.value.toNonEmptyMapOrNull()!! }
        )

    private fun paywallComponentsState(offering: Offering): PaywallState.Loaded.Components =
        offering.toComponentsPaywallState(offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!)

    private fun PaywallComponent.buildLocalizationNonEmptyMapOrThrow(): NonEmptyMap<LocalizationKey, LocalizationData.Text> =
        buildLocalizationMap().toNonEmptyMapOrNull()!!

    private fun PaywallComponent.buildLocalizationMap(): Map<LocalizationKey, LocalizationData.Text> =
        getLocalizationKeys().associateWith { localizationKey -> localizationKey.asText() }

    private fun PaywallComponent.getLocalizationKeys(): Set<LocalizationKey> =
        filter { it is TextComponent }
            .map { it as TextComponent }
            .flatMap { component -> component.overrides?.getLocalizationKeys().orEmpty() + component.text }
            .toSet()

    private fun ComponentOverrides<PartialTextComponent>.getLocalizationKeys(): Set<LocalizationKey> =
        listOf(
            introOffer,
            multipleIntroOffers,
            states?.selected,
            conditions?.compact,
            conditions?.medium,
            conditions?.expanded,
        ).mapNotNull { partialTextComponent ->
            partialTextComponent?.text
        }.toSet()

    private val Package.selectedLocalizedText: LocalizationData.Text
        get() = selectedLocalizationKey.asText()

    private val Package.selectedLocalizationKey: LocalizationKey
        get() = LocalizationKey("package_${identifier}_selected")

    private val Package.unselectedLocalizedText: LocalizationData.Text
        get() = unselectedLocalizationKey.asText()

    private val Package.unselectedLocalizationKey: LocalizationKey
        get() = LocalizationKey("package_${identifier}_unselected")

    private val Package.localizedText: LocalizationData.Text
        get() = localizationKey.asText()

    private val Package.localizationKey: LocalizationKey
        get() = LocalizationKey("package_$identifier")

    private fun LocalizationKey.asText(): LocalizationData.Text =
        // Creating fake text, which is just the key with underscores replaced for spaces.
        LocalizationData.Text(value.replace('_', ' '))

    /**
     * Returns all ComponentStyles that satisfy the predicate.
     *
     * Implemented as breadth-first search.
     */
    private fun PaywallComponent.filter(predicate: (PaywallComponent) -> Boolean): List<PaywallComponent> {
        val matches = mutableListOf<PaywallComponent>()
        val queue = ArrayDeque<PaywallComponent>()
        queue.add(this)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (predicate(current)) {
                matches.add(current)
            }

            when (current) {
                is StackComponent -> queue.addAll(current.components)
                is PurchaseButtonComponent -> queue.add(current.stack)
                is ButtonComponent -> queue.add(current.stack)
                is PackageComponent -> queue.add(current.stack)
                is StickyFooterComponent -> queue.add(current.stack)
                is CarouselComponent -> queue.addAll(current.slides)
                is TabControlButtonComponent -> queue.add(current.stack)
                is TabsComponent -> {
                    when (val control = current.control) {
                        is TabsComponent.TabControl.Buttons -> queue.add(control.stack)
                        is TabsComponent.TabControl.Toggle -> queue.add(control.stack)
                    }
                    queue.addAll(current.tabs.map { it.stack })
                }

                is TabControlToggleComponent,
                is TabControlComponent,
                is TimelineComponent,
                is ImageComponent,
                is IconComponent,
                is TextComponent,
                    -> {
                    // These don't have child components.
                }
            }
        }

        return matches
    }
}
