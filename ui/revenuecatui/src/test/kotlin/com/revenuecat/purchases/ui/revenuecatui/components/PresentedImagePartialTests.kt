package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
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
        val missingColorKey = ColorAlias("missing-color-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.MissingColorAlias(missingColorKey),
        )

        // Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Alias(missingColorKey)),
            ),
            sources = null,
            aliases = mapOf(
                ColorAlias("existing-color-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
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
        val firstColorKey = ColorAlias("first-color-key")
        val secondColorKey = ColorAlias("second-color-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.AliasedColorIsAlias(firstColorKey, secondColorKey),
        )

        // Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Alias(firstColorKey)),
            ),
            sources = null,
            aliases = mapOf(
                firstColorKey to ColorScheme(light = ColorInfo.Alias(secondColorKey)),
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
        val existingColorKey = ColorAlias("existing-color-key")
        val expectedColor = Color.Red

        // Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Alias(existingColorKey)),
            ),
            sources = null,
            aliases = mapOf(
                existingColorKey to ColorScheme(light = ColorInfo.Hex(expectedColor.toArgb())),
            )
        )

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        val actualColor = actual.overlay?.light ?: error("Actual color is null")
        actualColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedColor)
        }
    }

    @Test
    fun `Should create successfully if the PartialImageComponent has no ColorAlias`() {
        // Arrange, Act
        val actualResult = PresentedImagePartial(
            from = PartialImageComponent(
                colorOverlay = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
            ),
            sources = null,
            aliases = mapOf(
                ColorAlias("existing-color-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
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
            ),
            sources = null,
            aliases = emptyMap()
        )

        // Assert
        assert(actualResult.isSuccess)
    }
}
