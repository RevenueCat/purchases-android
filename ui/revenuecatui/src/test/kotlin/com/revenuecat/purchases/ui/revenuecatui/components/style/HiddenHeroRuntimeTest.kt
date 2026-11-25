package com.revenuecat.purchases.ui.revenuecatui.components.style

import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.view.descendants
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentStyleIdentity
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.MeasurePaywallBounds
import com.revenuecat.purchases.ui.revenuecatui.components.ResolvedHeroInsets
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.CountdownComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.heroInsetsFor
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.resolveHeroInsets
import com.revenuecat.purchases.ui.revenuecatui.components.video.FakeFileRepository
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.Date

@RunWith(AndroidJUnit4::class)
internal class HiddenHeroRuntimeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `visible empty leading stack stops hero selection`() {
        assertRootInsets(
            StackComponent(components = listOf(StackComponent(components = emptyList()), heroStack())),
            expectedHero = false,
            expectedRootInsets = true,
        )
    }

    @Test
    fun `nested vertical fill media stays edge to edge`() {
        assertRootInsets(
            StackComponent(components = listOf(StackComponent(components = listOf(media())))),
            expectedHero = true,
            expectedRootInsets = false,
        )
    }

    @Test
    fun `horizontal hero stops bleeding when a sibling appears and restores when hidden`() {
        val hidden = StackComponent(visible = false, components = emptyList())
        val sibling = StackComponent(
            visible = false,
            components = emptyList(),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                )),
                properties = PartialStackComponent(visible = true),
            )),
        )
        val row = StackComponent(
            components = listOf(hidden, heroStack(), sibling),
            dimension = Dimension.Horizontal(VerticalAlignment.TOP, FlexDistribution.START),
        )
        val state = FakePaywallState(components = listOf(StackComponent(
            components = listOf(hidden, row, media()),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
        )))
        val zLayer = (state.stack as StackComponentStyle).children.single() as StackComponentStyle
        val rowStyle = zLayer.children[1]
        val width = mutableStateOf(500.dp)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(width.value, 1000.dp)) {
                resolveHeroInsets(state.stack, state)
            }
        }
        fun assertHero(expectedHero: Boolean) = composeTestRule.runOnIdle {
            val insets = state.heroInsetsFor(state.stack)!!
            assertThat(state.mainStackHasHeroImage).isEqualTo(expectedHero)
            assertThat(insets.hero != null).isEqualTo(expectedHero)
            assertThat(insets.exemptChildFor(zLayer) === rowStyle).isEqualTo(expectedHero)
        }
        assertHero(true)
        composeTestRule.runOnIdle { width.value = 1000.dp }
        assertHero(false)
        composeTestRule.runOnIdle { width.value = 500.dp }
        assertHero(true)
    }

    @Test
    fun `root configured without top insets retains its existing policy`() {
        val root = StyleFactory().create(heroStack(), applyTopWindowInsets = false)
            .getOrThrow().componentStyle as StackComponentStyle
        val state = FakePaywallState()
        composeTestRule.setContent { assertThat(resolveHeroInsets(root, state)).isNull() }
        composeTestRule.runOnIdle { assertThat(state.heroInsetsFor(root)).isNull() }
    }

    @Test
    fun `equal media siblings switch hero identity when width rules change visibility`() {
        // Build both branches with identical factory inset hints to exercise identity-based lookup.
        fun branch(baseVisible: Boolean, wideVisible: Boolean) = (StyleFactory().create(StackComponent(
            components = listOf(media()),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            visible = true,
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                )),
                properties = PartialStackComponent(visible = wideVisible),
            )),
        )).getOrThrow().componentStyle as StackComponentStyle).copy(visible = baseVisible)
        val first = branch(baseVisible = true, wideVisible = false)
        val second = branch(baseVisible = false, wideVisible = true)
        assertThat(first.children.single()).isEqualTo(second.children.single())
        val root = (StyleFactory().create(StackComponent(components = emptyList()))
            .getOrThrow().componentStyle as StackComponentStyle).copy(children = listOf(first, second))
        val state = FakePaywallState()
        val width = mutableStateOf(500.dp)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(width.value, 1000.dp)) {
                resolveHeroInsets(root, state)
            }
        }
        fun assertHeroBranch(owner: StackComponentStyle) = composeTestRule.runOnIdle {
            val insets = state.heroInsetsFor(root)!!
            assertThat(insets.exemptChildFor(owner) === owner.children.single()).isTrue()
            assertThat(insets.hero === owner.children.single()).isTrue()
        }
        assertHeroBranch(first)
        composeTestRule.runOnIdle { width.value = 1000.dp }
        assertHeroBranch(second)
        composeTestRule.runOnIdle { width.value = 500.dp }
        assertHeroBranch(first)
    }

    @Test
    fun `empty spacer inside a hidden stack allows the following header media to bleed`() {
        val state = FakePaywallState(
            header = HeaderComponent(StackComponent(components = emptyList())),
            components = listOf(
                StackComponent(visible = false, components = listOf(StackComponent(components = emptyList()))),
                heroStack(),
            ),
        )
        assertThat(state.mainStackHasHeroImage).isTrue()
    }

    @Test
    fun `visible spacer with only a hidden stack keeps the video below the header`() {
        val spacer = StackComponent(
            components = listOf(StackComponent(visible = false, components = emptyList())),
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(0u)),
        )
        val url = URL("file:///header-video.mp4")
        val video = VideoComponent(
            source = ThemeVideoUrls(light = VideoUrls(320u, 120u, url), dark = null),
            fallbackSource = null,
            visible = true,
            showControls = false,
            autoplay = false,
            loop = false,
            muteAudio = true,
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(120u)),
            fitMode = FitMode.FILL,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = null,
        )
        val state = FakePaywallState(
            header = HeaderComponent(StackComponent(
                components = emptyList(),
                size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(64u)),
            )),
            components = listOf(spacer, video),
            purchases = object : PurchasesType by MockPurchasesType() {
                override val fileRepositoryOverride = FakeFileRepository(mapOf(url to url.toURI()))
            },
        )
        lateinit var root: ViewGroup
        composeTestRule.setContent {
            root = LocalView.current.rootView as ViewGroup
            LoadedPaywallComponents(state, clickHandler = {})
        }

        val paywallTop = composeTestRule.onRoot().fetchSemanticsNode().boundsInWindow.top
        composeTestRule.runOnIdle {
            val videoView = root.descendants.filterIsInstance<TextureView>().single()
            val location = IntArray(2)
            videoView.getLocationInWindow(location)
            assertThat(state.headerHeightPx).isPositive()
            assertThat(location[1].toFloat()).isEqualTo(paywallTop + state.headerHeightPx)
        }
    }

    @Test
    fun `header clearance follows the presented visibility of an empty spacer`() {
        val spacer = StackComponent(
            components = emptyList(),
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(0u)),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 250.0,
                )),
                properties = PartialStackComponent(visible = false),
            )),
        )
        val caption = TextComponent(text = LocalizationKey("dummyKey"), color = ColorScheme(ColorInfo.Hex(-1)))
        val state = FakePaywallState(
            header = HeaderComponent(StackComponent(
                components = emptyList(),
                size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(64u)),
            )),
            components = listOf(spacer, heroStack(), caption),
        )
        val width = mutableStateOf(200.dp)
        composeTestRule.setContent {
            LoadedPaywallComponents(state, clickHandler = {}, modifier = Modifier.requiredSize(width.value, 400.dp))
        }
        fun captionTop() = composeTestRule.onNodeWithText("dummyText").fetchSemanticsNode().boundsInRoot.top
        val visibleTop = captionTop()
        composeTestRule.runOnIdle {
            assertThat(state.headerHeightPx).isPositive()
            assertThat(state.mainStackHasHeroImage).isFalse()
            width.value = 300.dp
        }
        assertThat(captionTop()).isEqualTo(visibleTop - state.headerHeightPx)
        composeTestRule.runOnIdle {
            assertThat(state.mainStackHasHeroImage).isTrue()
            width.value = 200.dp
        }
        assertThat(captionTop()).isEqualTo(visibleTop)
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isFalse() }
    }

    @Test
    fun `state and package overrides can reveal a spacer while hero overlays stay below the header`() {
        val monthly = TestData.Packages.monthly
        val annual = TestData.Packages.annual
        val key = "showSpacer"
        val show = PartialStackComponent(visible = true)
        val spacer = StackComponent(
            components = listOf(StackComponent(components = emptyList())),
            visible = false,
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(0u)),
            overrides = listOf(
                ComponentOverride(listOf(ComponentOverride.Condition.State(
                    ComponentOverride.EqualityOperator.EQUALS, key, JsonPrimitive(true),
                )), show),
                ComponentOverride(listOf(ComponentOverride.Condition.SelectedPackage(
                    ComponentOverride.ArrayOperator.IN, listOf(monthly.identifier),
                )), show),
            ),
        )
        val overlay = TextComponent(text = LocalizationKey("dummyKey"), color = ColorScheme(ColorInfo.Hex(-1)))
        val state = FakePaywallState(
            header = HeaderComponent(StackComponent(
                components = emptyList(),
                size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(64u)),
            )),
            components = listOf(spacer, StackComponent(
                components = listOf(media(), overlay),
                dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            )),
            packages = listOf(annual, monthly),
            stateDeclarations = mapOf(
                key to StateDeclaration(StateDeclaration.ValueType.BOOLEAN, JsonPrimitive(false)),
            ),
        )
        state.update(annual.identifier)
        composeTestRule.setContent { LoadedPaywallComponents(state, clickHandler = {}) }

        fun assertLayout(hero: Boolean) {
            val top = composeTestRule.onNodeWithText("dummyText").fetchSemanticsNode().boundsInRoot.top
            composeTestRule.runOnIdle {
                assertThat(state.mainStackHasHeroImage).isEqualTo(hero)
                assertThat(state.headerHeightPx).isPositive()
                assertThat(top).isEqualTo(state.headerHeightPx.toFloat())
            }
        }
        fun setShowSpacer(value: Boolean) = composeTestRule.runOnIdle {
            state.stateStore.applyUpdates(listOf(StateUpdate.Set(key, StateUpdateValue.Literal(JsonPrimitive(value)))))
        }

        assertLayout(hero = true)
        setShowSpacer(true)
        assertLayout(hero = false)
        setShowSpacer(false)
        assertLayout(hero = true)
        composeTestRule.runOnIdle { state.update(monthly.identifier) }
        assertLayout(hero = false)
        composeTestRule.runOnIdle { state.update(annual.identifier) }
        assertLayout(hero = true)
    }

    @Test
    fun `header paywalls retain authored hero detection and insets across resizing`() {
        val state = FakePaywallState(
            components = listOf(
                StackComponent(visible = false, components = listOf(media(width = SizeConstraint.Fixed(120u)))),
                heroStack(),
            ),
            header = HeaderComponent(stack = StackComponent(components = emptyList())),
        )
        val originalHero = state.mainStackHasHeroImage
        val width = mutableStateOf(500.dp)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(width.value, 1000.dp)) {
                resolveHeroInsets(state.stack, state)
            }
        }
        for (size in listOf(1000.dp, 500.dp)) {
            composeTestRule.runOnIdle {
                assertThat(state.heroInsetsFor(state.stack)).isNull()
                assertThat(state.mainStackHasHeroImage).isEqualTo(originalHero)
                width.value = size
            }
        }
    }

    @Test
    fun `resolved dimensions determine which ancestors pad their overlays`() {
        val root = StyleFactory().create(StackComponent(
            components = listOf(media()),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                )),
                properties = PartialStackComponent(dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP)),
            )),
        )).getOrThrow().componentStyle as StackComponentStyle
        val state = FakePaywallState()
        val width = mutableStateOf(500.dp)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(width.value, 1000.dp)) {
                resolveHeroInsets(root, state)
            }
        }
        composeTestRule.runOnIdle {
            assertThat(state.heroInsetsFor(root)!!.exemptChildFor(root)).isNull()
            width.value = 1000.dp
        }
        composeTestRule.runOnIdle {
            assertThat(state.heroInsetsFor(root)!!.exemptChildFor(root) === root.children.single()).isTrue()
        }
    }

    @Test
    fun `hero ZLayer does not apply another top inset inside its button`() {
        val root = StyleFactory().create(StackComponent(
            components = listOf(media(), ButtonComponent(
                action = ButtonComponent.Action.NavigateBack,
                stack = StackComponent(components = emptyList()),
            )),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
        )).getOrThrow().componentStyle as StackComponentStyle
        val button = root.children[1] as ButtonComponentStyle
        val state = FakePaywallState()
        composeTestRule.setContent { resolveHeroInsets(root, state) }
        composeTestRule.runOnIdle {
            val buttonInsets = state.heroInsetsFor(button.stackComponentStyle)
            assertThat(buttonInsets).isNotNull()
            assertThat(buttonInsets!!.appliesTo(button.stackComponentStyle)).isFalse()
            assertThat(state.heroInsetsFor(root)!!.appliesTo(root)).isTrue()
        }
    }

    @Test
    fun `countdown renders the same active stack used for hero resolution`() {
        val running = previewStackComponentStyle(children = listOf(previewTextComponentStyle(text = "Running")))
        val ended = previewStackComponentStyle(children = listOf(previewTextComponentStyle(text = "Expired")))
        val countdown = CountdownComponentStyle(
            date = Date(Long.MAX_VALUE),
            countFrom = CountdownComponent.CountFrom.DAYS,
            countdownStackComponentStyle = running,
            endStackComponentStyle = ended,
            fallbackStackComponentStyle = null,
        )
        val state = FakePaywallState()
        val root = state.stack as StackComponentStyle
        fun select(stack: StackComponentStyle) {
            val insets = ResolvedHeroInsets(
                root = root,
                hero = null,
                zLayerBranches = emptyList(),
                firstVisibleChildren = emptyList(),
                activeCountdownStacks = mapOf(ComponentStyleIdentity(countdown) to stack),
            )
            state.resolvedHeroInsets = mapOf(ComponentStyleIdentity(countdown) to insets)
        }
        // A separate timer would pick Running from the date, disagreeing with the resolved branch.
        select(ended)
        composeTestRule.setContent { CountdownComponentView(countdown, state, onClick = {}) }
        composeTestRule.onNodeWithText("Expired").assertIsDisplayed()
        composeTestRule.runOnIdle { select(running) }
        composeTestRule.onNodeWithText("Running").assertIsDisplayed()
    }

    @Test
    fun `countdown visibility follows width rules before choosing a hero`() {
        val countdown = CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "fixed", date = Date(Long.MAX_VALUE)),
            countdownStack = StackComponent(
                visible = false, components = emptyList(),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                        ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                    )),
                    properties = PartialStackComponent(visible = true),
                )),
            ),
        )
        val state = FakePaywallState(components = listOf(countdown, heroStack()))
        val root = state.stack as StackComponentStyle
        val width = mutableStateOf(500.dp)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(width.value, 1000.dp)) {
                resolveHeroInsets(root, state)
            }
        }
        fun assertHero(expected: Boolean) = composeTestRule.runOnIdle {
            assertThat(state.heroInsetsFor(root)!!.hero != null).isEqualTo(expected)
        }
        assertHero(true)
        composeTestRule.runOnIdle { width.value = 1000.dp }
        assertHero(false)
        composeTestRule.runOnIdle { width.value = 500.dp }
        assertHero(true)
    }

    @Test
    fun `ended countdown uses its end stack visibility`() {
        val countdown = CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "fixed", date = Date(0)),
            countdownStack = StackComponent(components = emptyList()),
            endStack = StackComponent(visible = false, components = emptyList()),
        )
        assertRootInsets(
            StackComponent(components = listOf(countdown, heroStack())),
            expectedHero = true,
            expectedRootInsets = false,
        )
    }

    @Test
    fun `button with a hidden inner stack does not block the hero`() {
        val button = ButtonComponent(
            action = ButtonComponent.Action.NavigateBack, visible = true,
            stack = StackComponent(visible = false, components = emptyList()),
        )
        assertRootInsets(
            StackComponent(components = listOf(button, heroStack())),
            expectedHero = true,
            expectedRootInsets = false,
        )
    }

    private fun heroStack() = StackComponent(
        components = listOf(media()),
        dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
    )

    private fun assertRootInsets(
        component: StackComponent,
        expectedHero: Boolean,
        expectedRootInsets: Boolean,
    ) {
        val root = StyleFactory().create(component)
            .getOrThrow().componentStyle as StackComponentStyle
        val state = FakePaywallState()
        composeTestRule.setContent { resolveHeroInsets(root, state) }
        composeTestRule.runOnIdle {
            val insets = state.heroInsetsFor(root)!!
            assertThat(insets.hero != null).isEqualTo(expectedHero)
            assertThat(insets.appliesTo(root)).isEqualTo(expectedRootInsets)
        }
    }

    private fun media(width: SizeConstraint = SizeConstraint.Fill()) = ImageComponent(
            source = ThemeImageUrls(light = ImageUrls(
                original = URL("https://example.com/hero.png"),
                webp = URL("https://example.com/hero.webp"),
                webpLowRes = URL("https://example.com/hero-small.webp"),
                width = 400u,
                height = 240u,
            )),
            size = Size(width, SizeConstraint.Fixed(240u)),
        )
}
