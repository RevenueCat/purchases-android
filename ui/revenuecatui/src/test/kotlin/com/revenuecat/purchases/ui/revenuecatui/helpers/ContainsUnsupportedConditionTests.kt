package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.FallbackHeaderComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.util.Date

@Suppress("LargeClass")
internal class ContainsUnsupportedConditionTests {

    // region helpers

    private val unsupportedOverride = ComponentOverride(
        conditions = listOf(ComponentOverride.Condition.Unsupported),
        properties = PartialTextComponent(),
    )

    private val supportedOverride = ComponentOverride(
        conditions = listOf(ComponentOverride.Condition.Compact),
        properties = PartialTextComponent(),
    )

    private val color = ColorScheme(light = ColorInfo.Hex(0xFF000000.toInt()))
    private val textKey = LocalizationKey("key")
    private val dummyUrl = URL("https://example.com/img.png")
    private val imageUrls = ImageUrls(original = dummyUrl, webp = dummyUrl, webpLowRes = dummyUrl, width = 100u, height = 100u)
    private val themeImageUrls = ThemeImageUrls(light = imageUrls)
    private val videoUrls = VideoUrls(width = 100u, height = 100u, url = dummyUrl)
    private val themeVideoUrls = ThemeVideoUrls(light = videoUrls, dark = null)
    private val iconFormats = IconComponent.Formats(webp = "icon.webp")

    private fun emptyStack(
        overrides: List<ComponentOverride<PartialStackComponent>> = emptyList(),
        components: List<com.revenuecat.purchases.paywalls.components.PaywallComponent> = emptyList(),
    ) = StackComponent(components = components, overrides = overrides)

    private fun textComponent(
        overrides: List<ComponentOverride<PartialTextComponent>> = emptyList(),
    ) = TextComponent(text = textKey, color = color, overrides = overrides)

    private fun config(
        stack: StackComponent,
        header: HeaderComponent? = null,
        stickyFooter: StickyFooterComponent? = null,
    ) = PaywallComponentsConfig(
        stack = stack,
        background = Background.Color(color),
        header = header,
        stickyFooter = stickyFooter,
    )

    // endregion

    // region PaywallComponentsConfig

    @Test
    fun `Config with no unsupported conditions returns false`() {
        val result = config(stack = emptyStack()).containsUnsupportedCondition()
        assertFalse(result)
    }

