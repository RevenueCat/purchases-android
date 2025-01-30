package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.PartialCarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import org.junit.Test

internal class PresentedCarouselPartialTests {

    @Test
    fun `Should accumulate errors if the ColorAlias is not found`() {
        // Arrange
        val missingBackgroundKey = ColorAlias("missing-background-key")
        val missingBorderKey = ColorAlias("missing-border-key")
        val missingShadowKey = ColorAlias("missing-shadow-key")
        val missingActiveIndicatorKey = ColorAlias("missing-active-indicator-key")
        val missingDefaultIndicatorKey = ColorAlias("missing-default-indicator-key")

        val expected = nonEmptyListOf(
            PaywallValidationError.MissingColorAlias(missingBackgroundKey),
            PaywallValidationError.MissingColorAlias(missingBorderKey),
            PaywallValidationError.MissingColorAlias(missingShadowKey),
            PaywallValidationError.MissingColorAlias(missingActiveIndicatorKey),
            PaywallValidationError.MissingColorAlias(missingDefaultIndicatorKey),
        )

        // Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                backgroundColor = ColorScheme(light = ColorInfo.Alias(missingBackgroundKey)),
                border = Border(color = ColorScheme(light = ColorInfo.Alias(missingBorderKey)), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Alias(missingShadowKey)), radius = 2.0, x = 2.0, y = 2.0
                ),
                pageControl = CarouselComponent.PageControl(
                    alignment = VerticalAlignment.TOP,
                    active = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Alias(missingActiveIndicatorKey)),
                        margin = Padding.zero,
                    ),
                    default = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Alias(missingDefaultIndicatorKey)),
                        margin = Padding.zero,
                    )
                )
            ),
            aliases = mapOf(
                ColorAlias("existing-background-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                ColorAlias("existing-border-key") to ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())),
                ColorAlias("existing-shadow-key") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                ColorAlias("existing-active-indicator-key") to ColorScheme(
                    light = ColorInfo.Hex(Color.Cyan.toArgb())
                ),
                ColorAlias("existing-default-indicator-key") to ColorScheme(
                    light = ColorInfo.Hex(Color.Black.toArgb())
                ),
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
        val firstActiveIndicatorKey = ColorAlias("first-active-indicator-key")
        val firstDefaultIndicatorKey = ColorAlias("first-default-indicator-key")
        val secondBackgroundKey = ColorAlias("second-background-key")
        val secondBorderKey = ColorAlias("second-border-key")
        val secondShadowKey = ColorAlias("second-shadow-key")
        val secondActiveIndicatorKey = ColorAlias("second-active-indicator-key")
        val secondDefaultIndicatorKey = ColorAlias("second-default-indicator-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.AliasedColorIsAlias(firstBackgroundKey, secondBackgroundKey),
            PaywallValidationError.AliasedColorIsAlias(firstBorderKey, secondBorderKey),
            PaywallValidationError.AliasedColorIsAlias(firstShadowKey, secondShadowKey),
            PaywallValidationError.AliasedColorIsAlias(firstActiveIndicatorKey, secondActiveIndicatorKey),
            PaywallValidationError.AliasedColorIsAlias(firstDefaultIndicatorKey, secondDefaultIndicatorKey),
        )

        // Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                backgroundColor = ColorScheme(light = ColorInfo.Alias(firstBackgroundKey)),
                border = Border(color = ColorScheme(light = ColorInfo.Alias(firstBorderKey)), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Alias(firstShadowKey)), radius = 2.0, x = 2.0, y = 2.0
                ),
                pageControl = CarouselComponent.PageControl(
                    alignment = VerticalAlignment.TOP,
                    active = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Alias(firstActiveIndicatorKey)),
                        margin = Padding.zero,
                    ),
                    default = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Alias(firstDefaultIndicatorKey)),
                        margin = Padding.zero,
                    )
                )
            ),
            aliases = mapOf(
                firstBackgroundKey to ColorScheme(light = ColorInfo.Alias(secondBackgroundKey)),
                firstBorderKey to ColorScheme(light = ColorInfo.Alias(secondBorderKey)),
                firstShadowKey to ColorScheme(light = ColorInfo.Alias(secondShadowKey)),
                firstActiveIndicatorKey to ColorScheme(light = ColorInfo.Alias(secondActiveIndicatorKey)),
                firstDefaultIndicatorKey to ColorScheme(light = ColorInfo.Alias(secondDefaultIndicatorKey)),
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
        val existingActiveIndicatorKey = ColorAlias("existing-active-indicator-key")
        val existingDefaultIndicatorKey = ColorAlias("existing-default-indicator-key")
        val expectedBackgroundColor = Color.Red
        val expectedBorderColor = Color.Green
        val expectedShadowColor = Color.Yellow
        val expectedActiveIndicatorColor = Color.Cyan
        val expectedDefaultIndicatorColor = Color.Black

        // Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                backgroundColor = ColorScheme(light = ColorInfo.Alias(existingBackgroundKey)),
                border = Border(color = ColorScheme(light = ColorInfo.Alias(existingBorderKey)), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Alias(existingShadowKey)), radius = 2.0, x = 2.0, y = 2.0
                ),
                pageControl = CarouselComponent.PageControl(
                    alignment = VerticalAlignment.TOP,
                    active = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Alias(existingActiveIndicatorKey)),
                        margin = Padding.zero,
                    ),
                    default = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Alias(existingDefaultIndicatorKey)),
                        margin = Padding.zero,
                    )
                )
            ),
            aliases = mapOf(
                existingBackgroundKey to ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb())),
                existingBorderKey to ColorScheme(light = ColorInfo.Hex(expectedBorderColor.toArgb())),
                existingShadowKey to ColorScheme(light = ColorInfo.Hex(expectedShadowColor.toArgb())),
                existingActiveIndicatorKey to ColorScheme(light = ColorInfo.Hex(expectedActiveIndicatorColor.toArgb())),
                existingDefaultIndicatorKey to ColorScheme(
                    light = ColorInfo.Hex(expectedDefaultIndicatorColor.toArgb())
                ),
            )
        )

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        val actualBackgroundColor = actual.backgroundColorStyles?.light ?: error("Actual background color is null")
        val actualBorderColor = actual.borderStyles?.colors?.light ?: error("Actual border color is null")
        val actualShadowColor = actual.shadowStyles?.colors?.light ?: error("Actual shadow color is null")
        val actualActiveIndicatorColor = actual.pageControlStyles?.active?.color?.light
            ?: error("Actual active indicator color is null")
        val actualDefaultIndicatorColor = actual.pageControlStyles?.default?.color?.light
            ?: error("Actual default indicator color is null")
        actualBackgroundColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedBackgroundColor)
        }
        actualBorderColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedBorderColor)
        }
        actualShadowColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedShadowColor)
        }
        actualActiveIndicatorColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedActiveIndicatorColor)
        }
        actualDefaultIndicatorColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedDefaultIndicatorColor)
        }
    }

    @Test
    fun `Should create successfully if the PartialCarouselComponent has no ColorAlias`() {
        // Arrange, Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())), radius = 2.0, x = 2.0, y = 2.0
                ),
                pageControl = CarouselComponent.PageControl(
                    alignment = VerticalAlignment.TOP,
                    active = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                        margin = Padding.zero,
                    ),
                    default = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                        margin = Padding.zero,
                    )
                )
            ),
            aliases = mapOf(
                ColorAlias("existing-color-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
            )
        )

        // Assert
        assert(actualResult.isSuccess)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Should create successfully if the PartialCarouselComponent has no ColorAlias, alias map is empty`() {
        // Arrange, Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())), radius = 2.0, x = 2.0, y = 2.0
                ),
                pageControl = CarouselComponent.PageControl(
                    alignment = VerticalAlignment.TOP,
                    active = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                        margin = Padding.zero,
                    ),
                    default = CarouselComponent.PageControl.Indicator(
                        size = Size(width = SizeConstraint.Fixed(10u), height = SizeConstraint.Fixed(10u)),
                        spacing = 2f,
                        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                        margin = Padding.zero,
                    )
                )
            ),
            aliases = emptyMap()
        )

        // Assert
        assert(actualResult.isSuccess)
    }
}
