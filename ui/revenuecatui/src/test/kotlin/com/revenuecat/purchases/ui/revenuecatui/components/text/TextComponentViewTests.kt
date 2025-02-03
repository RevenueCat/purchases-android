package com.revenuecat.purchases.ui.revenuecatui.components.text

import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertTextColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.PackageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
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
    private val productMonthlyMxn = TestStoreProduct(
        id = "com.revenuecat.monthly_product",
        name = "Monthly",
        title = "Monthly (App name)",
        price = Price(
            amountMicros = 1_000_000,
            currencyCode = "MXN",
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
    @Suppress("DEPRECATION")
    private val packageMonthlyMxn = Package(
        packageType = PackageType.MONTHLY,
        identifier = "package_monthly",
        offering = offeringId,
        product = productMonthlyMxn,
    )
    private val packageWithoutIntroOffer = TestData.Packages.monthly
    private val packageWithSingleIntroOffer = TestData.Packages.annual
    private val packageWithMultipleIntroOffers = TestData.Packages.quarterly
    private val localeIdEnUs = LocaleId("en_US")
    private val localeIdNlNl = LocaleId("nl_NL")
    private val unselectedLocalizationKey = LocalizationKey("unselected key")
    private val selectedLocalizationKey = LocalizationKey("selected key")
    private val ineligibleLocalizationKey = LocalizationKey("ineligible key")
    private val singleEligibleLocalizationKey = LocalizationKey("single eligible key")
    private val multipleEligibleLocalizationKey = LocalizationKey("multiple eligible key")
    private val expectedTextUnselected = "unselected text"
    private val expectedTextSelected = "selected text"
    private val expectedTextIneligibleEnUs = "ineligible text"
    private val expectedTextSingleEligibleEnUs = "single eligible text"
    private val expectedTextMultipleEligibleEnUs = "multiple eligible text"
    private val localizations = nonEmptyMapOf(
        localeIdEnUs to nonEmptyMapOf(
            LocalizationKey("text1") to LocalizationData.Text("this is text 1"),
            unselectedLocalizationKey to LocalizationData.Text(expectedTextUnselected),
            selectedLocalizationKey to LocalizationData.Text(expectedTextSelected),
            ineligibleLocalizationKey to LocalizationData.Text(expectedTextIneligibleEnUs),
            singleEligibleLocalizationKey to LocalizationData.Text(expectedTextSingleEligibleEnUs),
            multipleEligibleLocalizationKey to LocalizationData.Text(expectedTextMultipleEligibleEnUs),
        ),
    )
    private val styleFactory = StyleFactory(
        localizations = localizations,
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
        val largeTextComponent = TextComponent(text = textId, color = color, fontSize = 28, size = size)
        val smallTextComponent = TextComponent(text = textId, color = color, fontSize = 13, size = size)
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
                    TextComponentView(
                        style = largeTextStyle,
                        state = state,
                        modifier = Modifier.testTag("large")
                    )
                    TextComponentView(
                        style = smallTextStyle,
                        state = state,
                        modifier = Modifier.testTag("small")
                    )
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
        val rcPackage = packageYearly
        val expectedUnselectedTextColor = Color.Black
        val expectedSelectedTextColor = Color.White
        val expectedUnselectedBackgroundColor = Color.Yellow
        val expectedSelectedBackgroundColor = Color.Red
        val component = PackageComponent(
            packageId = rcPackage.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = unselectedLocalizationKey,
                        color = ColorScheme(light = ColorInfo.Hex(expectedUnselectedTextColor.toArgb())),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedUnselectedBackgroundColor.toArgb())),
                        overrides = ComponentOverrides(
                            states = ComponentStates(
                                selected = PartialTextComponent(
                                    text = selectedLocalizationKey,
                                    color = ColorScheme(ColorInfo.Hex(expectedSelectedTextColor.toArgb())),
                                    backgroundColor = ColorScheme(
                                        ColorInfo.Hex(expectedSelectedBackgroundColor.toArgb())
                                    ),
                                ),
                            ),
                        )
                    )
                )
            )
        )
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(component)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(rcPackage),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val style = styleFactory.create(component).getOrThrow() as PackageComponentStyle

        // Act
        setContent {
            // This PackageComponentView has a TextComponentView child.
            PackageComponentView(style = style, state = state)
        }

        // Assert
        onNodeWithText(expectedTextUnselected)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedUnselectedTextColor)
            .assertPixelColorPercentage(expectedUnselectedBackgroundColor) { percentage -> percentage > 0.4 }

        // Select the yearly package
        state.update(selectedPackage = rcPackage)

        onNodeWithText(expectedTextSelected)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedSelectedTextColor)
            .assertPixelColorPercentage(expectedSelectedBackgroundColor) { percentage -> percentage > 0.4 }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the intro offer overrides for the selected package`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedIneligibleTextColor = Color.Black
        val expectedIneligibleBackgroundColor = Color.Yellow
        val expectedSingleEligibleTextColor = Color.White
        val expectedSingleEligibleBackgroundColor = Color.Red
        val expectedMultiEligibleTextColor = Color.Blue
        val expectedMultiEligibleBackgroundColor = Color.Green
        val component = TextComponent(
            text = ineligibleLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleTextColor.toArgb())),
            backgroundColor = ColorScheme(ColorInfo.Hex(expectedIneligibleBackgroundColor.toArgb())),
            overrides = ComponentOverrides(
                introOffer = PartialTextComponent(
                    text = singleEligibleLocalizationKey,
                    color = ColorScheme(ColorInfo.Hex(expectedSingleEligibleTextColor.toArgb())),
                    backgroundColor = ColorScheme(ColorInfo.Hex(expectedSingleEligibleBackgroundColor.toArgb())),
                ),
                multipleIntroOffers = PartialTextComponent(
                    text = multipleEligibleLocalizationKey,
                    color = ColorScheme(ColorInfo.Hex(expectedMultiEligibleTextColor.toArgb())),
                    backgroundColor = ColorScheme(ColorInfo.Hex(expectedMultiEligibleBackgroundColor.toArgb())),
                )
            )
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            components = listOf(component),
            packages = listOf(packageWithoutIntroOffer, packageWithSingleIntroOffer, packageWithMultipleIntroOffers)
        )
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle

        // Act
        setContent { TextComponentView(style = style, state = state) }

        // Assert
        state.update(selectedPackage = packageWithoutIntroOffer)
        onNodeWithText(expectedTextIneligibleEnUs)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedIneligibleTextColor)
            .assertPixelColorPercentage(expectedIneligibleBackgroundColor) { percentage -> percentage > 0.4 }

        state.update(selectedPackage = packageWithSingleIntroOffer)
        onNodeWithText(expectedTextSingleEligibleEnUs)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedSingleEligibleTextColor)
            .assertPixelColorPercentage(expectedSingleEligibleBackgroundColor) { percentage -> percentage > 0.4 }

        state.update(selectedPackage = packageWithMultipleIntroOffers)
        onNodeWithText(expectedTextMultipleEligibleEnUs)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedMultiEligibleTextColor)
            .assertPixelColorPercentage(expectedMultiEligibleBackgroundColor) { percentage -> percentage > 0.4 }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the intro offer overrides as child of PackageComponentView`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedIneligibleTextColor = Color.Black
        val expectedIneligibleBackgroundColor = Color.Yellow
        val expectedSingleEligibleTextColor = Color.White
        val expectedSingleEligibleBackgroundColor = Color.Red
        val expectedMultiEligibleTextColor = Color.Blue
        val expectedMultiEligibleBackgroundColor = Color.Green
        val textComponent = TextComponent(
            text = ineligibleLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleTextColor.toArgb())),
            backgroundColor = ColorScheme(ColorInfo.Hex(expectedIneligibleBackgroundColor.toArgb())),
            overrides = ComponentOverrides(
                introOffer = PartialTextComponent(
                    text = singleEligibleLocalizationKey,
                    color = ColorScheme(ColorInfo.Hex(expectedSingleEligibleTextColor.toArgb())),
                    backgroundColor = ColorScheme(ColorInfo.Hex(expectedSingleEligibleBackgroundColor.toArgb())),
                ),
                multipleIntroOffers = PartialTextComponent(
                    text = multipleEligibleLocalizationKey,
                    color = ColorScheme(ColorInfo.Hex(expectedMultiEligibleTextColor.toArgb())),
                    backgroundColor = ColorScheme(ColorInfo.Hex(expectedMultiEligibleBackgroundColor.toArgb())),
                )
            )
        )
        val noIntroOfferPackageComponent = PackageComponent(
            packageId = packageWithoutIntroOffer.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(textComponent)),
        )
        val singleIntroOfferPackageComponent = PackageComponent(
            packageId = packageWithSingleIntroOffer.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(textComponent)),
        )
        val multipleIntroOffersPackageComponent = PackageComponent(
            packageId = packageWithMultipleIntroOffers.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(textComponent)),
        )
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            noIntroOfferPackageComponent,
                            singleIntroOfferPackageComponent,
                            multipleIntroOffersPackageComponent
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(
                packageWithoutIntroOffer,
                packageWithSingleIntroOffer,
                packageWithMultipleIntroOffers
            ),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val noIntroOfferPackageComponentStyle =
            styleFactory.create(noIntroOfferPackageComponent).getOrThrow() as PackageComponentStyle
        val singleIntroOfferPackageComponentStyle =
            styleFactory.create(singleIntroOfferPackageComponent).getOrThrow() as PackageComponentStyle
        val multipleIntroOffersPackageComponentStyle =
            styleFactory.create(multipleIntroOffersPackageComponent).getOrThrow() as PackageComponentStyle

        // Act
        setContent {
            Column {
                PackageComponentView(
                    style = noIntroOfferPackageComponentStyle,
                    state = state,
                    modifier = Modifier.testTag("noIntroOffer")
                )
                PackageComponentView(
                    style = singleIntroOfferPackageComponentStyle,
                    state = state,
                    modifier = Modifier.testTag("singleIntroOffer")
                )
                PackageComponentView(
                    style = multipleIntroOffersPackageComponentStyle,
                    state = state,
                    modifier = Modifier.testTag("multipleIntroOffers")
                )
            }
        }

        // Assert
        fun assertAll() {
            onNodeWithTag("noIntroOffer")
                .assertIsDisplayed()
                .assertTextEquals(expectedTextIneligibleEnUs)
                .assertTextColorEquals(expectedIneligibleTextColor)
                .assertPixelColorPercentage(expectedIneligibleBackgroundColor) { percentage -> percentage > 0.4 }

            onNodeWithTag("singleIntroOffer")
                .assertIsDisplayed()
                .assertTextEquals(expectedTextSingleEligibleEnUs)
                .assertTextColorEquals(expectedSingleEligibleTextColor)
                .assertPixelColorPercentage(expectedSingleEligibleBackgroundColor) { percentage -> percentage > 0.4 }

            onNodeWithTag("multipleIntroOffers")
                .assertIsDisplayed()
                .assertTextEquals(expectedTextMultipleEligibleEnUs)
                .assertTextColorEquals(expectedMultiEligibleTextColor)
                .assertPixelColorPercentage(expectedMultiEligibleBackgroundColor) { percentage -> percentage > 0.4 }
        }

        // Make sure the selected package does not influence the package used to pick the override properties.
        assertAll()
        state.update(selectedPackage = packageWithoutIntroOffer)
        assertAll()
        state.update(selectedPackage = packageWithSingleIntroOffer)
        assertAll()
        state.update(selectedPackage = packageWithMultipleIntroOffers)
        assertAll()
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
        val styleFactory = StyleFactory(localizations)
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
                    text = singleEligibleLocalizationKey,
                ),
            )
        )
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                ineligibleLocalizationKey to LocalizationData.Text(unexpectedText),
                singleEligibleLocalizationKey to LocalizationData.Text(expectedTextEnUs),
            ),
            localeIdNlNl to nonEmptyMapOf(
                ineligibleLocalizationKey to LocalizationData.Text(unexpectedText),
                singleEligibleLocalizationKey to LocalizationData.Text(expectedTextNlNl),
            )
        )
        val styleFactory = StyleFactory(localizations)
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            components = listOf(component),
            packages = listOf(packageWithSingleIntroOffer)
        ).apply {
            update(selectedPackage = packageWithSingleIntroOffer)
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
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
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
                TextComponentView(
                    style = styleSelected,
                    state = state,
                    modifier = Modifier.testTag("selected")
                )
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

    @Test
    fun `Should correctly show or hide price decimals`(): Unit = with(composeTestRule) {
        // Arrange
        val textColor = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))
        val defaultLocaleIdentifier = LocaleId("en_US")
        val usdPackage = packageYearly
        val mxnPackage = packageMonthlyMxn
        val countryWithoutDecimals = "MX"
        val textKey = LocalizationKey("key_selected")
        val textWithPriceVariable = LocalizationData.Text("Price: {{ price }}")
        val expectedTextWithDecimals = "Price: \$ 2.00"
        val expectedTextWithoutDecimals = "Price: MX\$1"
        val localizations = nonEmptyMapOf(
            defaultLocaleIdentifier to nonEmptyMapOf(
                textKey to textWithPriceVariable,
            )
        )
        val component = TextComponent(text = textKey, color = textColor)
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(component)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleIdentifier,
            zeroDecimalPlaceCountries = listOf(countryWithoutDecimals),
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(usdPackage, mxnPackage),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        // We create 3 PaywallStates for different country codes. Also make sure our package is the selected one, so
        // the variables take its values. Otherwise our TextComponent would need to be a child of a PackageComponent
        // for it to take those values.
        val stateWithNullStoreFrontCountryCode = offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = null
        ).apply { update(selectedPackage = usdPackage) }
        val stateWithNlStoreFrontCountryCode = offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = "NL",
        ).apply { update(selectedPackage = usdPackage) }
        val stateWithMxStoreFrontCountryCode = offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = countryWithoutDecimals,
        ).apply { update(selectedPackage = mxnPackage) }

        val styleFactory = StyleFactory(
            localizations = localizations,
            offering = offering,
        )
        val styleSelected = styleFactory.create(component).getOrThrow() as TextComponentStyle

        // Act
        setContent {
            Column {
                TextComponentView(
                    style = styleSelected,
                    state = stateWithNullStoreFrontCountryCode,
                    modifier = Modifier.testTag("country-null")
                )
                TextComponentView(
                    style = styleSelected,
                    state = stateWithNlStoreFrontCountryCode,
                    modifier = Modifier.testTag("country-nl")
                )
                TextComponentView(
                    style = styleSelected,
                    state = stateWithMxStoreFrontCountryCode,
                    modifier = Modifier.testTag("country-mx")
                )
            }
        }

        // Assert
        onNodeWithTag("country-null")
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(expectedTextWithDecimals)

        onNodeWithTag("country-nl")
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(expectedTextWithDecimals)

        onNodeWithTag("country-mx")
            .assertIsDisplayed()
            .onChild()
            .assertTextEquals(expectedTextWithoutDecimals)
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
