package com.revenuecat.purchases.ui.revenuecatui.components.text

import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertTextColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.net.URL

@RunWith(AndroidJUnit4::class)
class TextComponentViewTests {

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
    private val localeIdEnUs = LocaleId("en_US")
    private val localeIdNlNl = LocaleId("nl_NL")
    private val unselectedLocalizationKey = LocalizationKey("unselected key")
    private val selectedLocalizationKey = LocalizationKey("selected key")
    private val ineligibleLocalizationKey = LocalizationKey("ineligible key")
    private val eligibleLocalizationKey = LocalizationKey("eligible key")
    private val expectedTextUnselected = "unselected text"
    private val expectedTextSelected = "selected text"
    private val expectedTextIneligibleEnUs = "ineligible text"
    private val expectedTextEligibleEnUs = "eligible text"
    private val localizations = nonEmptyMapOf(
        localeIdEnUs to nonEmptyMapOf(
            LocalizationKey("text1") to LocalizationData.Text("this is text 1"),
            unselectedLocalizationKey to LocalizationData.Text(expectedTextUnselected),
            selectedLocalizationKey to LocalizationData.Text(expectedTextSelected),
            ineligibleLocalizationKey to LocalizationData.Text(expectedTextIneligibleEnUs),
            eligibleLocalizationKey to LocalizationData.Text(expectedTextEligibleEnUs),
        ),
    )
    private val styleFactory = StyleFactory(
        localizations = localizations,
        offering = Offering(
            identifier = "identifier",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
        )
    )

