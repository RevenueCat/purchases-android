package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.PartialIconComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import org.junit.Test

internal class PresentedIconPartialTests {

    @Test
    fun `Should accumulate errors if the ColorAlias is not found`() {
        // Arrange
        val missingColorKey = ColorAlias("missing-color-key")
        val missingBackgroundKey = ColorAlias("missing-background-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.MissingColorAlias(missingColorKey),
            PaywallValidationError.MissingColorAlias(missingBackgroundKey),
        )

        // Act
        val actualResult = PresentedIconPartial(
            from = PartialIconComponent(
                color = ColorScheme(light = ColorInfo.Alias(missingColorKey)),
                iconBackground = IconComponent.IconBackground(
                    color = ColorScheme(light = ColorInfo.Alias(missingBackgroundKey)),
                    shape = MaskShape.Circle,
                )
            ),
            aliases = mapOf(
                ColorAlias("existing-color-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                ColorAlias("existing-background-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
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
        val firstBackgroundKey = ColorAlias("first-background-key")
        val secondColorKey = ColorAlias("second-color-key")
        val secondBackgroundKey = ColorAlias("second-background-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.AliasedColorIsAlias(firstColorKey, secondColorKey),
            PaywallValidationError.AliasedColorIsAlias(firstBackgroundKey, secondBackgroundKey),
        )

        // Act
        val actualResult = PresentedIconPartial(
            from = PartialIconComponent(
                color = ColorScheme(light = ColorInfo.Alias(firstColorKey)),
                iconBackground = IconComponent.IconBackground(
                    color = ColorScheme(light = ColorInfo.Alias(firstBackgroundKey)),
                    shape = MaskShape.Circle,
                )
            ),
            aliases = mapOf(
                firstColorKey to ColorScheme(light = ColorInfo.Alias(secondColorKey)),
                firstBackgroundKey to ColorScheme(light = ColorInfo.Alias(secondBackgroundKey)),
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
        val existingBackgroundKey = ColorAlias("existing-background-key")
        val expectedColor = Color.Red
        val expectedBackgroundColor = Color.Blue

        // Act
        val actualResult = PresentedIconPartial(
            from = PartialIconComponent(
                color = ColorScheme(light = ColorInfo.Alias(existingColorKey)),
                iconBackground = IconComponent.IconBackground(
                    color = ColorScheme(light = ColorInfo.Alias(existingBackgroundKey)),
                    shape = MaskShape.Circle,
                )
            ),
            aliases = mapOf(
                existingColorKey to ColorScheme(light = ColorInfo.Hex(expectedColor.toArgb())),
                existingBackgroundKey to ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb()))
            )
        )

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        val actualColor = actual.colorStyles?.light ?: error("Actual color is null")
        val actualBackgroundColor = actual.background?.color?.light ?: error("Actual background color is null")
        actualColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedColor)
        }
        actualBackgroundColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedBackgroundColor)
        }
    }

    @Test
    fun `Should create successfully if the PartialIconComponent has no ColorAlias`() {
        // Arrange, Act
        val actualResult = PresentedIconPartial(
            from = PartialIconComponent(
                color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                iconBackground = IconComponent.IconBackground(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                    shape = MaskShape.Circle,
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
    fun `Should create successfully if the PartialIconComponent has no ColorAlias, alias map is empty`() {
        // Arrange, Act
        val actualResult = PresentedIconPartial(
            from = PartialIconComponent(
                color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                iconBackground = IconComponent.IconBackground(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                    shape = MaskShape.Circle,
                )
            ),
            aliases = emptyMap()
        )

        // Assert
        assert(actualResult.isSuccess)
    }
}
