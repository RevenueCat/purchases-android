package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.ExitOffer
import com.revenuecat.purchases.paywalls.components.common.ExitOffers
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
class WorkflowSkeletonTests {

    private val white = ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()))
    private val green = ColorScheme(light = ColorInfo.Hex(0xFF00FF00.toInt()))
    private val clear = ColorScheme(light = ColorInfo.Hex(0x00000000))

    private val imageUrls = ThemeImageUrls(
        light = ImageUrls(
            original = URL("https://example.com/i.png"),
            webp = URL("https://example.com/i.webp"),
            webpLowRes = URL("https://example.com/i-low.webp"),
            width = 100u,
            height = 100u,
        ),
    )

    private fun dataWith(vararg components: PaywallComponent) = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://example.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                stack = StackComponent(components = components.toList()),
                background = Background.Color(white),
            ),
        ),
        componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
        exitOffers = ExitOffers(dismiss = ExitOffer(offeringId = "exit")),
    )

    private fun transformedChildren(vararg components: PaywallComponent): List<PaywallComponent> =
        WorkflowSkeleton.transform(dataWith(*components)).componentsConfig.base.stack.components

    @Test
    fun `removes leaves that have no grey stand-in`() {
        val children = transformedChildren(
            IconComponent(baseUrl = "https://example.com", iconName = "star", formats = iconFormats()),
            WebViewComponent(
                url = "https://example.com",
                id = "web",
                protocolVersion = 1,
                size = Size(SizeConstraint.Fill(), SizeConstraint.Fit()),
            ),
        )

        assertThat(children).isEmpty()
    }

    @Test
    fun `turns a video into its fallback image so it cannot play`() {
        val video = VideoComponent(
            source = ThemeVideoUrls(
                light = VideoUrls(width = 10u, height = 10u, url = URL("https://example.com/v.mp4")),
                dark = null,
            ),
            fallbackSource = imageUrls,
            visible = null,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fit()),
            fitMode = com.revenuecat.purchases.paywalls.components.properties.FitMode.FIT,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = null,
        )

        val children = transformedChildren(video)

        assertThat(children).hasSize(1)
        // The video url is not an image, so it must never become the source.
        val standIn = children.first() as ImageComponent
        assertThat(standIn.source).isEqualTo(imageUrls)
        assertThat(standIn.colorOverlay).isNotNull
    }

    @Test
    fun `greys a text colour instead of keeping the real one`() {
        val text = TextComponent(text = LocalizationKey("key"), color = green, fontSize = 20)

        val children = transformedChildren(text)

        assertThat(children).hasSize(1)
        val color = (children.first() as TextComponent).color
        assertThat(color).isNotEqualTo(green)
        // Inverting contentHidden would make every label invisible and still not be green.
        assertThat(color).isNotEqualTo(clear)
    }

    @Test
    fun `drops exit offers so the stand-in cannot trigger one`() {
        val data = dataWith(StackComponent(components = emptyList()))
        // Guard the fixture: without this the assertion below holds whether or not the transform runs.
        assertThat(data.exitOffers).isNotNull

        assertThat(WorkflowSkeleton.transform(data).exitOffers).isNull()
    }

    @Test
    fun `covers an image inside a hidden block instead of leaving the bitmap bare`() {
        val filledCard = StackComponent(
            components = listOf(ImageComponent(source = imageUrls)),
            backgroundColor = green,
        )

        val card = transformedChildren(filledCard).first() as StackComponent
        val image = card.components.first() as ImageComponent

        // A clear overlay would paint nothing and leave the real image showing through the block.
        assertThat(image.colorOverlay).isNotNull
        assertThat(image.colorOverlay).isNotEqualTo(clear)
    }

    @Test
    fun `resolves an alias to its dark colour when it measures the dark background`() {
        val alias = ColorAlias("brand")
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://example.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(TextComponent(LocalizationKey("k"), green, fontSize = 20))),
                    background = Background.Color(ColorScheme(light = ColorInfo.Alias(alias), dark = ColorInfo.Alias(alias))),
                ),
            ),
            componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
        )
        // Light swatch is bright, dark swatch is nearly black. Reading light for both would pick the
        // same tone twice; the dark side must pick the opposite one.
        val colors = mapOf(
            alias to ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()), dark = ColorInfo.Hex(0xFF000000.toInt())),
        )

        val text = WorkflowSkeleton.transform(data, colors)
            .componentsConfig.base.stack.components.first() as TextComponent

        assertThat(text.color.light).isNotEqualTo(text.color.dark)
    }

    @Test
    fun `keeps a tabs legacy background colour so it still counts as a filled block`() {
        val tabs = TabsComponent(
            backgroundColor = green,
            control = TabsComponent.TabControl.Buttons(StackComponent(components = emptyList())),
            tabs = listOf(TabsComponent.Tab(id = "a", stack = StackComponent(components = emptyList()))),
        )

        val collapsed = transformedChildren(tabs).first() as StackComponent

        assertThat(collapsed.backgroundColor).isNotNull
    }

    @Test
    fun `keeps a badge so the stack still measures its height`() {
        val badged = StackComponent(
            components = emptyList(),
            badge = Badge(
                stack = StackComponent(components = listOf(TextComponent(LocalizationKey("k"), green, fontSize = 20))),
                style = Badge.Style.EdgeToEdge,
                alignment = TwoDimensionalAlignment.TOP,
            ),
        )

        val card = transformedChildren(badged).first() as StackComponent

        assertThat(card.badge).isNotNull
        // The badge is a stand-in too, so it must not keep the real colour.
        val badgeText = card.badge!!.stack.components.first() as TextComponent
        assertThat(badgeText.color).isNotEqualTo(green)
    }


    @Test
    fun `opens tabs on the configured tab, not the first`() {
        val tabs = TabsComponent(
            defaultTabId = "b",
            control = TabsComponent.TabControl.Buttons(StackComponent(components = emptyList())),
            tabs = listOf(
                TabsComponent.Tab(id = "a", stack = StackComponent(components = emptyList())),
                TabsComponent.Tab(
                    id = "b",
                    stack = StackComponent(
                        components = listOf(TextComponent(LocalizationKey("k"), green, fontSize = 20)),
                    ),
                ),
            ),
        )

        val collapsed = transformedChildren(tabs).first() as StackComponent
        val keptTab = collapsed.components.first() as StackComponent

        // Tab "a" is empty, so keeping the first tab would grey out nothing.
        assertThat(keptTab.components).hasSize(1)
    }

    @Test
    fun `keeps an override so the stand-in can take the shape the rules will pick`() {
        val overridden = StackComponent(
            components = emptyList(),
            backgroundColor = green,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Compact),
                    properties = PartialStackComponent(
                        size = Size(SizeConstraint.Fixed(42u), SizeConstraint.Fixed(42u)),
                        backgroundColor = green,
                    ),
                ),
            ),
        )

        val card = transformedChildren(overridden).first() as StackComponent

        assertThat(card.overrides).hasSize(1)
        val partial = card.overrides.first().properties
        // The size decides the shape, so it survives; the colour must not.
        assertThat(partial.size?.width).isEqualTo(SizeConstraint.Fixed(42u))
        assertThat(partial.backgroundColor).isNotEqualTo(green)
    }


    @Test
    fun `keeps an image override opaque so a matching rule cannot expose the bitmap`() {
        val card = StackComponent(
            components = listOf(
                ImageComponent(
                    source = imageUrls,
                    overrides = listOf(
                        ComponentOverride(
                            conditions = listOf(ComponentOverride.Condition.Compact),
                            properties = PartialImageComponent(size = Size(SizeConstraint.Fixed(10u), SizeConstraint.Fixed(10u))),
                        ),
                    ),
                ),
            ),
            backgroundColor = green,
        )

        val image = (transformedChildren(card).first() as StackComponent).components.first() as ImageComponent

        // A clear overlay on a matching override draws the real photo inside the grey card.
        assertThat(image.overrides.first().properties.colorOverlay).isNotEqualTo(clear)
    }

    private fun iconFormats() = IconComponent.Formats(
        webp = "star.webp",
    )
}
