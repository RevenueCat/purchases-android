package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
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
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias("overrideColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("overrideBgColor")),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = FontSize.BODY_M,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            )
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias("overrideColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("overrideBgColor")),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = FontSize.BODY_M,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            )
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
                            using = nonEmptyMapOf(localeId to emptyMap())
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
                            using = nonEmptyMapOf(localeId to emptyMap())
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
                            using = nonEmptyMapOf(localeId to emptyMap())
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
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
                        ).getOrThrow(),
                        override = null,
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
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
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias("overrideColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("overrideBgColor")),
                                fontName = "overrideFont",
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            )
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias("overrideColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("overrideBgColor")),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            )
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
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(localeId to emptyMap()),
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias("overrideColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("overrideBgColor")),
                                fontName = "overrideFont",
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            )
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = false,
                                text = LocalizationKey("overrideKey"),
                                color = ColorScheme(light = ColorInfo.Alias("overrideColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("overrideBgColor")),
                                fontName = "overrideFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("overrideKey") to LocalizationData.Text("override"),
                                )
                            )
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
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.LIGHT,
                                fontSize = FontSize.BODY_S,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = FontSize.BODY_M,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(localeId to emptyMap()),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = FontSize.BODY_M,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
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
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = null,
                                padding = null,
                                margin = null,
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
                        ).getOrThrow(),
                        override = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = null,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = FontSize.BODY_M,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(localeId to emptyMap()),
                        ).getOrThrow(),
                        expected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("baseKey"),
                                color = ColorScheme(light = ColorInfo.Alias("baseColor")),
                                backgroundColor = ColorScheme(light = ColorInfo.Alias("baseBgColor")),
                                fontName = "baseFont",
                                fontWeight = FontWeight.MEDIUM,
                                fontSize = FontSize.BODY_M,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to mapOf(
                                    LocalizationKey("baseKey") to LocalizationData.Text("base"),
                                )
                            )
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
        }

        @Test
        fun `Should fail to create if the LocalizationKey is not found`() {
            // Arrange, Act
            val actualResult = LocalizedTextPartial(
                from = PartialTextComponent(
                    text = LocalizationKey("missing-key"),
                ),
                using = nonEmptyMapOf(
                    localeId to mapOf(
                        LocalizationKey("key") to LocalizationData.Text("Hello, world"),
                    )
                )
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
                    localeId to mapOf(
                        LocalizationKey("existing-key") to LocalizationData.Text("Hello, world"),
                    )
                )
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
                    localeId to mapOf(
                        LocalizationKey("existing-key") to LocalizationData.Text("Hello, world"),
                    )
                )
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
                using = nonEmptyMapOf(localeId to emptyMap())
            )

            // Assert
            assert(actualResult.isSuccess)
        }
    }
}
