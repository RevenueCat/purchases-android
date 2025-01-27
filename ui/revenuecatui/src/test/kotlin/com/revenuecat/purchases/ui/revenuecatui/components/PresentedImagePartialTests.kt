package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import org.junit.Test

internal class PresentedImagePartialTests {
    
    @Test
    fun `Should accumulate errors if the ColorAlias is not found`() {
        // Arrange
        val missingColorOverlayKey = ColorAlias("missing-color-overlay-key")
        val missingBorderKey = ColorAlias("missing-border-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.MissingColorAlias(missingColorOverlayKey),
            PaywallValidationError.MissingColorAlias(missingBorderKey),
        )

        // Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Alias(missingColorOverlayKey)),
                border = Border(color = ColorScheme(light = ColorInfo.Alias(missingBorderKey)), width = 2.0)
            ),
            sources = null,
            aliases = mapOf(
                ColorAlias("existing-color-overlay-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                ColorAlias("existing-border-key") to ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())),
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
        val firstColorOverlayKey = ColorAlias("first-color-overlay-key")
        val secondColorOverlayKey = ColorAlias("second-color-overlay-key")
        val firstBorderKey = ColorAlias("first-border-key")
        val secondBorderKey = ColorAlias("second-border-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.AliasedColorIsAlias(firstColorOverlayKey, secondColorOverlayKey),
            PaywallValidationError.AliasedColorIsAlias(firstBorderKey, secondBorderKey),
        )

        // Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Alias(firstColorOverlayKey)),
                border = Border(color = ColorScheme(light = ColorInfo.Alias(firstBorderKey)), width = 2.0)
            ),
            sources = null,
            aliases = mapOf(
                firstColorOverlayKey to ColorScheme(light = ColorInfo.Alias(secondColorOverlayKey)),
                firstBorderKey to ColorScheme(light = ColorInfo.Alias(secondBorderKey)),
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
        val existingColorOverlayKey = ColorAlias("existing-color-overlay-key")
        val existingBorderKey = ColorAlias("existing-border-key")
        val expectedOverlayColor = Color.Red
        val expectedBorderColor = Color.Green

        // Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Alias(existingColorOverlayKey)),
                border = Border(color = ColorScheme(light = ColorInfo.Alias(existingBorderKey)), width = 2.0)
            ),
            sources = null,
            aliases = mapOf(
                existingColorOverlayKey to ColorScheme(light = ColorInfo.Hex(expectedOverlayColor.toArgb())),
                existingBorderKey to ColorScheme(light = ColorInfo.Hex(expectedBorderColor.toArgb())),
            )
        )

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        val actualOverlayColor = actual.overlay?.light ?: error("Actual color is null")
        val actualBorderColor = actual.border?.colors?.light ?: error("Actual border color is null")
        actualOverlayColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedOverlayColor)
        }
        actualBorderColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedBorderColor)
        }
    }

    @Test
    fun `Should create successfully if the PartialImageComponent has no ColorAlias`() {
        // Arrange, Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())), width = 2.0)
            ),
            sources = null,
            aliases = mapOf(
                ColorAlias("existing-color-overlay-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
            )
        )

        // Assert
        assert(actualResult.isSuccess)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Should create successfully if the PartialImageComponent has no ColorAlias, alias map is empty`() {
        // Arrange, Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                border = Border(color = ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())), width = 2.0)
            ),
            sources = null,
            aliases = emptyMap()
        )

        // Assert
        assert(actualResult.isSuccess)
    }
}
