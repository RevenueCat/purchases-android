package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
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
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class PackageComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val productYearly = TestStoreProduct(
        id = "com.revenuecat.annual_product",
        name = "Annual",
        title = "Annual (App name)",
        price = Price(
            amountMicros = 2_000_000,
            currencyCode = "USD",
            formatted = "$ 2.00",
        ),
        description = "Annual",
        period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
    )
    private val productMonthly = TestStoreProduct(
        id = "com.revenuecat.monthly_product",
        name = "Monthly",
        title = "Monthly (App name)",
        price = Price(
            amountMicros = 1_000_000,
            currencyCode = "USD",
            formatted = "$ 1.00",
        ),
        description = "Monthly",
        period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
    )
    private val offeringId = "offering_identifier"
    @Suppress("DEPRECATION")
    private val packageYearly = Package(
        packageType = PackageType.ANNUAL,
        identifier = "package_yearly",
        offering = offeringId,
        product = productYearly,
    )
    @Suppress("DEPRECATION")
    private val packageMonthly = Package(
        packageType = PackageType.MONTHLY,
        identifier = "package_monthly",
        offering = offeringId,
        product = productMonthly,
    )

    @Test
    fun `Should properly update selected state of children`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")
        val unselectedKeyYearly = LocalizationKey("unselected_yearly")
        val selectedKeyYearly = LocalizationKey("selected_yearly")
        val unselectedKeyMonthly = LocalizationKey("unselected_monthly")
        val selectedKeyMonthly = LocalizationKey("selected_monthly")
        val unselectedTextYearly = LocalizationData.Text("yearly package unselected")
        val selectedTextYearly = LocalizationData.Text("yearly package selected")
        val unselectedTextMonthly = LocalizationData.Text("monthly package unselected")
        val selectedTextMonthly = LocalizationData.Text("monthly package selected")
        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                unselectedKeyYearly to unselectedTextYearly,
                selectedKeyYearly to selectedTextYearly,
                unselectedKeyMonthly to unselectedTextMonthly,
                selectedKeyMonthly to selectedTextMonthly,
            )
        )
        val componentYearly = PackageComponent(
            packageId = packageYearly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = unselectedKeyYearly,
                        color = textColor,
                        horizontalAlignment = HorizontalAlignment.LEADING,
                        overrides = ComponentOverrides(
                            states = ComponentStates(
                                selected = PartialTextComponent(
                                    text = selectedKeyYearly,
                                )
                            )
                        )
                    )
                ),
            )
        )
        val componentMonthly = PackageComponent(
            packageId = packageMonthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = unselectedKeyMonthly,
                        color = textColor,
                        horizontalAlignment = HorizontalAlignment.LEADING,
                        overrides = ComponentOverrides(
                            states = ComponentStates(
                                selected = PartialTextComponent(
                                    text = selectedKeyMonthly,
                                )
                            )
                        )
                    )
                ),
            )
        )

        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(componentYearly, componentMonthly)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(packageYearly, packageMonthly),
            paywallComponents = data,
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val styleYearly = styleFactory.create(componentYearly).getOrThrow() as PackageComponentStyle
        val styleMonthly = styleFactory.create(componentMonthly).getOrThrow() as PackageComponentStyle

        // Act
        setContent {
            Column {
                PackageComponentView(style = styleYearly, state = state, modifier = Modifier.testTag("yearly"))
                PackageComponentView(style = styleMonthly, state = state, modifier = Modifier.testTag("monthly"))
            }
        }

        // Assert
        // Nothing selected
        onNodeWithText(unselectedTextYearly.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextMonthly.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextYearly.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextMonthly.value)
            .assertIsNotDisplayed()

        // Select yearly
        onNodeWithTag("yearly")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(unselectedTextYearly.value)
            .assertIsNotDisplayed()
        onNodeWithText(unselectedTextMonthly.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextYearly.value)
            .assertIsDisplayed()
        onNodeWithText(selectedTextMonthly.value)
            .assertIsNotDisplayed()

        // Select monthly
        onNodeWithTag("monthly")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(unselectedTextYearly.value)
            .assertIsDisplayed()
        onNodeWithText(unselectedTextMonthly.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextYearly.value)
            .assertIsNotDisplayed()
        onNodeWithText(selectedTextMonthly.value)
            .assertIsDisplayed()
    }

    @Test
    fun `Should take variable values from the correct package`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")
        // Both packages use the same unprocessed text.
        val textKey = LocalizationKey("key")
        val textWithVariable = LocalizationData.Text("Product: {{ product_name }}")
        // However the displayed text should be different, as the product names are different.
        val expectedTextYearly = "Product: ${packageYearly.product.name}"
        val expectedTextMonthly = "Product: ${packageMonthly.product.name}"
        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                textKey to textWithVariable,
            )
        )
        val componentYearly = PackageComponent(
            packageId = packageYearly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = textKey,
                        color = textColor,
                    )
                ),
            )
        )
        val componentMonthly = PackageComponent(
            packageId = packageMonthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = textKey,
                        color = textColor,
                    )
                ),
            )
        )
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(componentYearly, componentMonthly)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(packageYearly, packageMonthly),
            paywallComponents = data,
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val styleYearly = styleFactory.create(componentYearly).getOrThrow() as PackageComponentStyle
        val styleMonthly = styleFactory.create(componentMonthly).getOrThrow() as PackageComponentStyle

        // Act
        setContent {
            Column {
                PackageComponentView(style = styleYearly, state = state, modifier = Modifier.testTag("yearly"))
                PackageComponentView(style = styleMonthly, state = state, modifier = Modifier.testTag("monthly"))
            }
        }

        // Assert
        onNodeWithText(expectedTextYearly)
            .assertIsDisplayed()
        onNodeWithText(expectedTextMonthly)
            .assertIsDisplayed()
    }

    /**
     * This is more of an integration test as it is not exactly testing logic in the PackageComponentView, but this
     * allows us to assert this behavior from a higher level.
     */
    @Test
    fun `Should select the correct package by default`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")
        val yearlyPackageTextKey = LocalizationKey("key_yearly")
        val monthlyPackageTextKey = LocalizationKey("key_monthly")
        val selectedPackageTextKey = LocalizationKey("key_selected")
        val yearlyPackageText = LocalizationData.Text("Yearly")
        val monthlyPackageText = LocalizationData.Text("Monthly")
        val selectedPackageTextWithVariable = LocalizationData.Text("Selected product: {{ product_name }}")
        val expectedTextYearly = "Selected product: ${packageYearly.product.name}"
        val expectedTextMonthly = "Selected product: ${packageMonthly.product.name}"
        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                yearlyPackageTextKey to yearlyPackageText,
                monthlyPackageTextKey to monthlyPackageText,
                selectedPackageTextKey to selectedPackageTextWithVariable,
            )
        )
        // We have 2 packages, one of which is selected by default.
        val yearlyComponent = PackageComponent(
            packageId = packageYearly.identifier,
            isSelectedByDefault = true,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = yearlyPackageTextKey,
                        color = textColor,
                    )
                ),
            )
        )
        val monthlyComponent = PackageComponent(
            packageId = packageMonthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = monthlyPackageTextKey,
                        color = textColor,
                    )
                ),
            )
        )
        // And we have a TextComponent containing a variable of which the value should reflect the currently selected
        // package.
        val selectedComponent = TextComponent(
            text = selectedPackageTextKey,
            color = textColor,
        )
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(yearlyComponent, monthlyComponent, selectedComponent)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(packageYearly, packageMonthly),
            paywallComponents = data,
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val styleYearly = styleFactory.create(yearlyComponent).getOrThrow() as PackageComponentStyle
        val styleMonthly = styleFactory.create(monthlyComponent).getOrThrow() as PackageComponentStyle
        val styleSelected = styleFactory.create(selectedComponent).getOrThrow() as TextComponentStyle

        // Act
        setContent {
            Column {
                PackageComponentView(style = styleYearly, state = state, modifier = Modifier.testTag("yearly"))
                PackageComponentView(style = styleMonthly, state = state, modifier = Modifier.testTag("monthly"))
                // The text component is not a child of a package component, so any variables should take its values
                // from the currently selected package.
                TextComponentView(style = styleSelected, state = state, modifier = Modifier.testTag("selected"))
            }
        }

        // Assert
        // Yearly selected by default
        onNodeWithText(expectedTextYearly)
            .assertIsDisplayed()
        onNodeWithText(expectedTextMonthly)
            .assertIsNotDisplayed()

        // Select monthly
        onNodeWithTag("monthly")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(expectedTextYearly)
            .assertIsNotDisplayed()
        onNodeWithText(expectedTextMonthly)
            .assertIsDisplayed()

        // Select yearly again
        onNodeWithTag("yearly")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(expectedTextYearly)
            .assertIsDisplayed()
        onNodeWithText(expectedTextMonthly)
            .assertIsNotDisplayed()
    }

}
