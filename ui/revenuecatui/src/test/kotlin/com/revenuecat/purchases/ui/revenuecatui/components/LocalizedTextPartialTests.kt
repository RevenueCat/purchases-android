package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class LocalizedTextPartialTests {

    @RunWith(Parameterized::class)
    class CombineLocalizedTextPartialTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

        class Args(
            val base: LocalizedTextPartial,
            val override: LocalizedTextPartial?,
            val expected: LocalizedTextPartial,
        )

        companion object {
            private val localeId = LocaleId("en_US")
            private val dummyLocalizationDictionary = nonEmptyMapOf(
                LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
            )

            @Suppress("LongMethod")
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "Should properly override all properties if they are non-null in both",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideBgColor"))),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("overrideColor") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                ColorAlias("overrideBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            ),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideBgColor"))),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("overrideColor") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                ColorAlias("overrideBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            ),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should not override anything if all properties are null in both",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should not override anything if override is null",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                        override = null,
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the first 6 individual properties if they are non-null in both",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideBgColor"))),
                                fontName = "overrideFont",
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("overrideColor") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                ColorAlias("overrideBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            ),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideBgColor"))),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("overrideColor") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                ColorAlias("overrideBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            ),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the first 6 individual properties if they are null in the base",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideBgColor"))),
                                fontName = "overrideFont",
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("overrideColor") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                ColorAlias("overrideBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            ),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("overrideBgColor"))),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("overrideColor") to ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                ColorAlias("overrideBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            ),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the second 6 individual properties if they are non-null in both",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                    )
                ),
                arrayOf(
                    "Should properly override the second 6 individual properties if they are null in the base",
                    Args(
                        base = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseColor"))),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias(ColorAlias("baseBgColor"))),
                                fontName = "baseFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            ),
                            aliases = mapOf(
                                ColorAlias("baseColor") to ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                ColorAlias("baseBgColor") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            ),
                        ).getOrThrow(),
                    )
                ),

                )
        }

        @Test
        fun `Should properly combine LocalizedTextPartials`() {
            // Arrange, Act
            val actual = args.base.combine(args.override)

            // Assert
            assertEquals(args.expected, actual)
        }
    }

    class CreateLocalizedTextPartialTests {

        companion object {
            private val localeId = LocaleId("en_US")
            private val dummyLocalizationDictionary = nonEmptyMapOf(
                LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
            )
        }

        @Test
        fun `Should fail to create if the LocalizationKey is not found`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    text = LocalizationKey("missing-key"),
                ),
                using = nonEmptyMapOf(
                    localeId to nonEmptyMapOf(
                        LocalizationKey("key") to LocalizationData.Text("Hello, world"),
                    )
                ),
                aliases = emptyMap(),
            )

            // Assert
            assert(actualResult.isError)
        }

        @Test
        fun `Should create successfully if the LocalizationKey is found`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    text = LocalizationKey("existing-key"),
                ),
                using = nonEmptyMapOf(
                    localeId to nonEmptyMapOf(
                        LocalizationKey("existing-key") to LocalizationData.Text("Hello, world"),
                    )
                ),
                aliases = emptyMap(),
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Test
        fun `Should create successfully if the PartialTextComponent has no LocalizationKey`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    text = null,
                ),
                using = nonEmptyMapOf(
                    localeId to nonEmptyMapOf(
                        LocalizationKey("existing-key") to LocalizationData.Text("Hello, world"),
                    )
                ),
                aliases = emptyMap(),
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `Should create successfully if the PartialTextComponent has no LocalizationKey, LocalizationDictionary is empty`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    text = null,
                ),
                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                aliases = emptyMap(),
            )

            // Assert
            assert(actualResult.isSuccess)
        }

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
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    color = ColorScheme(light = ColorInfo.Alias(missingColorKey)),
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(missingBackgroundKey)),
                ),
                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
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
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    color = ColorScheme(light = ColorInfo.Alias(firstColorKey)),
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(firstBackgroundKey)),
                ),
                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
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
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    color = ColorScheme(light = ColorInfo.Alias(existingColorKey)),
                    backgroundColor = ColorScheme(light = ColorInfo.Alias(existingBackgroundKey)),
                ),
                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                aliases = mapOf(
                    existingColorKey to ColorScheme(light = ColorInfo.Hex(expectedColor.toArgb())),
                    existingBackgroundKey to ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb()))
                )
            )

            // Assert
            assert(actualResult.isSuccess)
            val actual = actualResult.getOrThrow()
            val actualColor = actual.color?.light ?: error("Actual color is null")
            val actualBackgroundColor = actual.backgroundColor?.light ?: error("Actual background color is null")
            actualColor.let { it as ColorStyle.Solid }.also {
                assert(it.color == expectedColor)
            }
            actualBackgroundColor.let { it as ColorStyle.Solid }.also {
                assert(it.color == expectedBackgroundColor)
            }
        }

        @Test
        fun `Should create successfully if the PartialTextComponent has no ColorAlias`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                ),
                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                aliases = mapOf(
                    ColorAlias("existing-color-key") to ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))
                )
            )

            // Assert
            assert(actualResult.isSuccess)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `Should create successfully if the PartialTextComponent has no ColorAlias, alias map is empty`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                ),
                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                aliases = emptyMap()
            )

            // Assert
            assert(actualResult.isSuccess)
        }
        
    }
}
