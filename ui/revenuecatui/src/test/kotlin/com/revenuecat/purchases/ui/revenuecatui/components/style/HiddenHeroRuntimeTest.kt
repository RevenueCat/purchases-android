package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.MeasurePaywallBounds
import com.revenuecat.purchases.ui.revenuecatui.components.heroInsetsFor
import com.revenuecat.purchases.ui.revenuecatui.components.resolveHeroInsets
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import java.net.URL
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class HiddenHeroRuntimeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `default hidden side revealed by width rule relinquishes mobile hero on wide and restores on mobile`() {
        assertResponsiveOwnership(baseVisible = false, wideVisible = true, mobileHero = true)
    }

    @Test
    fun `default visible side hidden by width rule releases mobile hero and restores on mobile`() {
        assertResponsiveOwnership(baseVisible = true, wideVisible = false, mobileHero = false)
    }

    @Test
    fun `visible empty leading stack stops hero selection`() {
        assertRootInsets(
            StackComponent(components = listOf(StackComponent(components = emptyList()), heroStack())),
            expectedHero = false,
            expectedRootInsets = true,
        )
    }

    @Test
    fun `nested vertical fill media preserves root top inset behavior`() {
        assertRootInsets(
            StackComponent(components = listOf(StackComponent(components = listOf(media())))),
            expectedHero = true,
            expectedRootInsets = true,
        )
    }

    @Test
    fun `root configured without top insets keeps them disabled for ordinary content`() {
        assertRootInsets(
            StackComponent(components = listOf(StackComponent(components = emptyList()))),
            expectedHero = false,
            expectedRootInsets = false,
            requestedTopInsets = false,
        )
    }

    @Test
    fun `identical media styles in body and header retain independent hero ownership`() {
        val body = StyleFactory().create(heroStack()).getOrThrow().componentStyle as StackComponentStyle
        val header = StyleFactory().create(heroStack(), applyTopWindowInsets = false)
            .getOrThrow().componentStyle as StackComponentStyle
        val bodyHero = body.children.single()
        val headerHero = header.children.single()
        assertThat(bodyHero).isEqualTo(headerHero)
        assertThat(bodyHero === headerHero).isFalse()
        val state = FakePaywallState()
        val revision = mutableStateOf(0)
        composeTestRule.setContent {
            revision.value
            resolveHeroInsets(body, state)
            resolveHeroInsets(header, state)
        }
        repeat(3) {
            composeTestRule.runOnIdle {
                assertThat(state.heroInsetsFor(bodyHero)!!.hero === bodyHero).isTrue()
                assertThat(state.heroInsetsFor(headerHero)!!.hero === headerHero).isTrue()
                revision.value += 1
            }
        }
    }

    @Test
    fun `equal media siblings switch hero identity when width rules change visibility`() {
        fun branch(baseVisible: Boolean, wideVisible: Boolean) = StyleFactory().create(StackComponent(
            components = listOf(media()),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            visible = baseVisible,
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                )),
                properties = PartialStackComponent(visible = wideVisible),
            )),
        )).getOrThrow().componentStyle as StackComponentStyle
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
        fun assertOwner(owner: StackComponentStyle) = composeTestRule.runOnIdle {
            val insets = state.heroInsetsFor(root)!!
            assertThat(insets.owner === owner).isTrue()
            assertThat(insets.hero === owner.children.single()).isTrue()
        }
        assertOwner(first)
        composeTestRule.runOnIdle { width.value = 1000.dp }
        assertOwner(second)
        composeTestRule.runOnIdle { width.value = 500.dp }
        assertOwner(first)
    }

    private fun heroStack() = StackComponent(
        components = listOf(media()),
        dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
    )

    private fun assertRootInsets(
        component: StackComponent,
        expectedHero: Boolean,
        expectedRootInsets: Boolean,
        requestedTopInsets: Boolean = true,
    ) {
        val root = StyleFactory().create(component, applyTopWindowInsets = requestedTopInsets)
            .getOrThrow().componentStyle as StackComponentStyle
        val state = FakePaywallState()
        composeTestRule.setContent { resolveHeroInsets(root, state) }
        composeTestRule.runOnIdle {
            val insets = state.heroInsetsFor(root)!!
            assertThat(insets.hero != null).isEqualTo(expectedHero)
            assertThat(insets.appliesTo(root)).isEqualTo(expectedRootInsets)
        }
    }

    private fun media() = ImageComponent(
            source = ThemeImageUrls(light = ImageUrls(
                original = URL("https://example.com/hero.png"),
                webp = URL("https://example.com/hero.webp"),
                webpLowRes = URL("https://example.com/hero-small.webp"),
                width = 400u,
                height = 240u,
            )),
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(240u)),
        )

    private fun assertResponsiveOwnership(baseVisible: Boolean, wideVisible: Boolean, mobileHero: Boolean) {
        val media = media()
        val side = StackComponent(
            components = listOf(ImageComponent(
                source = media.source,
                size = Size(SizeConstraint.Fixed(120u), SizeConstraint.Fixed(240u)),
            )),
            visible = baseVisible,
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                )),
                properties = PartialStackComponent(visible = wideVisible),
            )),
        )
        val state = FakePaywallState(components = listOf(side, StackComponent(
            components = listOf(media),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
        )))
        val root = state.stack as StackComponentStyle
        val heroStack = root.children[1] as StackComponentStyle
        val width = mutableStateOf(500.dp)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(width.value, 1000.dp)) {
                resolveHeroInsets(root, state)
            }
        }
        fun assertHero(expected: Boolean) = composeTestRule.runOnIdle {
            val insets = state.heroInsetsFor(root)!!
            assertThat(insets.hero != null).isEqualTo(expected)
            assertThat(state.mainStackHasHeroImage).isEqualTo(expected)
            assertThat(insets.owner === heroStack).isEqualTo(expected)
            assertThat(insets.appliesTo(root)).isEqualTo(!expected)
            assertThat(insets.firstVisibleChildFor(root)).isEqualTo(root.children[if (expected) 1 else 0])
        }
        assertHero(mobileHero)
        composeTestRule.runOnIdle { width.value = 1000.dp }
        assertHero(!mobileHero)
        composeTestRule.runOnIdle { width.value = 500.dp }
        assertHero(mobileHero)
    }
}
