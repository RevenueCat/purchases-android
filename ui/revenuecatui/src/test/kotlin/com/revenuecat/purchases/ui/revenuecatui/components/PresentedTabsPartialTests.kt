package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialTabsComponent
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class PresentedTabsPartialTests {

    @RunWith(Parameterized::class)
    class CombinePresentedTabsPartialTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            val base: PresentedTabsPartial,
            val override: PresentedTabsPartial?,
            val expected: PresentedTabsPartial,
        )

        companion object {

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "Should properly override all properties if they are non-null in both",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow()
                    )
                ),
                arrayOf(
                    "Should not override anything if all properties are null in both",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = null,
                                size = null,
                                backgroundColor = null,
                                padding = null,
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = null,
                                size = null,
                                backgroundColor = null,
                                padding = null,
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = null,
                                size = null,
                                backgroundColor = null,
                                padding = null,
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should not override anything if override is null",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = null,
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the first 6 individual properties if they are non-null in both",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the first 6 individual properties if they are null in the base",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = null,
                                size = null,
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(20u), height = Fixed(20u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the second 5 individual properties if they are non-null in both",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = null,
                                size = null,
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the second 5 individual properties if they are null in the base",
                    Args(
                        base = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = null,
                                shape = null,
                                border = null,
                                shadow = null,
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = null,
                                size = null,
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = PresentedTabsPartial(
                            from = PartialTabsComponent(
                                visible = true,
                                size = Size(width = Fixed(10u), height = Fixed(10u)),
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
                            ),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
            )
        }

        @Test
        fun `Should properly combine PresentedTabsPartials`() {
            // Arrange, Act
            val actual = args.base.combine(args.override)

            // Assert
            assertEquals(args.expected, actual)
        }
    }

    class CreatePresentedTabsPartial {

        @Test
        fun `Should accumulate errors if the ColorAlias is not found`() {
            // Arrange
            val missingBackgroundKey = ColorAlias("missing-background-key")
            val missingBorderKey = ColorAlias("missing-border-key")
            val missingShadowKey = ColorAlias("missing-shadow-key")
            val expected = nonEmptyListOf(
                PaywallValidationError.MissingColorAlias(missingBackgroundKey),
                PaywallValidationError.MissingColorAlias(missingBorderKey),
                PaywallValidationError.MissingColorAlias(missingShadowKey),
            )

            // Act
            val actualResult = PresentedTabsPartial(
                from = PartialTabsComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(missingBackgroundKey)),
                    border = Border(color = ColorScheme(light = ColorInfo.Alias(missingBorderKey)), width = 2.0),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Alias(missingShadowKey)),
                        radius = 2.0,
                        x = 2.0,
                        y = 2.0
                    )
                ),
                aliases = mapOf(
                    ColorAlias("existing-background-key") to ColorScheme(ColorInfo.Hex(Color.Red.toArgb())),
                    ColorAlias("existing-border-key") to ColorScheme(ColorInfo.Hex(Color.Blue.toArgb())),
                    ColorAlias("existing-shadow-key") to ColorScheme(ColorInfo.Hex(Color.Yellow.toArgb())),
                )
            )

            // Assert
            assert(actualResult.isError)
            val actual = actualResult.errorOrNull()!!
            assert(actual == expected)
        }

        @Test
        fun `Should accumulate errors if the ColorAlias points to another alias`() {
            // Arrange
            val firstBackgroundKey = ColorAlias("first-background-key")
            val firstBorderKey = ColorAlias("first-border-key")
            val firstShadowKey = ColorAlias("first-shadow-key")
            val secondBackgroundKey = ColorAlias("second-background-key")
            val secondBorderKey = ColorAlias("second-border-key")
            val secondShadowKey = ColorAlias("second-shadow-key")
            val expected = nonEmptyListOf(
                PaywallValidationError.AliasedColorIsAlias(firstBackgroundKey, secondBackgroundKey),
                PaywallValidationError.AliasedColorIsAlias(firstBorderKey, secondBorderKey),
                PaywallValidationError.AliasedColorIsAlias(firstShadowKey, secondShadowKey),
            )

            // Act
            val actualResult = PresentedTabsPartial(
                from = PartialTabsComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(firstBackgroundKey)),
                    border = Border(color = ColorScheme(light = ColorInfo.Alias(firstBorderKey)), width = 2.0),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Alias(firstShadowKey)),
                        radius = 2.0,
                        x = 2.0,
                        y = 2.0
                    )
                ),
                aliases = mapOf(
                    firstBackgroundKey to ColorScheme(light = ColorInfo.Alias(secondBackgroundKey)),
                    firstBorderKey to ColorScheme(light = ColorInfo.Alias(secondBorderKey)),
                    firstShadowKey to ColorScheme(light = ColorInfo.Alias(secondShadowKey)),
                )
            )

            // Assert
            assert(actualResult.isError)
            val actual = actualResult.errorOrNull()!!
            assert(actual == expected)
        }

        @Test
        fun `Should create successfully if the ColorAlias is found`() {
            // Arrange
            val existingBackgroundKey = ColorAlias("existing-background-key")
            val existingBorderKey = ColorAlias("existing-border-key")
            val existingShadowKey = ColorAlias("existing-shadow-key")
            val expectedBackgroundColor = Color.Red
            val expectedBorderColor = Color.Blue
            val expectedShadowColor = Color.Yellow
            // Act
            val actualResult = PresentedTabsPartial(
                from = PartialTabsComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(existingBackgroundKey)),
                    border = Border(color = ColorScheme(light = ColorInfo.Alias(existingBorderKey)), width = 2.0),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Alias(existingShadowKey)),
                        radius = 2.0,
                        x = 2.0,
                        y = 2.0
                    )
                ),
                aliases = mapOf(
                    existingBackgroundKey to ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb())),
                    existingBorderKey to ColorScheme(light = ColorInfo.Hex(expectedBorderColor.toArgb())),
                    existingShadowKey to ColorScheme(light = ColorInfo.Hex(expectedShadowColor.toArgb())),
                )
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            val actualBackgroundColor = actual.backgroundColorStyles?.light ?: error("Actual background color is null")
            val actualBorderColor = actual.borderStyles?.colors?.light ?: error("Actual border color is null")
            val actualShadowColor = actual.shadowStyles?.colors?.light ?: error("Actual shadow color is null")
            actualBackgroundColor.let { it as ColorStyle.Solid }.also {
                assert(it.color == expectedBackgroundColor)
            }
            actualBorderColor.let { it as ColorStyle.Solid }.also {
                assert(it.color == expectedBorderColor)
            }
            actualShadowColor.let { it as ColorStyle.Solid }.also {
                assert(it.color == expectedShadowColor)
            }
        }

        @Test
        fun `Should create successfully if the PartialTabsComponent has no ColorAlias`() {
            // Arrange, Act
            val actualResult = PresentedTabsPartial(
                from = PartialTabsComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                    border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())), width = 2.0),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                        radius = 2.0,
                        x = 2.0,
                        y = 2.0
                    )
                ),
                aliases = mapOf(
                    ColorAlias("existing-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
                )
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `Should create successfully if the PartialTabsComponent has no ColorAlias, alias map is empty`() {
            // Arrange, Act
            val actualResult = PresentedTabsPartial(
                from = PartialTabsComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                    border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())), width = 2.0),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                        radius = 2.0,
                        x = 2.0,
                        y = 2.0
                    ),
                ),
                aliases = emptyMap()
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using different aliases`() {
            // Arrange
            val expectedLightBackgroundColor = Color.Red
            val expectedDarkBackgroundColor = Color.Cyan
            val expectedLightBorderColor = Color.Blue
            val expectedDarkBorderColor = Color.Yellow
            val expectedLightShadowColor = Color.Black
            val expectedDarkShadowColor = Color.White
            val lightBackgroundKey = ColorAlias("existing-light-background-key")
            val darkBackgroundKey = ColorAlias("existing-dark-background-key")
            val lightBorderColorKey = ColorAlias("existing-light-border-key")
            val darkBorderColorKey = ColorAlias("existing-dark-border-key")
            val lightShadowKey = ColorAlias("existing-light-shadow-key")
            val darkShadowKey = ColorAlias("existing-dark-shadow-key")
            val partial = PartialTabsComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(lightBackgroundKey),
                    dark = ColorInfo.Alias(darkBackgroundKey)
                ),
                border = Border(
                    color = ColorScheme(
                        light = ColorInfo.Alias(lightBorderColorKey),
                        dark = ColorInfo.Alias(darkBorderColorKey),
                    ),
                    width = 2.0,
                ),
                shadow = Shadow(
                    color = ColorScheme(
                        light = ColorInfo.Alias(lightShadowKey),
                        dark = ColorInfo.Alias(darkShadowKey),
                    ),
                    radius = 2.0,
                    x = 2.0,
                    y = 2.0
                ),
            )
            val colorAliases = mapOf(
                lightBackgroundKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightBackgroundColor.toArgb()),
                    dark = ColorInfo.Hex(Color.Blue.toArgb())
                ),
                darkBackgroundKey to ColorScheme(
                    light = ColorInfo.Hex(Color.Yellow.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkBackgroundColor.toArgb())
                ),
                lightBorderColorKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightBorderColor.toArgb()),
                    dark = ColorInfo.Hex(Color.Black.toArgb())
                ),
                darkBorderColorKey to ColorScheme(
                    light = ColorInfo.Hex(Color.White.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkBorderColor.toArgb())
                ),
                lightShadowKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightShadowColor.toArgb()),
                    dark = ColorInfo.Hex(Color.Red.toArgb())
                ),
                darkShadowKey to ColorScheme(
                    light = ColorInfo.Hex(Color.DarkGray.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkShadowColor.toArgb())
                ),
            )
            val expected = PresentedTabsPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedLightBackgroundColor),
                    dark = ColorStyle.Solid(expectedDarkBackgroundColor),
                ),
                borderStyles = BorderStyles(
                    width = 2.dp,
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedLightBorderColor),
                        dark = ColorStyle.Solid(expectedDarkBorderColor),
                    ),
                ),
                shadowStyles = ShadowStyles(
                    radius = 2.dp,
                    x = 2.dp,
                    y = 2.dp,
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedLightShadowColor),
                        dark = ColorStyle.Solid(expectedDarkShadowColor),
                    ),
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedTabsPartial(
                from = partial,
                aliases = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using different aliases - aliased scheme only has light`() {
            // Arrange
            val expectedLightBackgroundColor = Color.Red
            val expectedDarkBackgroundColor = Color.Cyan
            val expectedLightBorderColor = Color.Blue
            val expectedDarkBorderColor = Color.Yellow
            val expectedLightShadowColor = Color.Black
            val expectedDarkShadowColor = Color.White
            val lightBackgroundKey = ColorAlias("existing-light-background-key")
            val darkBackgroundKey = ColorAlias("existing-dark-background-key")
            val lightBorderColorKey = ColorAlias("existing-light-border-key")
            val darkBorderColorKey = ColorAlias("existing-dark-border-key")
            val lightShadowKey = ColorAlias("existing-light-shadow-key")
            val darkShadowKey = ColorAlias("existing-dark-shadow-key")
            val partial = PartialTabsComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(lightBackgroundKey),
                    dark = ColorInfo.Alias(darkBackgroundKey)
                ),
                border = Border(
                    color = ColorScheme(
                        light = ColorInfo.Alias(lightBorderColorKey),
                        dark = ColorInfo.Alias(darkBorderColorKey),
                    ),
                    width = 2.0,
                ),
                shadow = Shadow(
                    color = ColorScheme(
                        light = ColorInfo.Alias(lightShadowKey),
                        dark = ColorInfo.Alias(darkShadowKey),
                    ),
                    radius = 2.0,
                    x = 2.0,
                    y = 2.0
                ),
            )
            val colorAliases = mapOf(
                lightBackgroundKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightBackgroundColor.toArgb()),
                ),
                darkBackgroundKey to ColorScheme(
                    light = ColorInfo.Hex(expectedDarkBackgroundColor.toArgb()),
                ),
                lightBorderColorKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightBorderColor.toArgb()),
                ),
                darkBorderColorKey to ColorScheme(
                    light = ColorInfo.Hex(expectedDarkBorderColor.toArgb()),
                ),
                lightShadowKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightShadowColor.toArgb()),
                ),
                darkShadowKey to ColorScheme(
                    light = ColorInfo.Hex(expectedDarkShadowColor.toArgb()),
                ),
            )
            val expected = PresentedTabsPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedLightBackgroundColor),
                    dark = ColorStyle.Solid(expectedDarkBackgroundColor),
                ),
                borderStyles = BorderStyles(
                    width = 2.dp,
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedLightBorderColor),
                        dark = ColorStyle.Solid(expectedDarkBorderColor),
                    ),
                ),
                shadowStyles = ShadowStyles(
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedLightShadowColor),
                        dark = ColorStyle.Solid(expectedDarkShadowColor),
                    ),
                    radius = 2.dp,
                    x = 2.dp,
                    y = 2.dp,
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedTabsPartial(
                from = partial,
                aliases = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using the same alias`() {
            // Arrange
            val expectedLightBackgroundColor = Color.Red
            val expectedDarkBackgroundColor = Color.Cyan
            val expectedLightBorderColor = Color.Blue
            val expectedDarkBorderColor = Color.Yellow
            val expectedLightShadowColor = Color.Black
            val expectedDarkShadowColor = Color.White
            val backgroundKey = ColorAlias("existing-background-key")
            val borderKey = ColorAlias("existing-border-key")
            val shadowKey = ColorAlias("existing-shadow-key")
            val partial = PartialTabsComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(backgroundKey),
                    dark = ColorInfo.Alias(backgroundKey)
                ),
                border = Border(
                    color = ColorScheme(
                        light = ColorInfo.Alias(borderKey),
                        dark = ColorInfo.Alias(borderKey)
                    ),
                    width = 2.0,
                ),
                shadow = Shadow(
                    color = ColorScheme(
                        light = ColorInfo.Alias(shadowKey),
                        dark = ColorInfo.Alias(shadowKey),
                    ),
                    radius = 2.0,
                    x = 2.0,
                    y = 2.0
                ),
            )
            val colorAliases = mapOf(
                backgroundKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightBackgroundColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkBackgroundColor.toArgb())
                ),
                borderKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightBorderColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkBorderColor.toArgb())
                ),
                shadowKey to ColorScheme(
                    light = ColorInfo.Hex(expectedLightShadowColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkShadowColor.toArgb())
                ),
            )
            val expected = PresentedTabsPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedLightBackgroundColor),
                    dark = ColorStyle.Solid(expectedDarkBackgroundColor),
                ),
                borderStyles = BorderStyles(
                    width = 2.dp,
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedLightBorderColor),
                        dark = ColorStyle.Solid(expectedDarkBorderColor),
                    ),
                ),
                shadowStyles = ShadowStyles(
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedLightShadowColor),
                        dark = ColorStyle.Solid(expectedDarkShadowColor),
                    ),
                    radius = 2.dp,
                    x = 2.dp,
                    y = 2.dp,
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedTabsPartial(
                from = partial,
                aliases = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }

        @Test
        fun `Should properly combine light and dark ColorAliases using the same alias - aliased scheme only has light`() {
            // Arrange
            val expectedBackgroundColor = Color.Red
            val expectedBorderColor = Color.Blue
            val expectedShadowColor = Color.Black
            val backgroundKey = ColorAlias("existing-background-key")
            val borderKey = ColorAlias("existing-border-key")
            val shadowKey = ColorAlias("existing-shadow-key")
            val partial = PartialTabsComponent(
                backgroundColor = ColorScheme(
                    light = ColorInfo.Alias(backgroundKey),
                    dark = ColorInfo.Alias(backgroundKey)
                ),
                border = Border(
                    color = ColorScheme(
                        light = ColorInfo.Alias(borderKey),
                        dark = ColorInfo.Alias(borderKey)
                    ),
                    width = 2.0,
                ),
                shadow = Shadow(
                    color = ColorScheme(
                        light = ColorInfo.Alias(shadowKey),
                        dark = ColorInfo.Alias(shadowKey),
                    ),
                    radius = 2.0,
                    x = 2.0,
                    y = 2.0
                ),
            )
            val colorAliases = mapOf(
                backgroundKey to ColorScheme(
                    light = ColorInfo.Hex(expectedBackgroundColor.toArgb()),
                ),
                borderKey to ColorScheme(
                    light = ColorInfo.Hex(expectedBorderColor.toArgb()),
                ),
                shadowKey to ColorScheme(
                    light = ColorInfo.Hex(expectedShadowColor.toArgb()),
                ),
            )
            val expected = PresentedTabsPartial(
                backgroundColorStyles = ColorStyles(
                    light = ColorStyle.Solid(expectedBackgroundColor),
                    dark = ColorStyle.Solid(expectedBackgroundColor),
                ),
                borderStyles = BorderStyles(
                    width = 2.dp,
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedBorderColor),
                        dark = ColorStyle.Solid(expectedBorderColor),
                    ),
                ),
                shadowStyles = ShadowStyles(
                    colors = ColorStyles(
                        light = ColorStyle.Solid(expectedShadowColor),
                        dark = ColorStyle.Solid(expectedShadowColor),
                    ),
                    radius = 2.dp,
                    x = 2.dp,
                    y = 2.dp,
                ),
                partial = partial,
            )

            // Act
            val actualResult = PresentedTabsPartial(
                from = partial,
                aliases = colorAliases,
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            assert(actual == expected)
        }
    }
}
