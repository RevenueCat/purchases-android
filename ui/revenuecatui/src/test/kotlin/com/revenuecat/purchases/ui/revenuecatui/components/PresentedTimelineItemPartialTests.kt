package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialTimelineComponentItem
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import org.junit.Test

internal class PresentedTimelineItemPartialTests {
    
    @Test
    fun `Should accumulate errors if the ColorAlias is not found in the PresentedTimelineItemPartial`() {
        // Arrange
        val missingConnectorColorKey = ColorAlias("missing-connector-color-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.MissingColorAlias(missingConnectorColorKey),
        )

        // Act
        val actualResult = PresentedTimelineItemPartial(
            from = PartialTimelineComponentItem(
                connector = TimelineComponent.Connector(
                    width = 23,
                    margin = Padding.zero,
                    color = ColorScheme(light = ColorInfo.Alias(missingConnectorColorKey))
                ),
            ),
            aliases = mapOf(
                ColorAlias("existing-color-overlay-key") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                ColorAlias("existing-border-key") to ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb())),
                ColorAlias("existing-shadow-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
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
        val firstConnectorColorKey = ColorAlias("first-connector-color-key")
        val secondConnectorColorKey = ColorAlias("second-connector-color-key")
        val expected = nonEmptyListOf(
            PaywallValidationError.AliasedColorIsAlias(firstConnectorColorKey, secondConnectorColorKey),
        )

        // Act
        val actualResult = PresentedTimelineItemPartial(
            from = PartialTimelineComponentItem(
                connector = TimelineComponent.Connector(
                    width = 23,
                    margin = Padding.zero,
                    color = ColorScheme(light = ColorInfo.Alias(firstConnectorColorKey))
                ),
            ),
            aliases = mapOf(
                firstConnectorColorKey to ColorScheme(light = ColorInfo.Alias(secondConnectorColorKey)),
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
        val existingConnectorColorKey = ColorAlias("existing-color-overlay-key")
        val expectedConnectorColor = Color.Red

        // Act
        val actualResult = PresentedTimelineItemPartial(
            from = PartialTimelineComponentItem(
                connector = TimelineComponent.Connector(
                    width = 23,
                    margin = Padding.zero,
                    color = ColorScheme(light = ColorInfo.Alias(existingConnectorColorKey))
                ),
            ),
            aliases = mapOf(
                existingConnectorColorKey to ColorScheme(light = ColorInfo.Hex(expectedConnectorColor.toArgb())),
            )
        )

        // Assert
        assert(actualResult.isSuccess)
        val actual = actualResult.getOrThrow()
        val actualOverlayColor = actual.connectorStyle?.color?.light ?: error("Actual color is null")
        actualOverlayColor.let { it as ColorStyle.Solid }.also {
            assert(it.color == expectedConnectorColor)
        }
    }

    @Test
    fun `Should create successfully if the PartialTimelineItemComponent has no ColorAlias`() {
        // Arrange, Act
        val actualResult = PresentedTimelineItemPartial(
            from = PartialTimelineComponentItem(
                connector = TimelineComponent.Connector(
                    width = 23,
                    margin = Padding.zero,
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))
                ),
            ),
            aliases = mapOf(
                ColorAlias("existing-color-overlay-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
            )
        )

        // Assert
        assert(actualResult.isSuccess)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Should create successfully if the PartialTimelineItemComponent has no ColorAlias, alias map is empty`() {
        // Arrange, Act
        val actualResult = PresentedTimelineItemPartial(
            from = PartialTimelineComponentItem(
                connector = TimelineComponent.Connector(
                    width = 23,
                    margin = Padding.zero,
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))
                ),
            ),
            aliases = emptyMap()
        )

        // Assert
        assert(actualResult.isSuccess)
    }
}
