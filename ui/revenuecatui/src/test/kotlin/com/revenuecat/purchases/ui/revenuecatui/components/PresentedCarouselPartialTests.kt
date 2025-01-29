package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialCarouselComponent
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shadow
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
        val missingBorderKey = ColorAlias("missing-border-key")
        val missingShadowKey = ColorAlias("missing-shadow-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.MissingColorAlias(missingBorderKey),
            PaywallValidationError.MissingColorAlias(missingShadowKey),
        )

        // Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                border = Border(color = ColorScheme(light = ColorInfo.Alias(missingBorderKey)), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Alias(missingShadowKey)), radius = 2.0, x = 2.0, y = 2.0
                ),
            ),
            aliases = mapOf(
                ColorAlias("existing-border-key") to ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())),
                ColorAlias("existing-shadow-key") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
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
        val firstBorderKey = ColorAlias("first-border-key")
        val firstShadowKey = ColorAlias("first-shadow-key")
        val secondBorderKey = ColorAlias("second-border-key")
        val secondShadowKey = ColorAlias("second-shadow-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.AliasedColorIsAlias(firstBorderKey, secondBorderKey),
            PaywallValidationError.AliasedColorIsAlias(firstShadowKey, secondShadowKey),
        )

        // Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                border = Border(color = ColorScheme(light = ColorInfo.Alias(firstBorderKey)), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Alias(firstShadowKey)), radius = 2.0, x = 2.0, y = 2.0
                ),
            ),
            aliases = mapOf(
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
        val existingBorderKey = ColorAlias("existing-border-key")
        val existingShadowKey = ColorAlias("existing-shadow-key")
        val expectedBorderColor = Color.Green
        val expectedShadowColor = Color.Yellow

        // Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                border = Border(color = ColorScheme(light = ColorInfo.Alias(existingBorderKey)), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Alias(existingShadowKey)), radius = 2.0, x = 2.0, y = 2.0
                ),
            ),
            aliases = mapOf(
                existingBorderKey to ColorScheme(light = ColorInfo.Hex(expectedBorderColor.toArgb())),
                existingShadowKey to ColorScheme(light = ColorInfo.Hex(expectedShadowColor.toArgb())),
            )
        )

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        val actualBorderColor = actual.borderStyles?.colors?.light ?: error("Actual border color is null")
        val actualShadowColor = actual.shadowStyles?.colors?.light ?: error("Actual shadow color is null")
        actualBorderColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedBorderColor)
        }
        actualShadowColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedShadowColor)
        }
    }

    @Test
    fun `Should create successfully if the PartialCarouselComponent has no ColorAlias`() {
        // Arrange, Act
        val actualResult = PresentedCarouselPartial(
            from = PartialCarouselComponent(
                border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())), radius = 2.0, x = 2.0, y = 2.0
                ),
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
                border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())), width = 2.0),
                shadow = Shadow(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())), radius = 2.0, x = 2.0, y = 2.0
                ),
            ),
            aliases = emptyMap()
        )

        // Assert
        assert(actualResult.isSuccess)
    }
}
