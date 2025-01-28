package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.ComponentStates
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertApproximatePixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertNoPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertRectangularBorderColor
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.PackageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.net.URL

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class StackComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val localeIdEnUs = LocaleId("en_US")
    private val localizations = nonEmptyMapOf(
        localeIdEnUs to nonEmptyMapOf(
            LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
        )
    )
    private val styleFactory = StyleFactory(
        localizations = localizations,
        uiConfig = UiConfig(),
        fontAliases = emptyMap(),
        offering = Offering(
            identifier = "identifier",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
        )
    )
    private val packageWithoutIntroOffer = TestData.Packages.monthly
    private val packageWithSingleIntroOffer = TestData.Packages.annual
    private val packageWithMultipleIntroOffers = TestData.Packages.quarterly

    @Test
    fun `Should change background color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(100u), Fixed(100u)),
            backgroundColor = ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as StackComponentStyle
            },
            act = {
                StackComponentView(
                    style = it,
                    state = state,
                    clickHandler = { },
                    modifier = Modifier.testTag("stack")
                )
                  },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedLightColor)

                theme.setDark()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedDarkColor)
            }
        )
    }

    @Test
    fun `Should change border color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val sizeDp = 100
        val borderWidthDp = 10.0
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val expectedBackgroundColor = Color.White
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(sizeDp.toUInt()), Fixed(sizeDp.toUInt())),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb())),
            border = Border(
                color = ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
                ),
                width = borderWidthDp
            ),
        )
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as StackComponentStyle
            },
            act = {
                StackComponentView(
                style = it,
                state = state,
                clickHandler = { },
                modifier = Modifier.testTag("stack")
            )
                  },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertRectangularBorderColor(
                        borderWidth = borderWidthDp.dp,
                        expectedBorderColor = expectedLightColor,
                        expectedBackgroundColor = expectedBackgroundColor,
                    )

                theme.setDark()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertRectangularBorderColor(
                        borderWidth = borderWidthDp.dp,
                        expectedBorderColor = expectedDarkColor,
                        expectedBackgroundColor = expectedBackgroundColor,
                    )
            }
        )
    }

    @Test
    fun `Should change shadow color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val parentSizeDp = 200
        val stackSizeDp = 100
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val expectedBackgroundColor = Color.White
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(stackSizeDp.toUInt()), Fixed(stackSizeDp.toUInt())),
            shadow = Shadow(
                color = ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
                ),
                radius = 5.0,
                x = 10.0,
                y = 10.0,
            ),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb())),
        )
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as StackComponentStyle
            },
            act = {
                // An outer box, because a shadow draws outside the Composable's bounds.
                Box(
                    modifier = Modifier
                        .testTag(tag = "parent")
                        .requiredSize(parentSizeDp.dp)
                        .background(expectedBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    StackComponentView(
                        style = it,
                        state = state,
                        clickHandler = { },
                        modifier = Modifier.testTag("stack")
                    )
                }
            },
            assert = { theme ->
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    // No inner shadow, so the entire stack should be the same color.
                    .assertPixelColorEquals(expectedBackgroundColor)

                theme.setLight()
                onNodeWithTag("parent")
                    .assertIsDisplayed()
                    // When the shadow is drawn, at least some pixels are the exact color we're looking for.
                    .assertApproximatePixelColorPercentage(expectedLightColor, threshold = 0.1f) { it > 0f }
                    .assertNoPixelColorEquals(expectedDarkColor)

                theme.setDark()
                onNodeWithTag("parent")
                    .assertIsDisplayed()
                    // When the shadow is drawn, at least some pixels are the exact color we're looking for.
                    .assertApproximatePixelColorPercentage(expectedDarkColor, threshold = 0.1f) { it > 0f }
                    .assertNoPixelColorEquals(expectedLightColor)
            }
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the selected overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val rcPackage = TestData.Packages.annual
        val parentSizeDp = 200u
        val stackSizeDp = 100u
        val expectedUnselectedBorderColor = Color.Black
        val expectedSelectedBorderColor = Color.Cyan
        val expectedUnselectedBorderWidth = 2.0
        val expectedSelectedBorderWidth = 4.0
        val expectedUnselectedShadowColor = Color.Yellow
        val expectedSelectedShadowColor = Color.Red
        val expectedSelectedBackgroundColor = Color.Blue
        val expectedUnselectedBackgroundColor = Color.Green
        val parentBackgroundColor = Color.Magenta
        val component = PackageComponent(
            packageId = rcPackage.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = emptyList(),
                size = Size(Fixed(stackSizeDp), Fixed(stackSizeDp)),
                backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedUnselectedBackgroundColor.toArgb())),
                border = Border(
                    color = ColorScheme(light = ColorInfo.Hex(expectedUnselectedBorderColor.toArgb())),
                    width = expectedUnselectedBorderWidth
                ),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Hex(expectedUnselectedShadowColor.toArgb())),
                    radius = 5.0,
                    x = 10.0,
                    y = 10.0,
                ),
                overrides = ComponentOverrides(
                    states = ComponentStates(
                        selected = PartialStackComponent(
                            backgroundColor = ColorScheme(ColorInfo.Hex(expectedSelectedBackgroundColor.toArgb())),
                            border = Border(
                                color = ColorScheme(light = ColorInfo.Hex(expectedSelectedBorderColor.toArgb())),
                                width = expectedSelectedBorderWidth
                            ),
                            shadow = Shadow(
                                color = ColorScheme(light = ColorInfo.Hex(expectedSelectedShadowColor.toArgb())),
                                radius = 5.0,
                                x = 10.0,
                                y = 10.0,
                            ),
                        ),
                    ),
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
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(rcPackage),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val styleFactory = StyleFactory(
            localizations = localizations,
            uiConfig = UiConfig(),
            fontAliases = emptyMap(),
            offering = offering,
        )
        val style = styleFactory.create(component).getOrThrow() as PackageComponentStyle


        // Act
        setContent {
            // An outer box, because a shadow draws outside the Composable's bounds.
            Box(
                modifier = Modifier
                    .testTag(tag = "parent")
                    .requiredSize(parentSizeDp.toInt().dp)
                    .background(parentBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                // This PackageComponentView has a StackComponentView child.
                PackageComponentView(style = style, state = state, modifier = Modifier.testTag("pkg"))
            }
        }

        // Assert
        onNodeWithTag("pkg")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedUnselectedBorderWidth.dp,
                expectedBorderColor = expectedUnselectedBorderColor,
                expectedBackgroundColor = expectedUnselectedBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedUnselectedShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedSelectedShadowColor)

        // Select our package
        state.update(selectedPackage = rcPackage)

        onNodeWithTag("pkg")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedSelectedBorderWidth.dp,
                expectedBorderColor = expectedSelectedBorderColor,
                expectedBackgroundColor = expectedSelectedBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedSelectedShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedUnselectedShadowColor)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the intro offer overrides for the selected package`(): Unit = with(composeTestRule) {
        // Arrange
        val parentSizeDp = 200
        val stackSizeDp = 100
        val expectedIneligibleBorderColor = Color.Black
        val expectedIneligibleBorderWidth = 2.0
        val expectedIneligibleShadowColor = Color.Yellow
        val expectedIneligibleBackgroundColor = Color.Green

        val expectedSingleEligibleBorderColor = Color.Green
        val expectedSingleEligibleBorderWidth = 4.0
        val expectedSingleEligibleShadowColor = Color.Red
        val expectedSingleEligibleBackgroundColor = Color.Blue

        val expectedMultipleEligibleBorderColor = Color.Magenta
        val expectedMultipleEligibleBorderWidth = 6.0
        val expectedMultipleEligibleShadowColor = Color.Cyan
        val expectedMultipleEligibleBackgroundColor = Color.Black

        val parentBackgroundColor = Color.Magenta
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(stackSizeDp.toUInt()), Fixed(stackSizeDp.toUInt())),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedIneligibleBackgroundColor.toArgb())),
            border = Border(
                color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleBorderColor.toArgb())),
                width = expectedIneligibleBorderWidth
            ),
            shadow = Shadow(
                color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleShadowColor.toArgb())),
                radius = 5.0,
                x = 10.0,
                y = 10.0,
            ),
            overrides = ComponentOverrides(
                introOffer = PartialStackComponent(
                    backgroundColor = ColorScheme(
                        light = ColorInfo.Hex(expectedSingleEligibleBackgroundColor.toArgb())
                    ),
                    border = Border(
                        color = ColorScheme(light = ColorInfo.Hex(expectedSingleEligibleBorderColor.toArgb())),
                        width = expectedSingleEligibleBorderWidth
                    ),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(expectedSingleEligibleShadowColor.toArgb())),
                        radius = 5.0,
                        x = 10.0,
                        y = 10.0,
                    ),
                ),
                multipleIntroOffers = PartialStackComponent(
                    backgroundColor = ColorScheme(
                        light = ColorInfo.Hex(expectedMultipleEligibleBackgroundColor.toArgb())
                    ),
                    border = Border(
                        color = ColorScheme(light = ColorInfo.Hex(expectedMultipleEligibleBorderColor.toArgb())),
                        width = expectedMultipleEligibleBorderWidth
                    ),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(expectedMultipleEligibleShadowColor.toArgb())),
                        radius = 5.0,
                        x = 10.0,
                        y = 10.0,
                    ),
                ),
            )
        )
        val state = FakePaywallState(
            components = listOf(component),
            packages = listOf(
                packageWithoutIntroOffer,
                packageWithSingleIntroOffer,
                packageWithMultipleIntroOffers
            )
        )
        val style = styleFactory.create(component).getOrThrow() as StackComponentStyle

        // Act
        setContent {
            // An outer box, because a shadow draws outside the Composable's bounds.
            Box(
                modifier = Modifier
                    .testTag(tag = "parent")
                    .requiredSize(parentSizeDp.dp)
                    .background(parentBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { },
                    modifier = Modifier.testTag("stack")
                )
            }
        }

        // Assert
        state.update(selectedPackage = packageWithoutIntroOffer)
        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedIneligibleBorderWidth.dp,
                expectedBorderColor = expectedIneligibleBorderColor,
                expectedBackgroundColor = expectedIneligibleBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedIneligibleShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedSingleEligibleShadowColor)
            .assertNoPixelColorEquals(expectedMultipleEligibleShadowColor)

        state.update(selectedPackage = packageWithSingleIntroOffer)
        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedSingleEligibleBorderWidth.dp,
                expectedBorderColor = expectedSingleEligibleBorderColor,
                expectedBackgroundColor = expectedSingleEligibleBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedSingleEligibleShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedIneligibleShadowColor)
            .assertNoPixelColorEquals(expectedMultipleEligibleShadowColor)

        state.update(selectedPackage = packageWithMultipleIntroOffers)
        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedMultipleEligibleBorderWidth.dp,
                expectedBorderColor = expectedMultipleEligibleBorderColor,
                expectedBackgroundColor = expectedMultipleEligibleBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedMultipleEligibleShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedIneligibleShadowColor)
            .assertNoPixelColorEquals(expectedSingleEligibleShadowColor)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the intro offer overrides as child of PackageComponentView`(): Unit = with(composeTestRule) {
        // Arrange
        val parentSize = 100.dp
        val stackSize = DpSize(width = 50.dp, height = 50.dp)
        // For some reason, the PackageComponentView's node is measured to be as wide as its parent, even if the inner
        // stack isn't. This makes testing difficult, because we can only add a testTag to the PackageComponentView.
        // Fortunately we know exactly where the StackComponentView is going to be drawn, and so we can use that info
        // to assert the StackComponentView's border (used below).
        val stackOffsetInParent = DpOffset(
            x = (parentSize - stackSize.width) / 2,
            y = (parentSize - stackSize.height) / 2,
        )
        val expectedIneligibleBorderColor = Color.Black
        val expectedIneligibleBorderWidth = 2.0
        val expectedIneligibleShadowColor = Color.Yellow
        val expectedIneligibleBackgroundColor = Color.Green

        val expectedSingleEligibleBorderColor = Color.Green
        val expectedSingleEligibleBorderWidth = 4.0
        val expectedSingleEligibleShadowColor = Color.Red
        val expectedSingleEligibleBackgroundColor = Color.Blue

        val expectedMultipleEligibleBorderColor = Color.Magenta
        val expectedMultipleEligibleBorderWidth = 6.0
        val expectedMultipleEligibleShadowColor = Color.Cyan
        val expectedMultipleEligibleBackgroundColor = Color.Black

        val parentBackgroundColor = Color.Magenta
        val stackComponent = StackComponent(
            components = emptyList(),
            size = Size(Fixed(stackSize.width.value.toUInt()), Fixed(stackSize.height.value.toUInt())),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedIneligibleBackgroundColor.toArgb())),
            border = Border(
                color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleBorderColor.toArgb())),
                width = expectedIneligibleBorderWidth
            ),
            shadow = Shadow(
                color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleShadowColor.toArgb())),
                radius = 5.0,
                x = 10.0,
                y = 10.0,
            ),
            overrides = ComponentOverrides(
                introOffer = PartialStackComponent(
                    backgroundColor = ColorScheme(
                        light = ColorInfo.Hex(expectedSingleEligibleBackgroundColor.toArgb())
                    ),
                    border = Border(
                        color = ColorScheme(light = ColorInfo.Hex(expectedSingleEligibleBorderColor.toArgb())),
                        width = expectedSingleEligibleBorderWidth
                    ),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(expectedSingleEligibleShadowColor.toArgb())),
                        radius = 5.0,
                        x = 10.0,
                        y = 10.0,
                    ),
                ),
                multipleIntroOffers = PartialStackComponent(
                    backgroundColor = ColorScheme(
                        light = ColorInfo.Hex(expectedMultipleEligibleBackgroundColor.toArgb())
                    ),
                    border = Border(
                        color = ColorScheme(light = ColorInfo.Hex(expectedMultipleEligibleBorderColor.toArgb())),
                        width = expectedMultipleEligibleBorderWidth
                    ),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(expectedMultipleEligibleShadowColor.toArgb())),
                        radius = 5.0,
                        x = 10.0,
                        y = 10.0,
                    ),
                ),
            )
        )
        val noIntroOfferPackageComponent = PackageComponent(
            packageId = packageWithoutIntroOffer.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(stackComponent)),
        )
        val singleIntroOfferPackageComponent = PackageComponent(
            packageId = packageWithSingleIntroOffer.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(stackComponent)),
        )
        val multipleIntroOffersPackageComponent = PackageComponent(
            packageId = packageWithMultipleIntroOffers.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(stackComponent)),
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
            identifier = "offering-id",
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
            uiConfig = UiConfig(),
            fontAliases = emptyMap(),
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
            Column(
                verticalArrangement = Arrangement.spacedBy(50.dp)
            ) {
                // An outer box, because a shadow draws outside the Composable's bounds.
                Box(
                    modifier = Modifier
                        .testTag(tag = "noIntroOfferParent")
                        .requiredSize(parentSize)
                        .background(parentBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    PackageComponentView(style = noIntroOfferPackageComponentStyle, state = state)
                }
                Box(
                    modifier = Modifier
                        .testTag(tag = "singleIntroOfferParent")
                        .requiredSize(parentSize)
                        .background(parentBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    PackageComponentView(style = singleIntroOfferPackageComponentStyle, state = state)
                }
                Box(
                    modifier = Modifier
                        .testTag(tag = "multipleIntroOffersParent")
                        .requiredSize(parentSize)
                        .background(parentBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    PackageComponentView(style = multipleIntroOffersPackageComponentStyle, state = state)
                }
            }
        }

        // Assert
        fun assertAll() {
            onNodeWithTag("noIntroOfferParent")
                .assertIsDisplayed()
                .assertRectangularBorderColor(
                    borderWidth = expectedIneligibleBorderWidth.dp,
                    expectedBorderColor = expectedIneligibleBorderColor,
                    expectedBackgroundColor = expectedIneligibleBackgroundColor,
                    size = stackSize,
                    offset = stackOffsetInParent,
                )
                .assertPixelColorPercentage(expectedIneligibleShadowColor) { it > 0f }
                .assertNoPixelColorEquals(expectedSingleEligibleShadowColor)
                .assertNoPixelColorEquals(expectedMultipleEligibleShadowColor)

            onNodeWithTag("singleIntroOfferParent")
                .assertIsDisplayed()
                .assertRectangularBorderColor(
                    borderWidth = expectedSingleEligibleBorderWidth.dp,
                    expectedBorderColor = expectedSingleEligibleBorderColor,
                    expectedBackgroundColor = expectedSingleEligibleBackgroundColor,
                    size = stackSize,
                    offset = stackOffsetInParent,
                )
                .assertPixelColorPercentage(expectedSingleEligibleShadowColor) { it > 0f }
                .assertNoPixelColorEquals(expectedIneligibleShadowColor)
                .assertNoPixelColorEquals(expectedMultipleEligibleShadowColor)

            onNodeWithTag("multipleIntroOffersParent")
                .assertIsDisplayed()
                .assertRectangularBorderColor(
                    borderWidth = expectedMultipleEligibleBorderWidth.dp,
                    expectedBorderColor = expectedMultipleEligibleBorderColor,
                    expectedBackgroundColor = expectedMultipleEligibleBackgroundColor,
                    size = stackSize,
                    offset = stackOffsetInParent,
                )
                .assertPixelColorPercentage(expectedMultipleEligibleShadowColor) { it > 0f }
                .assertNoPixelColorEquals(expectedIneligibleShadowColor)
                .assertNoPixelColorEquals(expectedSingleEligibleShadowColor)
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
}
