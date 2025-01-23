package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import org.junit.Test

class ColorStyleTests {

    @Test
    fun `Should properly combine light and dark ColorAliases using different aliases`() {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Cyan
        val colorScheme = ColorScheme(
            light = ColorInfo.Alias(ColorAlias("existing-light-key")),
            dark = ColorInfo.Alias(ColorAlias("existing-dark-key"))
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
        val expected = ColorStyles(
            light = ColorStyle.Solid(expectedLightColor),
            dark = ColorStyle.Solid(expectedDarkColor),
        )

        // Act
        val actualResult = colorScheme.toColorStyles(aliases = colorAliases)

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
        val colorScheme = ColorScheme(
            light = ColorInfo.Alias(ColorAlias("existing-light-key")),
            dark = ColorInfo.Alias(ColorAlias("existing-dark-key"))
        )
        val colorAliases = mapOf(
            ColorAlias("existing-light-key") to ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
            ),
            ColorAlias("existing-dark-key") to ColorScheme(
                light = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )
        val expected = ColorStyles(
            light = ColorStyle.Solid(expectedLightColor),
            dark = ColorStyle.Solid(expectedDarkColor),
        )

        // Act
        val actualResult = colorScheme.toColorStyles(aliases = colorAliases)

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
        val colorScheme = ColorScheme(
            light = ColorInfo.Alias(ColorAlias("existing-key")),
            dark = ColorInfo.Alias(ColorAlias("existing-key"))
        )
        val colorAliases = mapOf(
            ColorAlias("existing-key") to ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb())
            ),
        )
        val expected = ColorStyles(
            light = ColorStyle.Solid(expectedLightColor),
            dark = ColorStyle.Solid(expectedDarkColor),
        )

        // Act
        val actualResult = colorScheme.toColorStyles(aliases = colorAliases)

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        assert(actual == expected)
    }

    @Test
    fun `Should properly combine light and dark ColorAliases using the same alias - aliased scheme only has light`() {
        // Arrange
        val expectedColor = Color.Red
        val colorScheme = ColorScheme(
            light = ColorInfo.Alias(ColorAlias("existing-key")),
            dark = ColorInfo.Alias(ColorAlias("existing-key"))
        )
        val colorAliases = mapOf(
            ColorAlias("existing-key") to ColorScheme(
                light = ColorInfo.Hex(expectedColor.toArgb()),
            ),
        )
        val expected = ColorStyles(
            light = ColorStyle.Solid(expectedColor),
            dark = ColorStyle.Solid(expectedColor),
        )

        // Act
        val actualResult = colorScheme.toColorStyles(aliases = colorAliases)

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        assert(actual == expected)
    }

    @Test
    fun `Should fail to combine if the ColorAlias is missing`() {
        // Arrange
        val missingKey = ColorAlias("missing-key")
        val colorScheme = ColorScheme(light = ColorInfo.Alias(missingKey),)
        val colorAliases = emptyMap<ColorAlias, ColorScheme>()
        val expected = nonEmptyListOf(PaywallValidationError.MissingColorAlias(missingKey))

        // Act
        val actualResult = colorScheme.toColorStyles(aliases = colorAliases)

        // Assert
        assert(actualResult.isError)
        val actual = actualResult.errorOrNull()!!
        assert(actual == expected)
    }

    @Test
    fun `Should fail to combine if the ColorAlias points to another alias`() {
        // Arrange
        val incorrectKey = ColorAlias("incorrect-key")
        val otherKey = ColorAlias("other-key")
        val colorScheme = ColorScheme(light = ColorInfo.Alias(incorrectKey))
        // Map of ColorAliases, which points to a ColorScheme containing a ColorInfo.Alias, which is wrong.
        val colorAliases = mapOf(incorrectKey to ColorScheme(light = ColorInfo.Alias(otherKey)))
        val expected = nonEmptyListOf(PaywallValidationError.AliasedColorIsAlias(incorrectKey, otherKey))

        // Act
        val actualResult = colorScheme.toColorStyles(aliases = colorAliases)

        // Assert
        assert(actualResult.isError)
        val actual = actualResult.errorOrNull()!!
        assert(actual == expected)
    }
}
