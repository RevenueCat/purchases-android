package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialCarouselComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponent
import com.revenuecat.purchases.paywalls.components.PartialVideoComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.paywalls.components.properties.Size as ComponentSize
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed as FixedSize
import com.revenuecat.purchases.ui.revenuecatui.components.carousel.CarouselComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.pkg.PackageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TimelineComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.VideoComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.tabs.TabsComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.timeline.TimelineComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

/**
 * Consolidated visibility and styling condition tests for all component types.
 * Covers carousel, timeline, video, text, stack, footer, and tabs components,
 * plus cross-component scenarios (sibling independence, color-only override,
 * full paywall footer+body interaction, click interactions).
 */
@RunWith(AndroidJUnit4::class)
class VisibilityConditionTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val localeId = LocaleId("en_US")
    private val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))

    private val pageTextKey = LocalizationKey("page_text")
    private val pageTextValue = "Carousel page content"
    private val timelineTitleKey = LocalizationKey("timeline_title")
    private val timelineTitleValue = "Timeline step"
    private val textKey = LocalizationKey("main_text")
    private val textValue = "Visible text"
    private val text2Key = LocalizationKey("second_text")
    private val text2Value = "Second text"
    private val childTextKey = LocalizationKey("child_text")
    private val childTextValue = "Child in stack"
    private val bodyTextKey = LocalizationKey("body_text")
    private val bodyTextValue = "Body text"
    private val footerTextKey = LocalizationKey("footer_text")
    private val footerTextValue = "Footer text"
    private val monthlyLabelKey = LocalizationKey("monthly_label")
    private val monthlyLabelValue = "Monthly Plan"
    private val annualLabelKey = LocalizationKey("annual_label")
    private val annualLabelValue = "Annual Plan"

    private val localizations = nonEmptyMapOf(
        localeId to nonEmptyMapOf(
            pageTextKey to LocalizationData.Text(pageTextValue),
            timelineTitleKey to LocalizationData.Text(timelineTitleValue),
            textKey to LocalizationData.Text(textValue),
            text2Key to LocalizationData.Text(text2Value),
            childTextKey to LocalizationData.Text(childTextValue),
            bodyTextKey to LocalizationData.Text(bodyTextValue),
            footerTextKey to LocalizationData.Text(footerTextValue),
            monthlyLabelKey to LocalizationData.Text(monthlyLabelValue),
            annualLabelKey to LocalizationData.Text(annualLabelValue),
        )
    )

    private val styleFactory = StyleFactory(localizations = localizations)

    // region Carousel

    /**
     * Carousel hidden by variable condition — page content should not appear in the tree.
     */
    @Test
    fun `Carousel hidden by variable condition`(): Unit = with(composeTestRule) {
        val carousel = CarouselComponent(
            pages = listOf(
                StackComponent(
                    components = listOf(
                        TextComponent(text = pageTextKey, color = textColor),
                    ),
                ),
            ),
            pageAlignment = VerticalAlignment.CENTER,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_carousel",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialCarouselComponent(visible = false),
                ),
            ),
        )

        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(carousel),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("hide_carousel" to CustomVariableValue.Boolean(true)),
        )
        val style = styleFactory.create(carousel).getOrThrow().componentStyle as CarouselComponentStyle

        setContent {
            CarouselComponentView(style = style, state = state, clickHandler = { })
        }

        // Carousel is hidden — page text should not exist
        onNodeWithText(pageTextValue).assertDoesNotExist()
    }

    /**
     * Carousel visible when variable condition not met — page content should appear.
     */
    @Test
    fun `Carousel visible when variable condition not met`(): Unit = with(composeTestRule) {
        val carousel = CarouselComponent(
            pages = listOf(
                StackComponent(
                    components = listOf(
                        TextComponent(text = pageTextKey, color = textColor),
                    ),
                ),
            ),
            pageAlignment = VerticalAlignment.CENTER,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_carousel",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialCarouselComponent(visible = false),
                ),
            ),
        )

        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(carousel),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("hide_carousel" to CustomVariableValue.Boolean(false)),
        )
        val style = styleFactory.create(carousel).getOrThrow().componentStyle as CarouselComponentStyle

        setContent {
            CarouselComponentView(style = style, state = state, clickHandler = { })
        }

        // Carousel is visible — page text should be displayed
        onNodeWithText(pageTextValue).assertIsDisplayed()
    }

    // endregion

    // region Timeline

    /**
     * Timeline hidden by variable condition — item titles should not appear.
     */
    @Test
    fun `Timeline hidden by variable condition`(): Unit = with(composeTestRule) {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 12,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            items = listOf(
                TimelineComponent.Item(
                    title = TextComponent(text = timelineTitleKey, color = textColor),
                    icon = IconComponent(
                        baseUrl = "https://assets.example.com",
                        iconName = "check",
                        formats = IconComponent.Formats(webp = "check.webp"),
                        size = Size(
                            width = SizeConstraint.Fixed(24u),
                            height = SizeConstraint.Fixed(24u),
                        ),
                    ),
                ),
            ),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_timeline",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialTimelineComponent(visible = false),
                ),
            ),
        )

        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(timeline),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("hide_timeline" to CustomVariableValue.Boolean(true)),
        )
        val style = styleFactory.create(timeline).getOrThrow().componentStyle as TimelineComponentStyle

        setContent {
            TimelineComponentView(style = style, state = state)
        }

        // Timeline is hidden — item title should not exist
        onNodeWithText(timelineTitleValue).assertDoesNotExist()
    }

    // endregion

    // region Video (state level)

    /**
     * Video component with condition override validates and creates style correctly.
     * Tested at the state/style level because VideoComponentView requires a FileRepository
     * at call time even when the video is hidden.
     */
    @Test
    fun `Video component with condition override validates and creates style`() {
        val videoComponent = VideoComponent(
            source = ThemeVideoUrls(
                light = VideoUrls(
                    width = 640u,
                    height = 480u,
                    url = URL("https://example.com/video.mp4"),
                ),
                dark = null,
            ),
            fallbackSource = null,
            visible = true,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(200u)),
            fitMode = FitMode.FILL,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_video",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialVideoComponent(visible = false),
                ),
            ),
        )

        // State creation should succeed (component validation passes)
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(videoComponent),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("hide_video" to CustomVariableValue.Boolean(true)),
        )
        assertNotNull(state.stack)

        // Style creation should also succeed
        val style = styleFactory.create(videoComponent).getOrThrow().componentStyle
        assertTrue(style is VideoComponentStyle)
    }

    // endregion

    // region selected_package conditions for Stack, Carousel, Timeline

    /**
     * Stack hidden by selected_package condition — selecting monthly hides the stack and its children.
     */
    @Test
    fun `Stack hidden by selected_package condition`(): Unit = with(composeTestRule) {
        val stack = StackComponent(
            components = listOf(
                TextComponent(text = childTextKey, color = textColor),
            ),
            size = Size(width = SizeConstraint.Fixed(200u), height = SizeConstraint.Fixed(100u)),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialStackComponent(visible = false),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(stack),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )
        val style = styleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        setContent {
            StackComponentView(style = style, state = state, clickHandler = { })
        }

        // Select monthly → stack hidden
        state.update(TestData.Packages.monthly.identifier)
        onNodeWithText(childTextValue).assertDoesNotExist()

        // Select annual → stack visible
        state.update(TestData.Packages.annual.identifier)
        onNodeWithText(childTextValue).assertIsDisplayed()
    }

    /**
     * Carousel hidden by selected_package condition — selecting monthly hides carousel pages.
     */
    @Test
    fun `Carousel hidden by selected_package condition`(): Unit = with(composeTestRule) {
        val carousel = CarouselComponent(
            pages = listOf(
                StackComponent(
                    components = listOf(
                        TextComponent(text = pageTextKey, color = textColor),
                    ),
                ),
            ),
            pageAlignment = VerticalAlignment.CENTER,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialCarouselComponent(visible = false),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(carousel),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )
        val style = styleFactory.create(carousel).getOrThrow().componentStyle as CarouselComponentStyle

        setContent {
            CarouselComponentView(style = style, state = state, clickHandler = { })
        }

        // Select monthly → carousel hidden
        state.update(TestData.Packages.monthly.identifier)
        onNodeWithText(pageTextValue).assertDoesNotExist()

        // Select annual → carousel visible
        state.update(TestData.Packages.annual.identifier)
        onNodeWithText(pageTextValue).assertIsDisplayed()
    }

    /**
     * Timeline hidden by selected_package condition — selecting monthly hides timeline items.
     */
    @Test
    fun `Timeline hidden by selected_package condition`(): Unit = with(composeTestRule) {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 12,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            items = listOf(
                TimelineComponent.Item(
                    title = TextComponent(text = timelineTitleKey, color = textColor),
                    icon = IconComponent(
                        baseUrl = "https://assets.example.com",
                        iconName = "check",
                        formats = IconComponent.Formats(webp = "check.webp"),
                        size = Size(
                            width = SizeConstraint.Fixed(24u),
                            height = SizeConstraint.Fixed(24u),
                        ),
                    ),
                ),
            ),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialTimelineComponent(visible = false),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(timeline),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )
        val style = styleFactory.create(timeline).getOrThrow().componentStyle as TimelineComponentStyle

        setContent {
            TimelineComponentView(style = style, state = state)
        }

        // Select monthly → timeline hidden
        state.update(TestData.Packages.monthly.identifier)
        onNodeWithText(timelineTitleValue).assertDoesNotExist()
    }

    // endregion

    // region Click interaction

    /**
     * Clicking a PackageComponent hides a sibling text via selected_package condition.
     * Uses performClick() to simulate real user interaction.
     */
    @Test
    fun `Clicking package hides sibling text via selected_package condition`(): Unit = with(composeTestRule) {
        val conditionedText = TextComponent(
            text = textKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val monthlyPkg = PackageComponent(
            packageId = TestData.Packages.monthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(TextComponent(text = monthlyLabelKey, color = textColor)),
            ),
        )
        val annualPkg = PackageComponent(
            packageId = TestData.Packages.annual.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(TextComponent(text = annualLabelKey, color = textColor)),
            ),
        )

        val data = PaywallComponentsData(
            id = "click_test",
            templateName = "components",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(conditionedText, monthlyPkg, annualPkg),
                    ),
                    background = Background.Color(
                        ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                    ),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "click-test",
            serverDescription = "Click interaction test",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(localizations = localizations, offering = offering)
        val textStyle = styleFactory.create(conditionedText).getOrThrow().componentStyle as TextComponentStyle
        val monthlyStyle = styleFactory.create(monthlyPkg).getOrThrow().componentStyle as PackageComponentStyle
        val annualStyle = styleFactory.create(annualPkg).getOrThrow().componentStyle as PackageComponentStyle

        setContent {
            Column {
                TextComponentView(style = textStyle, state = state)
                PackageComponentView(
                    style = monthlyStyle,
                    state = state,
                    clickHandler = { },
                    modifier = Modifier.testTag("monthly"),
                )
                PackageComponentView(
                    style = annualStyle,
                    state = state,
                    clickHandler = { },
                    modifier = Modifier.testTag("annual"),
                )
            }
        }

        // Initially no package selected — text visible
        onNodeWithText(textValue).assertIsDisplayed()

        // Click monthly → text hidden
        onNodeWithTag("monthly").assertHasClickAction().performClick()
        onNodeWithText(textValue).assertDoesNotExist()

        // Click annual → text visible again
        onNodeWithTag("annual").assertHasClickAction().performClick()
        onNodeWithText(textValue).assertIsDisplayed()
    }

    /**
     * Clicking a PackageComponent hides a sibling stack via selected_package condition.
     * Verifies that the stack and its children disappear after a click.
     */
    @Test
    fun `Clicking package hides sibling stack via selected_package condition`(): Unit = with(composeTestRule) {
        val conditionedStack = StackComponent(
            components = listOf(
                TextComponent(text = childTextKey, color = textColor),
            ),
            size = Size(width = SizeConstraint.Fixed(200u), height = SizeConstraint.Fixed(100u)),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialStackComponent(visible = false),
                ),
            ),
        )
        val monthlyPkg = PackageComponent(
            packageId = TestData.Packages.monthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(TextComponent(text = monthlyLabelKey, color = textColor)),
            ),
        )
        val annualPkg = PackageComponent(
            packageId = TestData.Packages.annual.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(
                components = listOf(TextComponent(text = annualLabelKey, color = textColor)),
            ),
        )

        val data = PaywallComponentsData(
            id = "click_stack_test",
            templateName = "components",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(conditionedStack, monthlyPkg, annualPkg),
                    ),
                    background = Background.Color(
                        ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                    ),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "click-stack-test",
            serverDescription = "Click stack test",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)

        val styleFactory = StyleFactory(localizations = localizations, offering = offering)
        val stackStyle = styleFactory.create(conditionedStack).getOrThrow().componentStyle as StackComponentStyle
        val monthlyStyle = styleFactory.create(monthlyPkg).getOrThrow().componentStyle as PackageComponentStyle
        val annualStyle = styleFactory.create(annualPkg).getOrThrow().componentStyle as PackageComponentStyle

        setContent {
            Column {
                StackComponentView(style = stackStyle, state = state, clickHandler = { })
                PackageComponentView(
                    style = monthlyStyle,
                    state = state,
                    clickHandler = { },
                    modifier = Modifier.testTag("monthly"),
                )
                PackageComponentView(
                    style = annualStyle,
                    state = state,
                    clickHandler = { },
                    modifier = Modifier.testTag("annual"),
                )
            }
        }

        // Initially no package selected — stack child visible
        onNodeWithText(childTextValue).assertIsDisplayed()

        // Click monthly → stack hidden
        onNodeWithTag("monthly").assertHasClickAction().performClick()
        onNodeWithText(childTextValue).assertDoesNotExist()

        // Click annual → stack visible again
        onNodeWithTag("annual").assertHasClickAction().performClick()
        onNodeWithText(childTextValue).assertIsDisplayed()
    }

    // endregion

    // region Sibling independence

    /**
     * Two sibling text components with different variable conditions act independently.
     * Only the one whose condition matches should be affected.
     */
    @Test
    fun `Sibling texts with independent conditions act independently`(): Unit = with(composeTestRule) {
        // Text 1: hidden when feature_a = true
        val text1 = TextComponent(
            text = textKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "feature_a",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        // Text 2: hidden when feature_b = true
        val text2 = TextComponent(
            text = text2Key,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "feature_b",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )

        // Only feature_a is true — text1 should be hidden, text2 should be visible
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(text1, text2),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf(
                "feature_a" to CustomVariableValue.Boolean(true),
                "feature_b" to CustomVariableValue.Boolean(false),
            ),
        )
        val style1 = styleFactory.create(text1).getOrThrow().componentStyle as TextComponentStyle
        val style2 = styleFactory.create(text2).getOrThrow().componentStyle as TextComponentStyle

        setContent {
            TextComponentView(style = style1, state = state)
            TextComponentView(style = style2, state = state)
        }

        // Text 1 hidden (feature_a matches), text 2 visible (feature_b doesn't match)
        onNodeWithText(textValue).assertDoesNotExist()
        onNodeWithText(text2Value).assertIsDisplayed()
    }

    // endregion

    // region Color override

    /**
     * Variable condition override changes text color without affecting visibility.
     * The text should remain displayed when the condition matches a color-only override.
     */
    @Test
    fun `Variable condition changes color without affecting visibility`(): Unit = with(composeTestRule) {
        val component = TextComponent(
            text = textKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "theme",
                            value = JsonPrimitive("holiday"),
                        ),
                    ),
                    properties = PartialTextComponent(
                        color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                    ),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("theme" to CustomVariableValue.String("holiday")),
        )
        val style = styleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = style, state = state) }

        // Override only changed color — text remains visible
        onNodeWithText(textValue).assertIsDisplayed()
    }

    // endregion

    // region Full paywall footer + body cross-interaction

    /**
     * Body text visibility responds to package selection while the footer remains present.
     * Uses LoadedPaywallComponents to exercise the full rendering pipeline.
     */
    @Test
    fun `Body text visibility responds to package selection while footer is present`(): Unit =
        with(composeTestRule) {
            val bodyText = TextComponent(
                text = bodyTextKey,
                color = textColor,
                overrides = listOf(
                    ComponentOverride(
                        conditions = listOf(
                            ComponentOverride.Condition.SelectedPackage(
                                operator = ComponentOverride.ArrayOperator.IN,
                                packages = listOf(TestData.Packages.monthly.identifier),
                            ),
                        ),
                        properties = PartialTextComponent(visible = false),
                    ),
                ),
            )
            val footerText = TextComponent(text = footerTextKey, color = textColor)

            val monthlyPkg = PackageComponent(
                packageId = TestData.Packages.monthly.identifier,
                isSelectedByDefault = false,
                stack = StackComponent(components = emptyList()),
            )
            val annualPkg = PackageComponent(
                packageId = TestData.Packages.annual.identifier,
                isSelectedByDefault = false,
                stack = StackComponent(components = emptyList()),
            )

            val data = PaywallComponentsData(
                id = "cross_interaction_paywall",
                templateName = "components",
                assetBaseURL = URL("https://assets.pawwalls.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = StackComponent(
                            components = listOf(bodyText, monthlyPkg, annualPkg),
                        ),
                        background = Background.Color(
                            ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                        ),
                        stickyFooter = StickyFooterComponent(
                            stack = StackComponent(components = listOf(footerText)),
                        ),
                    ),
                ),
                componentsLocalizations = localizations,
                defaultLocaleIdentifier = localeId,
            )
            val offering = Offering(
                identifier = "cross-interaction",
                serverDescription = "Cross interaction test",
                metadata = emptyMap(),
                availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
                paywallComponents = Offering.PaywallComponents(UiConfig(), data),
            )
            val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
            val state = offering.toComponentsPaywallState(validated)

            setContent {
                LoadedPaywallComponents(state = state, clickHandler = { })
            }

            // Select annual — body text visible, footer visible
            state.update(TestData.Packages.annual.identifier)
            onNodeWithText(bodyTextValue).assertIsDisplayed()
            onNodeWithText(footerTextValue).assertIsDisplayed()

            // Select monthly — body text hidden, footer still visible
            state.update(TestData.Packages.monthly.identifier)
            onNodeWithText(bodyTextValue).assertDoesNotExist()
            onNodeWithText(footerTextValue).assertIsDisplayed()
        }

    // endregion

    // region Text visibility

    /**
     * Text hidden by selected_package condition.
     * When monthly is selected, text should be hidden. When annual is selected, text should be visible.
     */
    @Test
    fun `Text hidden by selected_package condition`(): Unit = with(composeTestRule) {
        val component = TextComponent(
            text = textKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )
        val style = styleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = style, state = state) }

        // Select monthly → text hidden
        state.update(TestData.Packages.monthly.identifier)
        onNodeWithText(textValue).assertDoesNotExist()

        // Select annual → text visible
        state.update(TestData.Packages.annual.identifier)
        onNodeWithText(textValue).assertIsDisplayed()
    }

    /**
     * Text hidden by default (visible=false), shown by variable condition override.
     */
    @Test
    fun `Text hidden by default shown by variable condition`(): Unit = with(composeTestRule) {
        val component = TextComponent(
            text = textKey,
            color = textColor,
            visible = false,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "plan",
                            value = JsonPrimitive("premium"),
                        ),
                    ),
                    properties = PartialTextComponent(visible = true),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("plan" to CustomVariableValue.String("premium")),
        )
        val style = styleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = style, state = state) }

        // Variable matches, so override sets visible=true
        onNodeWithText(textValue).assertIsDisplayed()
    }

    /**
     * Text hidden by default stays hidden when variable condition does not match.
     */
    @Test
    fun `Text hidden by default stays hidden when condition not met`(): Unit = with(composeTestRule) {
        val component = TextComponent(
            text = textKey,
            color = textColor,
            visible = false,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "plan",
                            value = JsonPrimitive("premium"),
                        ),
                    ),
                    properties = PartialTextComponent(visible = true),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("plan" to CustomVariableValue.String("free")),
        )
        val style = styleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = style, state = state) }

        // Variable doesn't match, so text stays hidden
        onNodeWithText(textValue).assertDoesNotExist()
    }

    /**
     * Same variable condition used for both text replacement AND visibility.
     * When plan=premium, text changes to premium text AND becomes visible.
     */
    @Test
    fun `Variable condition controls both text content and visibility`(): Unit = with(composeTestRule) {
        val premiumTextKey = LocalizationKey("premium_text")
        val premiumTextValue = "Premium content"
        val premiumLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                textKey to LocalizationData.Text(textValue),
                premiumTextKey to LocalizationData.Text(premiumTextValue),
            )
        )
        val premiumStyleFactory = StyleFactory(localizations = premiumLocalizations)

        val component = TextComponent(
            text = textKey,
            color = textColor,
            visible = false,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "plan",
                            value = JsonPrimitive("premium"),
                        ),
                    ),
                    properties = PartialTextComponent(
                        visible = true,
                        text = premiumTextKey,
                    ),
                ),
            ),
        )
        val state = FakePaywallState(
            localizations = premiumLocalizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("plan" to CustomVariableValue.String("premium")),
        )
        val style = premiumStyleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = style, state = state) }

        // Condition matches: text changes to premium AND becomes visible
        onNodeWithText(textValue).assertDoesNotExist()
        onNodeWithText(premiumTextValue).assertIsDisplayed()
    }

    /**
     * Text visible by default, hidden by variable condition with NOT_EQUALS.
     */
    @Test
    fun `Text hidden when variable not_equals condition matches`(): Unit = with(composeTestRule) {
        val component = TextComponent(
            text = textKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.NOT_EQUALS,
                            variable = "plan",
                            value = JsonPrimitive("premium"),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val stateHidden = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("plan" to CustomVariableValue.String("free")),
        )
        val style = styleFactory.create(component).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = style, state = stateHidden) }

        // plan is "free" != "premium", so override hides text
        onNodeWithText(textValue).assertDoesNotExist()
    }

    /**
     * Text inside a PackageComponent hidden by intro offer condition.
     */
    @Test
    fun `Text hidden by intro offer condition inside PackageComponent`(): Unit = with(composeTestRule) {
        val packageWithoutIntroOffer = TestData.Packages.monthly
        val packageWithSingleIntroOffer = TestData.Packages.annual
        val textComponent = TextComponent(
            text = textKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.IntroOffer()),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val noIntroPackageComponent = PackageComponent(
            packageId = packageWithoutIntroOffer.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(textComponent)),
        )
        val introPackageComponent = PackageComponent(
            packageId = packageWithSingleIntroOffer.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(textComponent)),
        )
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(noIntroPackageComponent, introPackageComponent),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(packageWithoutIntroOffer, packageWithSingleIntroOffer),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val introStyleFactory = StyleFactory(localizations = localizations, offering = offering)
        val noIntroStyle = introStyleFactory.create(noIntroPackageComponent).getOrThrow().componentStyle
        val introStyle = introStyleFactory.create(introPackageComponent).getOrThrow().componentStyle

        setContent {
            PackageComponentView(
                style = noIntroStyle as PackageComponentStyle,
                state = state,
                clickHandler = { },
            )
            PackageComponentView(
                style = introStyle as PackageComponentStyle,
                state = state,
                clickHandler = { },
            )
        }

        // The no-intro package shows text, the intro-offer package hides it.
        // Since both render the same text, we check at least one exists (from no-intro package).
        onNodeWithText(textValue).assertExists()
    }

    // endregion

    // region Stack visibility

    /**
     * Stack with variable condition visible=false should hide itself and all children.
     */
    @Test
    fun `Stack hidden by variable condition hides all children`(): Unit = with(composeTestRule) {
        val stackChildTextKey = LocalizationKey("child_text")
        val stackChildTextValue = "Hello from child"
        val stackLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                stackChildTextKey to LocalizationData.Text(stackChildTextValue),
            )
        )

        val childText = TextComponent(
            text = stackChildTextKey,
            color = textColor,
        )
        val stack = StackComponent(
            components = listOf(childText),
            size = Size(SizeConstraint.Fixed(200u), SizeConstraint.Fixed(200u)),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_stack",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialStackComponent(visible = false),
                ),
            ),
        )
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(stack)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = stackLocalizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(
            validated,
            customVariables = mapOf("hide_stack" to CustomVariableValue.Boolean(true)),
        )
        val stackStyleFactory = StyleFactory(localizations = stackLocalizations, offering = offering)
        val style = stackStyleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        setContent {
            StackComponentView(
                style = style,
                state = state,
                clickHandler = { },
                modifier = Modifier.testTag("stack"),
            )
        }

        // Stack is hidden, so the child text should not exist in the tree
        onNodeWithText(stackChildTextValue).assertDoesNotExist()
    }

    /**
     * When the condition is NOT met, stack and children should be visible.
     */
    @Test
    fun `Stack visible when variable condition not met`(): Unit = with(composeTestRule) {
        val stackChildTextKey = LocalizationKey("child_text")
        val stackChildTextValue = "Hello from child"
        val stackLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                stackChildTextKey to LocalizationData.Text(stackChildTextValue),
            )
        )

        val childText = TextComponent(
            text = stackChildTextKey,
            color = textColor,
        )
        val stack = StackComponent(
            components = listOf(childText),
            size = Size(SizeConstraint.Fixed(200u), SizeConstraint.Fixed(200u)),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_stack",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialStackComponent(visible = false),
                ),
            ),
        )
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(stack)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = stackLocalizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(
            validated,
            customVariables = mapOf("hide_stack" to CustomVariableValue.Boolean(false)),
        )
        val stackStyleFactory = StyleFactory(localizations = stackLocalizations, offering = offering)
        val style = stackStyleFactory.create(stack).getOrThrow().componentStyle as StackComponentStyle

        setContent {
            StackComponentView(
                style = style,
                state = state,
                clickHandler = { },
                modifier = Modifier.testTag("stack"),
            )
        }

        // Stack is visible, so child text should be displayed
        onNodeWithTag("stack").assertIsDisplayed()
        onNodeWithText(stackChildTextValue).assertIsDisplayed()
    }

    /**
     * Parent stack hidden → child text should also be hidden,
     * even if the child has its own override setting visible=true.
     */
    @Test
    fun `Parent stack hidden overrides child visible override`(): Unit = with(composeTestRule) {
        val stackChildTextKey = LocalizationKey("child_text")
        val stackChildTextValue = "Hello from child"
        val stackLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                stackChildTextKey to LocalizationData.Text(stackChildTextValue),
            )
        )

        val childText = TextComponent(
            text = stackChildTextKey,
            color = textColor,
            visible = false,
        )
        val parentStack = StackComponent(
            components = listOf(childText),
            size = Size(SizeConstraint.Fixed(200u), SizeConstraint.Fixed(200u)),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_all",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialStackComponent(visible = false),
                ),
            ),
        )
        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(parentStack)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = stackLocalizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(
            validated,
            customVariables = mapOf("hide_all" to CustomVariableValue.Boolean(true)),
        )
        val stackStyleFactory = StyleFactory(localizations = stackLocalizations, offering = offering)
        val style = stackStyleFactory.create(parentStack).getOrThrow().componentStyle as StackComponentStyle

        setContent {
            StackComponentView(
                style = style,
                state = state,
                clickHandler = { },
                modifier = Modifier.testTag("parentStack"),
            )
        }

        // Parent is hidden so nothing should be in the tree
        onNodeWithText(stackChildTextValue).assertDoesNotExist()
    }

    // endregion

    // region Footer conditions

    /**
     * Text inside sticky footer hidden by variable condition.
     */
    @Test
    fun `Text inside footer hidden by variable condition`(): Unit = with(composeTestRule) {
        val footerLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                footerTextKey to LocalizationData.Text(footerTextValue),
                LocalizationKey("dummyKey") to LocalizationData.Text("dummy"),
            )
        )

        val footerText = TextComponent(
            text = footerTextKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_footer_text",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val stickyFooter = StickyFooterComponent(
            stack = StackComponent(components = listOf(footerText)),
        )
        val state = FakePaywallState(
            localizations = footerLocalizations,
            defaultLocaleIdentifier = localeId,
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("hide_footer_text" to CustomVariableValue.Boolean(true)),
            stickyFooter = stickyFooter,
        )
        val footerStyleFactory = StyleFactory(localizations = footerLocalizations)
        val footerTextStyle = footerStyleFactory.create(footerText).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = footerTextStyle, state = state) }

        // Footer text hidden by variable condition
        onNodeWithText(footerTextValue).assertDoesNotExist()
    }

    /**
     * Text inside footer visible when variable condition not met.
     */
    @Test
    fun `Text inside footer visible when condition not met`(): Unit = with(composeTestRule) {
        val footerLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                footerTextKey to LocalizationData.Text(footerTextValue),
                LocalizationKey("dummyKey") to LocalizationData.Text("dummy"),
            )
        )

        val footerText = TextComponent(
            text = footerTextKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_footer_text",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val stickyFooter = StickyFooterComponent(
            stack = StackComponent(components = listOf(footerText)),
        )
        val state = FakePaywallState(
            localizations = footerLocalizations,
            defaultLocaleIdentifier = localeId,
            packages = listOf(TestData.Packages.monthly),
            customVariables = mapOf("hide_footer_text" to CustomVariableValue.Boolean(false)),
            stickyFooter = stickyFooter,
        )
        val footerStyleFactory = StyleFactory(localizations = footerLocalizations)
        val footerTextStyle = footerStyleFactory.create(footerText).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = footerTextStyle, state = state) }

        // Footer text visible
        onNodeWithText(footerTextValue).assertIsDisplayed()
    }

    /**
     * selected_package condition on text inside footer.
     */
    @Test
    fun `Text inside footer hidden by selected_package condition`(): Unit = with(composeTestRule) {
        val footerLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                footerTextKey to LocalizationData.Text(footerTextValue),
                LocalizationKey("dummyKey") to LocalizationData.Text("dummy"),
            )
        )

        val footerText = TextComponent(
            text = footerTextKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val stickyFooter = StickyFooterComponent(
            stack = StackComponent(components = listOf(footerText)),
        )
        val state = FakePaywallState(
            localizations = footerLocalizations,
            defaultLocaleIdentifier = localeId,
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            stickyFooter = stickyFooter,
        )
        val footerStyleFactory = StyleFactory(localizations = footerLocalizations)
        val footerTextStyle = footerStyleFactory.create(footerText).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = footerTextStyle, state = state) }

        // Select monthly → footer text hidden
        state.update(TestData.Packages.monthly.identifier)
        onNodeWithText(footerTextValue).assertDoesNotExist()

        // Select annual → footer text visible
        state.update(TestData.Packages.annual.identifier)
        onNodeWithText(footerTextValue).assertIsDisplayed()
    }

    /**
     * Package selector in footer + conditioned text in the main body.
     * Selecting a package via the shared state should affect the body text visibility.
     */
    @Test
    fun `Package selection affects body text visibility with footer`(): Unit = with(composeTestRule) {
        val footerBodyLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                bodyTextKey to LocalizationData.Text(bodyTextValue),
                footerTextKey to LocalizationData.Text(footerTextValue),
                LocalizationKey("dummyKey") to LocalizationData.Text("dummy"),
            )
        )

        val bodyText = TextComponent(
            text = bodyTextKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val monthlyPackageComponent = PackageComponent(
            packageId = TestData.Packages.monthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(components = listOf(
                TextComponent(text = footerTextKey, color = textColor),
            )),
        )
        val stickyFooter = StickyFooterComponent(
            stack = StackComponent(components = listOf(monthlyPackageComponent)),
        )
        val state = FakePaywallState(
            localizations = footerBodyLocalizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(bodyText),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            stickyFooter = stickyFooter,
        )
        val footerBodyStyleFactory = StyleFactory(localizations = footerBodyLocalizations)
        val bodyTextStyle = footerBodyStyleFactory.create(bodyText).getOrThrow().componentStyle as TextComponentStyle

        setContent { TextComponentView(style = bodyTextStyle, state = state) }

        // Select annual → body text visible
        state.update(TestData.Packages.annual.identifier)
        onNodeWithText(bodyTextValue).assertIsDisplayed()

        // Select monthly → body text hidden
        state.update(TestData.Packages.monthly.identifier)
        onNodeWithText(bodyTextValue).assertDoesNotExist()
    }

    // endregion

    // region Tabs conditions

    /**
     * Text inside a tab hidden by a Variable condition.
     */
    @Test
    fun `Text inside tab hidden by variable condition`(): Unit = with(composeTestRule) {
        val visibleTextKey = LocalizationKey("visible_text")
        val visibleTextValue = "I am visible"
        val hiddenTextKey = LocalizationKey("hidden_text")
        val hiddenTextValue = "I should be hidden"

        val tabControlButtons = listOf(0, 1).map { index ->
            val key = LocalizationKey("tab_$index")
            TabControlButtonComponent(
                tabIndex = index,
                tabId = "$index",
                stack = StackComponent(
                    components = listOf(TextComponent(text = key, color = textColor)),
                ),
            )
        }

        val hiddenTextComponent = TextComponent(
            text = hiddenTextKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "hide_promo",
                            value = JsonPrimitive(true),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val visibleTextComponent = TextComponent(text = visibleTextKey, color = textColor)

        val tabsComponent = TabsComponent(
            tabs = listOf(
                TabsComponent.Tab(
                    id = "0",
                    stack = StackComponent(components = listOf(TabControlComponent, hiddenTextComponent)),
                ),
                TabsComponent.Tab(
                    id = "1",
                    stack = StackComponent(components = listOf(TabControlComponent, visibleTextComponent)),
                ),
            ),
            control = TabsComponent.TabControl.Buttons(
                stack = StackComponent(components = tabControlButtons),
            ),
        )

        val tabLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                visibleTextKey to LocalizationData.Text(visibleTextValue),
                hiddenTextKey to LocalizationData.Text(hiddenTextValue),
                LocalizationKey("tab_0") to LocalizationData.Text("Tab 0"),
                LocalizationKey("tab_1") to LocalizationData.Text("Tab 1"),
            )
        )

        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(tabsComponent, TestData.Components.monthlyPackageComponent),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = tabLocalizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(
            validated,
            customVariables = mapOf("hide_promo" to CustomVariableValue.Boolean(true)),
        )
        val tabStyleFactory = StyleFactory(localizations = tabLocalizations, offering = offering)
        val tabsStyle = tabStyleFactory.create(tabsComponent).getOrThrow().componentStyle as TabsComponentStyle

        setContent { TabsComponentView(style = tabsStyle, state = state, clickHandler = { }) }

        // Tab 0 is shown by default, but the text inside it is hidden by variable condition
        onNodeWithText(hiddenTextValue).assertDoesNotExist()

        // Switch to tab 1
        onNodeWithText("Tab 1").performClick()

        // Tab 1 text is visible (no condition on it)
        onNodeWithText(visibleTextValue).assertIsDisplayed()
    }

    /**
     * Different conditions on components in different tabs.
     * Tab 0 text hidden by selected_package, Tab 1 text always visible.
     */
    @Test
    fun `Different conditions in different tabs`(): Unit = with(composeTestRule) {
        val tab0TextKey = LocalizationKey("tab0_text")
        val tab0TextValue = "Tab 0 content"
        val tab1TextKey = LocalizationKey("tab1_text")
        val tab1TextValue = "Tab 1 content"

        val tab0Text = TextComponent(
            text = tab0TextKey,
            color = textColor,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf(TestData.Packages.monthly.identifier),
                        ),
                    ),
                    properties = PartialTextComponent(visible = false),
                ),
            ),
        )
        val tab1Text = TextComponent(text = tab1TextKey, color = textColor)

        val tabControlButtons = listOf(0, 1).map { index ->
            val key = LocalizationKey("tab_btn_$index")
            TabControlButtonComponent(
                tabIndex = index,
                tabId = "$index",
                stack = StackComponent(
                    components = listOf(TextComponent(text = key, color = textColor)),
                ),
            )
        }

        val tabsComponent = TabsComponent(
            tabs = listOf(
                TabsComponent.Tab(
                    id = "0",
                    stack = StackComponent(components = listOf(TabControlComponent, tab0Text)),
                ),
                TabsComponent.Tab(
                    id = "1",
                    stack = StackComponent(components = listOf(TabControlComponent, tab1Text)),
                ),
            ),
            control = TabsComponent.TabControl.Buttons(
                stack = StackComponent(components = tabControlButtons),
            ),
        )

        val tabLocalizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                tab0TextKey to LocalizationData.Text(tab0TextValue),
                tab1TextKey to LocalizationData.Text(tab1TextValue),
                LocalizationKey("tab_btn_0") to LocalizationData.Text("Tab 0"),
                LocalizationKey("tab_btn_1") to LocalizationData.Text("Tab 1"),
            )
        )

        val data = PaywallComponentsData(
            id = "paywall_id",
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(tabsComponent, TestData.Components.monthlyPackageComponent),
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = tabLocalizations,
            defaultLocaleIdentifier = localeId,
        )
        val offering = Offering(
            identifier = "offering-id",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        val state = offering.toComponentsPaywallState(validated)
        val tabStyleFactory = StyleFactory(localizations = tabLocalizations, offering = offering)
        val tabsStyle = tabStyleFactory.create(tabsComponent).getOrThrow().componentStyle as TabsComponentStyle

        setContent { TabsComponentView(style = tabsStyle, state = state, clickHandler = { }) }

        // Select monthly package so the selected_package condition on tab 0 matches
        state.update(TestData.Packages.monthly.identifier)

        // Tab 0 is active, text is hidden because monthly is selected
        onNodeWithText(tab0TextValue).assertDoesNotExist()

        // Switch to tab 1
        onNodeWithText("Tab 1").performClick()

        // Tab 1 text is always visible
        onNodeWithText(tab1TextValue).assertIsDisplayed()
    }

    // endregion
}