    @Test
    fun `Config detects unsupported in main stack overrides`() {
        val stack = emptyStack(
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = PartialStackComponent(),
                ),
            ),
        )
        assertTrue(config(stack = stack).containsUnsupportedCondition())
    }

    @Test
    fun `Config detects unsupported in stickyFooter`() {
        val footer = StickyFooterComponent(
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        assertTrue(config(stack = emptyStack(), stickyFooter = footer).containsUnsupportedCondition())
    }

    @Test
    fun `Config detects unsupported in header`() {
        val header = HeaderComponent(
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        assertTrue(config(stack = emptyStack(), header = header).containsUnsupportedCondition())
    }

    // endregion

    // region TextComponent

    @Test
    fun `TextComponent with unsupported override detected`() {
        val text = textComponent(overrides = listOf(unsupportedOverride))
        val stack = emptyStack(components = listOf(text))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TextComponent with only supported overrides not detected`() {
        val text = textComponent(overrides = listOf(supportedOverride))
        val stack = emptyStack(components = listOf(text))
        assertFalse(stack.containsUnsupportedCondition())
    }

    // endregion

    // region ImageComponent

    @Test
    fun `ImageComponent with unsupported override detected`() {
        val image = ImageComponent(
            source = themeImageUrls,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = PartialImageComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(image))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region VideoComponent

    @Test
    fun `VideoComponent with unsupported override detected`() {
        val video = VideoComponent(
            source = themeVideoUrls,
            fallbackSource = null,
            visible = null,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
            fitMode = FitMode.FIT,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = com.revenuecat.purchases.paywalls.components.PartialVideoComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(video))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region IconComponent

    @Test
    fun `IconComponent with unsupported override detected`() {
        val icon = IconComponent(
            baseUrl = "https://example.com",
            iconName = "star",
            formats = iconFormats,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = com.revenuecat.purchases.paywalls.components.PartialIconComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(icon))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region Deeply nested StackComponent

    @Test
    fun `Unsupported condition 3 levels deep in stacks detected`() {
        val deepText = textComponent(overrides = listOf(unsupportedOverride))
        val innerStack = emptyStack(components = listOf(deepText))
        val middleStack = emptyStack(components = listOf(innerStack))
        val outerStack = emptyStack(components = listOf(middleStack))
        assertTrue(outerStack.containsUnsupportedCondition())
    }

    // endregion

    // region ButtonComponent

    @Test
    fun `ButtonComponent detects unsupported in its stack`() {
        val button = ButtonComponent(
            action = ButtonComponent.Action.RestorePurchases,
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(button))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `ButtonComponent detects unsupported in Sheet destination`() {
        val sheetStack = emptyStack(
            components = listOf(
                textComponent(overrides = listOf(unsupportedOverride)),
            ),
        )
        val button = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(
                destination = ButtonComponent.Destination.Sheet(
                    id = "sheet1",
                    name = "Sheet",
                    stack = sheetStack,
                    backgroundBlur = false,
                    size = null,
                ),
            ),
            stack = emptyStack(),
        )
        val stack = emptyStack(components = listOf(button))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `ButtonComponent with non-Sheet destination and clean stack returns false`() {
        val button = ButtonComponent(
            action = ButtonComponent.Action.NavigateBack,
            stack = emptyStack(components = listOf(textComponent())),
        )
        val stack = emptyStack(components = listOf(button))
        assertFalse(stack.containsUnsupportedCondition())
    }

    // endregion

    // region PackageComponent

    @Test
    fun `PackageComponent detects unsupported in its stack`() {
        val pkg = PackageComponent(
            packageId = "monthly",
            isSelectedByDefault = true,
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(pkg))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `PackageComponent detects unsupported in its own overrides`() {
        val pkg = PackageComponent(
            packageId = "monthly",
            isSelectedByDefault = true,
            stack = emptyStack(),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = PartialPackageComponent(visible = false),
                )
            ),
        )
        val stack = emptyStack(components = listOf(pkg))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region PurchaseButtonComponent

    @Test
    fun `PurchaseButtonComponent detects unsupported in its stack`() {
        val purchaseButton = PurchaseButtonComponent(
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(purchaseButton))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region StickyFooterComponent

    @Test
    fun `StickyFooterComponent detects unsupported in its stack`() {
        val footer = StickyFooterComponent(
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(footer))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region HeaderComponent

    @Test
    fun `HeaderComponent detects unsupported in its stack`() {
        val header = HeaderComponent(
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(header))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region FallbackHeaderComponent

    @Test
    fun `FallbackHeaderComponent does not contain unsupported condition`() {
        val stack = emptyStack(components = listOf(FallbackHeaderComponent))
        assertFalse(stack.containsUnsupportedCondition())
    }

    // endregion

    // region CarouselComponent

    @Test
    fun `CarouselComponent detects unsupported in its own overrides`() {
        val carousel = CarouselComponent(
            pages = listOf(emptyStack()),
            pageAlignment = VerticalAlignment.CENTER,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = com.revenuecat.purchases.paywalls.components.PartialCarouselComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(carousel))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `CarouselComponent detects unsupported in page stack`() {
        val carousel = CarouselComponent(
            pages = listOf(
                emptyStack(
                    components = listOf(
                        textComponent(overrides = listOf(unsupportedOverride)),
                    ),
                ),
            ),
            pageAlignment = VerticalAlignment.CENTER,
        )
        val stack = emptyStack(components = listOf(carousel))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region TabsComponent

    @Test
    fun `TabsComponent detects unsupported in its own overrides`() {
        val tabs = TabsComponent(
            control = TabsComponent.TabControl.Buttons(stack = emptyStack()),
            tabs = listOf(TabsComponent.Tab(id = "tab1", stack = emptyStack())),
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = com.revenuecat.purchases.paywalls.components.PartialTabsComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(tabs))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TabsComponent detects unsupported in tab stack`() {
        val tabs = TabsComponent(
            control = TabsComponent.TabControl.Buttons(stack = emptyStack()),
            tabs = listOf(
                TabsComponent.Tab(
                    id = "tab1",
                    stack = emptyStack(
                        components = listOf(
                            textComponent(overrides = listOf(unsupportedOverride)),
                        ),
                    ),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(tabs))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TabsComponent detects unsupported in Buttons control stack`() {
        val tabs = TabsComponent(
            control = TabsComponent.TabControl.Buttons(
                stack = emptyStack(
                    components = listOf(
                        textComponent(overrides = listOf(unsupportedOverride)),
                    ),
                ),
            ),
            tabs = listOf(TabsComponent.Tab(id = "tab1", stack = emptyStack())),
        )
        val stack = emptyStack(components = listOf(tabs))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TabsComponent detects unsupported in Toggle control stack`() {
        val tabs = TabsComponent(
            control = TabsComponent.TabControl.Toggle(
                stack = emptyStack(
                    components = listOf(
                        textComponent(overrides = listOf(unsupportedOverride)),
                    ),
                ),
            ),
            tabs = listOf(TabsComponent.Tab(id = "tab1", stack = emptyStack())),
        )
        val stack = emptyStack(components = listOf(tabs))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region TimelineComponent

    @Test
    fun `TimelineComponent detects unsupported in its own overrides`() {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 8,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Unsupported),
                    properties = com.revenuecat.purchases.paywalls.components.PartialTimelineComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(timeline))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TimelineComponent detects unsupported in item title overrides`() {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 8,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            items = listOf(
                TimelineComponent.Item(
                    title = textComponent(overrides = listOf(unsupportedOverride)),
                    icon = IconComponent(
                        baseUrl = "https://example.com",
                        iconName = "star",
                        formats = iconFormats,
                    ),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(timeline))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TimelineComponent detects unsupported in item description overrides`() {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 8,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            items = listOf(
                TimelineComponent.Item(
                    title = textComponent(),
                    description = textComponent(overrides = listOf(unsupportedOverride)),
                    icon = IconComponent(
                        baseUrl = "https://example.com",
                        iconName = "star",
                        formats = iconFormats,
                    ),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(timeline))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TimelineComponent detects unsupported in item icon overrides`() {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 8,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            items = listOf(
                TimelineComponent.Item(
                    title = textComponent(),
                    icon = IconComponent(
                        baseUrl = "https://example.com",
                        iconName = "star",
                        formats = iconFormats,
                        overrides = listOf(
                            ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.Unsupported),
                                properties = com.revenuecat.purchases.paywalls.components.PartialIconComponent(),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(timeline))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `TimelineComponent detects unsupported in item overrides`() {
        val timeline = TimelineComponent(
            itemSpacing = 8,
            textSpacing = 4,
            columnGutter = 8,
            iconAlignment = TimelineComponent.IconAlignment.Title,
            items = listOf(
                TimelineComponent.Item(
                    title = textComponent(),
                    icon = IconComponent(
                        baseUrl = "https://example.com",
                        iconName = "star",
                        formats = iconFormats,
                    ),
                    overrides = listOf(
                        ComponentOverride(
                            conditions = listOf(ComponentOverride.Condition.Unsupported),
                            properties = com.revenuecat.purchases.paywalls.components.PartialTimelineComponentItem(),
                        ),
                    ),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(timeline))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region CountdownComponent

    @Test
    fun `CountdownComponent detects unsupported in countdownStack`() {
        val countdown = CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "fixed", date = Date()),
            countdownStack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(countdown))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `CountdownComponent detects unsupported in endStack`() {
        val countdown = CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "fixed", date = Date()),
            countdownStack = emptyStack(),
            endStack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(countdown))
        assertTrue(stack.containsUnsupportedCondition())
    }

    @Test
    fun `CountdownComponent detects unsupported in fallback stack`() {
        val countdown = CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "fixed", date = Date()),
            countdownStack = emptyStack(),
            fallback = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(countdown))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region TabControlButtonComponent

    @Test
    fun `TabControlButtonComponent detects unsupported in its stack`() {
        val tabControlButton = TabControlButtonComponent(
            tabIndex = 0,
            tabId = "tab1",
            stack = emptyStack(
                components = listOf(
                    textComponent(overrides = listOf(unsupportedOverride)),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(tabControlButton))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion

    // region Mixed supported conditions only

    @Test
    fun `Stack with only supported conditions across all components returns false`() {
        val text = textComponent(overrides = listOf(supportedOverride))
        val image = ImageComponent(
            source = themeImageUrls,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.IntroOffer),
                    properties = PartialImageComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(text, image))
        assertFalse(stack.containsUnsupportedCondition())
    }

    // endregion

    // region VideoComponent with null overrides

    @Test
    fun `VideoComponent with null overrides returns false`() {
        val video = VideoComponent(
            source = themeVideoUrls,
            fallbackSource = null,
            visible = null,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
            fitMode = FitMode.FIT,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = null,
        )
        val stack = emptyStack(components = listOf(video))
        assertFalse(stack.containsUnsupportedCondition())
    }

    // endregion

    // region Rule conditions that are NOT Unsupported

    @Test
    fun `Rule conditions without Unsupported do not trigger scan`() {
        val text = textComponent(
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.SelectedPackage(
                            operator = ComponentOverride.ArrayOperator.IN,
                            packages = listOf("monthly"),
                        ),
                    ),
                    properties = PartialTextComponent(),
                ),
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Variable(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            variable = "plan",
                            value = kotlinx.serialization.json.JsonPrimitive("premium"),
                        ),
                    ),
                    properties = PartialTextComponent(),
                ),
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.IntroOfferRule(
                            operator = ComponentOverride.EqualityOperator.EQUALS,
                            value = true,
                        ),
                    ),
                    properties = PartialTextComponent(),
                ),
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.PromoOfferRule(
                            operator = ComponentOverride.EqualityOperator.NOT_EQUALS,
                            value = false,
                        ),
                    ),
                    properties = PartialTextComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(text))
        assertFalse(stack.containsUnsupportedCondition())
    }

    // endregion

    // region Unsupported mixed with other conditions in same override

    @Test
    fun `Override with Unsupported among other conditions is detected`() {
        val text = textComponent(
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.Compact,
                        ComponentOverride.Condition.Unsupported,
                    ),
                    properties = PartialTextComponent(),
                ),
            ),
        )
        val stack = emptyStack(components = listOf(text))
        assertTrue(stack.containsUnsupportedCondition())
    }

    // endregion
}