    @Test
    fun `Should change text color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val component = TextComponent(
            text = localizations.getValue(localeIdEnUs).keys.first(),
            color = ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as TextComponentStyle
            },
            act = { TextComponentView(style = it, state = state) },
            assert = { theme ->
                theme.setLight()
                onNodeWithText(localizations.getValue(localeIdEnUs).values.first().value)
                    .assertIsDisplayed()
                    .assertTextColorEquals(expectedLightColor)

                theme.setDark()
                onNodeWithText(localizations.getValue(localeIdEnUs).values.first().value)
                    .assertIsDisplayed()
                    .assertTextColorEquals(expectedDarkColor)
            }
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should change background color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val component = TextComponent(
            text = localizations.getValue(localeIdEnUs).keys.first(),
            color = ColorScheme(
                // We're setting the text color to transparent, because our way of checking the background is far from
                // optimal. It just checks a few pixels.
                light = ColorInfo.Hex(Color.Transparent.toArgb()),
            ),
            backgroundColor = ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as TextComponentStyle
            },
            act = { TextComponentView(style = it, state = state) },
            assert = { theme ->
                theme.setLight()
                onNodeWithText(localizations.getValue(localeIdEnUs).values.first().value)
                    .assertIsDisplayed()
                    .assertBackgroundColorEquals(expectedLightColor)

                theme.setDark()
                onNodeWithText(localizations.getValue(localeIdEnUs).values.first().value)
                    .assertIsDisplayed()
                    .assertBackgroundColorEquals(expectedDarkColor)
            }
        )
    }

    /**
     * There's some interplay between a Material3 theme and our Markdown component. If both of these are present in the
     * Compose tree, the font size in the Markdown component did not have any effect. This is fixed in #1981.
     * Unfortunately this bug does not show up in Compose Previews. Hence this test to protect against regressions.
     */
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [34])
    @Test
    fun `Should properly set the font size in a Material3 theme`(): Unit = with(composeTestRule) {
        // Arrange
        val textId = localizations.getValue(localeIdEnUs).keys.first()
        val color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
        val size = Size(Fit, Fit)
        val largeTextComponent = TextComponent(text = textId, color = color, fontSize = FontSize.HEADING_L, size = size)
        val smallTextComponent = TextComponent(text = textId, color = color, fontSize = FontSize.BODY_S, size = size)
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            largeTextComponent,
            smallTextComponent
        )
        setContent {
            val largeTextStyle = styleFactory.create(largeTextComponent).getOrThrow() as TextComponentStyle
            val smallTextStyle = styleFactory.create(smallTextComponent).getOrThrow() as TextComponentStyle

            // Act
            MaterialTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    TextComponentView(style = largeTextStyle, state = state, modifier = Modifier.testTag("large"))
                    TextComponentView(style = smallTextStyle, state = state, modifier = Modifier.testTag("small"))
                }
            }
        }

        // Assert
        val largeSize = onNodeWithTag("large")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .size

        val smallSize = onNodeWithTag("small")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .size

        assertThat(largeSize.height).isGreaterThan(smallSize.height)
        assertThat(largeSize.width).isGreaterThan(smallSize.width)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the selected overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedUnselectedTextColor = Color.Black
        val expectedSelectedTextColor = Color.White
        val expectedUnselectedBackgroundColor = Color.Yellow
        val expectedSelectedBackgroundColor = Color.Red
        val component = TextComponent(
            text = unselectedLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(expectedUnselectedTextColor.toArgb())),
            backgroundColor = ColorScheme(ColorInfo.Hex(expectedUnselectedBackgroundColor.toArgb())),
            overrides = ComponentOverrides(
                states = ComponentStates(
                    selected = PartialTextComponent(
                        text = selectedLocalizationKey,
                        color = ColorScheme(ColorInfo.Hex(expectedSelectedTextColor.toArgb())),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedSelectedBackgroundColor.toArgb())),
                    ),
                ),
            )
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle

        // Act
        setContent {
            var selected by remember { mutableStateOf(false) }
            TextComponentView(style = style, state = state, selected = selected)
            Switch(checked = selected, onCheckedChange = { selected = it }, modifier = Modifier.testTag("switch"))
        }

        // Assert
        onNodeWithText(expectedTextUnselected)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedUnselectedTextColor)
            .assertPixelColorPercentage(expectedUnselectedBackgroundColor) { percentage -> percentage > 0.4 }

        // Change `selected` to true.
        onNodeWithTag("switch")
            .performClick()

        onNodeWithText(expectedTextSelected)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedSelectedTextColor)
            .assertPixelColorPercentage(expectedSelectedBackgroundColor) { percentage -> percentage > 0.4 }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the intro offer overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedIneligibleTextColor = Color.Black
        val expectedEligibleTextColor = Color.White
        val expectedIneligibleBackgroundColor = Color.Yellow
        val expectedEligibleBackgroundColor = Color.Red
        val component = TextComponent(
            text = ineligibleLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleTextColor.toArgb())),
            backgroundColor = ColorScheme(ColorInfo.Hex(expectedIneligibleBackgroundColor.toArgb())),
            overrides = ComponentOverrides(
                introOffer = PartialTextComponent(
                    text = eligibleLocalizationKey,
                    color = ColorScheme(ColorInfo.Hex(expectedEligibleTextColor.toArgb())),
                    backgroundColor = ColorScheme(ColorInfo.Hex(expectedEligibleBackgroundColor.toArgb())),
                ),
            )
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle

        // Act
        setContent { TextComponentView(style = style, state = state) }

        // Assert
        state.update(isEligibleForIntroOffer = false)
        onNodeWithText(expectedTextIneligibleEnUs)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedIneligibleTextColor)
            .assertPixelColorPercentage(expectedIneligibleBackgroundColor) { percentage -> percentage > 0.4 }

        state.update(isEligibleForIntroOffer = true)
        onNodeWithText(expectedTextEligibleEnUs)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedEligibleTextColor)
            .assertPixelColorPercentage(expectedEligibleBackgroundColor) { percentage -> percentage > 0.4 }
    }

    @Test
    fun `Should use the correct text when the locale changes`(): Unit = with(composeTestRule) {
        val expectedTextEnUs = "expected"
        val expectedTextNlNl = "verwacht"
        val component = TextComponent(
            text = ineligibleLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
        )
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                ineligibleLocalizationKey to LocalizationData.Text(expectedTextEnUs),
            ),
            localeIdNlNl to nonEmptyMapOf(
                ineligibleLocalizationKey to LocalizationData.Text(expectedTextNlNl),
            )
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
        )
        val styleFactory = StyleFactory(localizations, offering)
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )

        // Act
        setContent { TextComponentView(style = style, state = state) }

        // Assert
        state.update(localeList = LocaleList(localeIdEnUs.toJavaLocale()))
        onNodeWithText(expectedTextEnUs).assertIsDisplayed()

        state.update(localeList = LocaleList(localeIdNlNl.toJavaLocale()))
        onNodeWithText(expectedTextNlNl).assertIsDisplayed()
    }

    @Test
    fun `Should use the correct override text when the locale changes`(): Unit = with(composeTestRule) {
        val expectedTextEnUs = "expected"
        val expectedTextNlNl = "verwacht"
        val unexpectedText = "unexpected"
        val component = TextComponent(
            text = ineligibleLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
            overrides = ComponentOverrides(
                introOffer = PartialTextComponent(
                    text = eligibleLocalizationKey,
                ),
            )
        )
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                ineligibleLocalizationKey to LocalizationData.Text(unexpectedText),
                eligibleLocalizationKey to LocalizationData.Text(expectedTextEnUs),
            ),
            localeIdNlNl to nonEmptyMapOf(
                ineligibleLocalizationKey to LocalizationData.Text(unexpectedText),
                eligibleLocalizationKey to LocalizationData.Text(expectedTextNlNl),
            )
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
        )
        val styleFactory = StyleFactory(localizations, offering)
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        ).apply {
            update(isEligibleForIntroOffer = true)
        }

        // Act
        setContent { TextComponentView(style = style, state = state) }

        // Assert
        state.update(localeList = LocaleList(localeIdEnUs.toJavaLocale()))
        onNodeWithText(expectedTextEnUs).assertIsDisplayed()

        state.update(localeList = LocaleList(localeIdNlNl.toJavaLocale()))
        onNodeWithText(expectedTextNlNl).assertIsDisplayed()
    }

    @Test
    fun `Should update variable values when the selected package changes`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")
        val selectedPackageTextKey = LocalizationKey("key_selected")
        val selectedPackageTextWithVariable = LocalizationData.Text("Selected product: {{ product_name }}")
        val expectedTextYearly = "Selected product: ${packageYearly.product.name}"
        val expectedTextMonthly = "Selected product: ${packageMonthly.product.name}"
        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                selectedPackageTextKey to selectedPackageTextWithVariable,
            )
        )
        val selectedComponent = TextComponent(
            text = selectedPackageTextKey,
            color = textColor,
        )
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(selectedComponent)),
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
        val styleSelected = styleFactory.create(selectedComponent).getOrThrow() as TextComponentStyle

        // Act
        setContent {
            Column {
                TextComponentView(style = styleSelected, state = state, modifier = Modifier.testTag("selected"))
            }
        }

        // Assert
        // Select monthly
        state.update(selectedPackage = packageMonthly)

        onNodeWithText(expectedTextYearly)
            .assertIsNotDisplayed()
        onNodeWithText(expectedTextMonthly)
            .assertIsDisplayed()

        // Select yearly
        state.update(selectedPackage = packageYearly)

        onNodeWithText(expectedTextYearly)
            .assertIsDisplayed()
        onNodeWithText(expectedTextMonthly)
            .assertIsNotDisplayed()
    }

    /**
     * This is a very naive way of checking the background color: by just looking at the 16 top-left pixels. It works
     * for the particular test where it is used, because the color is solid and the text is transparent, but it
     * shouldn't be used more generally.
     *
     * See the documentation for [assertPixelColorEquals] for required annotations.
     */
    private fun SemanticsNodeInteraction.assertBackgroundColorEquals(color: Color): SemanticsNodeInteraction =
        assertPixelColorEquals(startX = 0, startY = 0, width = 4, height = 4, color = color)
}
