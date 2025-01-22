package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class PresentedStackPartialTests {

    @RunWith(Parameterized::class)
    class CombinePresentedStackPartialTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            val base: PresentedStackPartial,
            val override: PresentedStackPartial?,
            val expected: PresentedStackPartial,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "Should properly override all properties if they are non-null in both",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                spacing = 20f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                    width = 2.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                    radius = 0.0,
                                    x = 20.0,
                                    y = 20.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Nested,
                                    alignment = TwoDimensionalAlignment.BOTTOM,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                spacing = 20f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                    width = 2.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                    radius = 0.0,
                                    x = 20.0,
                                    y = 20.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Nested,
                                    alignment = TwoDimensionalAlignment.BOTTOM,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow()
                    )
                ),
                arrayOf(
                    "Should not override anything if all properties are null in both",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = null,
                                dimension = null,
                                size = null,
                                spacing = null,
                                backgroundColor = null,
                                padding = null,
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                                badge = null
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = null,
                                dimension = null,
                                size = null,
                                spacing = null,
                                backgroundColor = null,
                                padding = null,
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                                badge = null
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = null,
                                dimension = null,
                                size = null,
                                spacing = null,
                                backgroundColor = null,
                                padding = null,
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                                badge = null
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should not override anything if override is null",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = null,
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the first 6 individual properties if they are non-null in both",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                spacing = 20f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                                badge = null,
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                spacing = 20f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the first 6 individual properties if they are null in the base",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = null,
                                dimension = null,
                                size = null,
                                spacing = null,
                                backgroundColor = null,
                                padding = null,
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                spacing = 20f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                                badge = null,
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                spacing = 20f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the second 5 individual properties if they are non-null in both",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 10.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                    width = 1.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                    radius = 10.0,
                                    x = 10.0,
                                    y = 10.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Overlay,
                                    alignment = TwoDimensionalAlignment.TOP,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = null,
                                dimension = null,
                                size = null,
                                spacing = null,
                                backgroundColor = null,
                                padding = null,
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                    width = 2.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                    radius = 0.0,
                                    x = 20.0,
                                    y = 20.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Nested,
                                    alignment = TwoDimensionalAlignment.BOTTOM,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                    width = 2.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                    radius = 0.0,
                                    x = 20.0,
                                    y = 20.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Nested,
                                    alignment = TwoDimensionalAlignment.BOTTOM,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the second 5 individual properties if they are null in the base",
                    Args(
                        base = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                                badge = null
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = null,
                                dimension = null,
                                size = null,
                                spacing = null,
                                backgroundColor = null,
                                padding = null,
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                    width = 2.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                    radius = 0.0,
                                    x = 20.0,
                                    y = 20.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Nested,
                                    alignment = TwoDimensionalAlignment.BOTTOM,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedStackPartial(
                            from = PartialStackComponent(
                                visible = true,
                                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                spacing = 10f,
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                                border = Border(
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                    width = 2.0
                                ),
                                shadow = Shadow(
                                    ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                    radius = 0.0,
                                    x = 20.0,
                                    y = 20.0
                                ),
                                badge = Badge(
                                    stack = StackComponent(components = emptyList()),
                                    style = Badge.Style.Nested,
                                    alignment = TwoDimensionalAlignment.BOTTOM,
                                ),
                            ),
                            using = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
            )
        }

        @Test
        fun `Should properly combine PresentedStackPartials`() {
            // Arrange, Act
            val actual = args.base.combine(args.override)

            // Assert
            assertEquals(args.expected, actual)
        }
    }

    class CreatePresentedStackPartial {

        @Test
        fun `Should fail to create if the ColorAlias is not found`() {
            // Arrange, Act
            val actualResult = PresentedStackPartial(
                from = PartialStackComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("missing-key")))
                ),
                using = mapOf(
                    ColorAlias("existing-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))
                )
            )

            // Assert
            assert(actualResult.isError)
        }

        @Test
        fun `Should create successfully if the ColorAlias is found`() {
            // Arrange, Act
            val actualResult = PresentedStackPartial(
                from = PartialStackComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("existing-key"))),
                ),
                using = mapOf(
                    ColorAlias("existing-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))
                )
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Test
        fun `Should create successfully if the PartialStackComponent has no ColorAlias`() {
            // Arrange, Act
            val actualResult = PresentedStackPartial(
                from = PartialStackComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                ),
                using = mapOf(
                    ColorAlias("existing-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
                )
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `Should create successfully if the PartialStackComponent has no ColorAlias, alias map is empty`() {
            // Arrange, Act
            val actualResult = PresentedStackPartial(
                from = PartialStackComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                ),
                using = emptyMap()
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using different aliases`() {
            // Arrange
            val expectedLightColor = Color.Red
            val expectedDarkColor = Color.Cyan
            val partial = PartialStackComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(ColorAlias("existing-light-key")),
                    dark = ColorInfo.Alias(ColorAlias("existing-dark-key"))
                ),
            )
            val colorAliases = mapOf(
                ColorAlias("existing-light-key") to ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                    dark = ColorInfo.Hex(Color.Blue.toArgb())
                ),
                ColorAlias("existing-dark-key") to ColorScheme(
                    light = ColorInfo.Hex(Color.Yellow.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkColor.toArgb())
                ),
            )
            val expected = PresentedStackPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedLightColor),
                    dark = ColorStyle.Solid(expectedDarkColor),
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedStackPartial(
                from = partial,
                using = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using different aliases - aliased scheme only has light`() {
            // Arrange
            val expectedLightColor = Color.Red
            val expectedDarkColor = Color.Cyan
            val partial = PartialStackComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(ColorAlias("existing-light-key")),
                    dark = ColorInfo.Alias(ColorAlias("existing-dark-key"))
                ),
            )
            val colorAliases = mapOf(
                ColorAlias("existing-light-key") to ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                ),
                ColorAlias("existing-dark-key") to ColorScheme(
                    light = ColorInfo.Hex(expectedDarkColor.toArgb()),
                ),
            )
            val expected = PresentedStackPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedLightColor),
                    dark = ColorStyle.Solid(expectedDarkColor),
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedStackPartial(
                from = partial,
                using = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using the same alias`() {
            // Arrange
            val expectedLightColor = Color.Red
            val expectedDarkColor = Color.Cyan
            val partial = PartialStackComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(ColorAlias("existing-key")),
                    dark = ColorInfo.Alias(ColorAlias("existing-key"))
                ),
            )
            val colorAliases = mapOf(
                ColorAlias("existing-key") to ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkColor.toArgb())
                ),
            )
            val expected = PresentedStackPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedLightColor),
                    dark = ColorStyle.Solid(expectedDarkColor),
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedStackPartial(
                from = partial,
                using = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using the same alias - aliased scheme only has light`() {
            // Arrange
            val expectedColor = Color.Red
            val partial = PartialStackComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(ColorAlias("existing-key")),
                    dark = ColorInfo.Alias(ColorAlias("existing-key"))
                ),
            )
            val colorAliases = mapOf(
                ColorAlias("existing-key") to ColorScheme(
                    light = ColorInfo.Hex(expectedColor.toArgb()),
                ),
            )
            val expected = PresentedStackPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedColor),
                    dark = ColorStyle.Solid(expectedColor),
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedStackPartial(
                from = partial,
                using = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }
    }
}
