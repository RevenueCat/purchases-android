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
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState.DEFAULT
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState.SELECTED
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition.COMPACT
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition.EXPANDED
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition.MEDIUM
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class BuildPresentedPartialTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    // LocalizedTextPartial is an arbitrary choice. Any PresentedPartial type would do to test
    // the buildPresentedPartial() logic.
    class Args(
        val availableOverrides: PresentedOverrides<LocalizedTextPartial>,
        val windowSize: ScreenCondition,
        val isEligibleForIntroOffer: Boolean,
        val state: ComponentViewState,
        val expected: LocalizedTextPartial?,
    )

    companion object {
        private val localeId = LocaleId("en_US")
        private val dummyLocalizationDictionary = nonEmptyMapOf(
            LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
        )
        private val selectedPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello selected"),
                )
            )
        ).getOrThrow()
        private val introOfferPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello intro"),
                )
            )
        ).getOrThrow()
        private val compactPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello compact"),
                )
            )
        ).getOrThrow()
        private val mediumPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello medium"),
                )
            )
        ).getOrThrow()
        private val expandedPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello expanded"),
                )
            )
        ).getOrThrow()

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "should pick selected when all overrides available and applicable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides available and state is not selected",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = DEFAULT,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should pick compact when all overrides available, only window applies, and size is compact",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = COMPACT,
                    isEligibleForIntroOffer = false,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should pick medium when all overrides available, only window applies, and size is medium",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = false,
                    state = DEFAULT,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "should pick expanded when all overrides available, only window applies, and size is expanded",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = EXPANDED,
                    isEligibleForIntroOffer = false,
                    state = DEFAULT,
                    expected = expandedPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides applicable, but selected override unavailable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = null,
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should pick medium when all overrides applicable, but selected and intro overrides not available",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = null,
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "should pick medium when window is expanded, all overrides applicable, but selected, intro and " +
                    "expanded overrides not available",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = null,
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = mediumPartial,
                            expanded = null,
                        )
                    ),
                    windowSize = EXPANDED,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "should pick compact when window is medium, all overrides applicable, but selected, intro and medium " +
                    "overrides not available",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = null,
                        conditions = PresentedConditions(
                            compact = compactPartial,
                            medium = null,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should return null when window is compact, all overrides applicable, but selected, intro and " +
                    "compact overrides not available",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = null,
                        conditions = PresentedConditions(
                            compact = null,
                            medium = mediumPartial,
                            expanded = expandedPartial,
                        )
                    ),
                    windowSize = COMPACT,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = null,
                ),
            ),
            arrayOf(
                "should return null when all overrides applicable, but none available",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = null,
                        conditions = null,
                    ),
                    windowSize = COMPACT,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = null,
                ),
            ),
            arrayOf(
                "should return null when all overrides applicable, but none available, selected is null",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = PresentedStates(
                            selected = null
                        ),
                        conditions = null,
                    ),
                    windowSize = COMPACT,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = null,
                ),
            ),
            arrayOf(
                "should pick selected when all overrides applicable, but window override unavailable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = null,
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick selected when all overrides applicable, but window and intro overrides unavailable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = PresentedStates(
                            selected = selectedPartial
                        ),
                        conditions = null,
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides applicable, but window and intro overrides unavailable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = introOfferPartial,
                        states = PresentedStates(
                            selected = null
                        ),
                        conditions = null,
                    ),
                    windowSize = MEDIUM,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should combine all window overrides when they're all available and applicable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = null,
                        states = PresentedStates(
                            selected = null
                        ),
                        conditions = PresentedConditions(
                            compact = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = LocalizationKey("compactKey"),
                                    color = ColorScheme(light = ColorInfo.Alias("compactColor")),
                                    backgroundColor = ColorScheme(light = ColorInfo.Alias("compactBgColor")),
                                    fontName = "compactFont",
                                    fontWeight = FontWeight.LIGHT,
                                    fontSize = FontSize.BODY_S,
                                    horizontalAlignment = HorizontalAlignment.LEADING,
                                    size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                    padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                    margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                ),
                                using = nonEmptyMapOf(
                                    localeId to nonEmptyMapOf(
                                        LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                                    )
                                )
                            ).getOrThrow(),
                            medium = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = null,
                                    color = null,
                                    backgroundColor = null,
                                    fontName = "mediumFont",
                                    fontWeight = FontWeight.MEDIUM,
                                    fontSize = FontSize.BODY_M,
                                    horizontalAlignment = HorizontalAlignment.CENTER,
                                    size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                    padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                    margin = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                                ),
                                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            ).getOrThrow(),
                            expanded = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = null,
                                    color = null,
                                    backgroundColor = null,
                                    fontName = null,
                                    fontWeight = null,
                                    fontSize = null,
                                    horizontalAlignment = null,
                                    size = Size(width = Fixed(30.toUInt()), height = Fixed(30.toUInt())),
                                    padding = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                                    margin = Padding(top = 40.0, bottom = 40.0, leading = 40.0, trailing = 40.0),
                                ),
                                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            ).getOrThrow(),
                        ),
                    ),
                    windowSize = EXPANDED,
                    isEligibleForIntroOffer = false,
                    state = DEFAULT,
                    // We expect all of the non-null properties from the expanded override, the non-null properties
                    // from the medium override that are null in expanded, and the non-null properties from the compact
                    // override that are null in medium or expanded.
                    expected = LocalizedTextPartial(
                        from = PartialTextComponent(
                            visible = true,
                            text = LocalizationKey("compactKey"),
                            color = ColorScheme(light = ColorInfo.Alias("compactColor")),
                            backgroundColor = ColorScheme(light = ColorInfo.Alias("compactBgColor")),
                            fontName = "mediumFont",
                            fontWeight = FontWeight.MEDIUM,
                            fontSize = FontSize.BODY_M,
                            horizontalAlignment = HorizontalAlignment.CENTER,
                            size = Size(width = Fixed(30.toUInt()), height = Fixed(30.toUInt())),
                            padding = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                            margin = Padding(top = 40.0, bottom = 40.0, leading = 40.0, trailing = 40.0),
                        ),
                        using = nonEmptyMapOf(
                            localeId to nonEmptyMapOf(
                                LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                            )
                        )
                    ).getOrThrow(),
                ),
            ),
            arrayOf(
                "should combine all overrides when all are available and applicable",
                Args(
                    availableOverrides = PresentedOverrides(
                        introOffer = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontSize = FontSize.BODY_XL,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(50.toUInt()), height = Fixed(50.toUInt())),
                                padding = Padding(top = 50.0, bottom = 50.0, leading = 50.0, trailing = 50.0),
                                margin = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                        ).getOrThrow(),
                        states = PresentedStates(
                            selected = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = null,
                                    color = null,
                                    backgroundColor = null,
                                    fontName = null,
                                    fontWeight = null,
                                    fontSize = null,
                                    horizontalAlignment = null,
                                    size = Size(width = Fixed(60.toUInt()), height = Fixed(60.toUInt())),
                                    padding = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                                    margin = Padding(top = 70.0, bottom = 70.0, leading = 70.0, trailing = 70.0),
                                ),
                                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            ).getOrThrow(),
                        ),
                        conditions = PresentedConditions(
                            compact = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = LocalizationKey("compactKey"),
                                    color = ColorScheme(light = ColorInfo.Alias("compactColor")),
                                    backgroundColor = ColorScheme(light = ColorInfo.Alias("compactBgColor")),
                                    fontName = "compactFont",
                                    fontWeight = FontWeight.LIGHT,
                                    fontSize = FontSize.BODY_S,
                                    horizontalAlignment = HorizontalAlignment.LEADING,
                                    size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                    padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                    margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                ),
                                using = nonEmptyMapOf(
                                    localeId to nonEmptyMapOf(
                                        LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                                    )
                                )
                            ).getOrThrow(),
                            medium = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = null,
                                    color = ColorScheme(light = ColorInfo.Alias("mediumColor")),
                                    backgroundColor = ColorScheme(light = ColorInfo.Alias("mediumBgColor")),
                                    fontName = "mediumFont",
                                    fontWeight = FontWeight.MEDIUM,
                                    fontSize = FontSize.BODY_M,
                                    horizontalAlignment = HorizontalAlignment.CENTER,
                                    size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                    padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                    margin = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                                ),
                                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            ).getOrThrow(),
                            expanded = LocalizedTextPartial(
                                from = PartialTextComponent(
                                    visible = true,
                                    text = null,
                                    color = null,
                                    backgroundColor = null,
                                    fontName = "expandedFont",
                                    fontWeight = FontWeight.BOLD,
                                    fontSize = FontSize.BODY_L,
                                    horizontalAlignment = HorizontalAlignment.TRAILING,
                                    size = Size(width = Fixed(40.toUInt()), height = Fixed(40.toUInt())),
                                    padding = Padding(top = 40.0, bottom = 40.0, leading = 40.0, trailing = 40.0),
                                    margin = Padding(top = 50.0, bottom = 50.0, leading = 50.0, trailing = 50.0),
                                ),
                                using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            ).getOrThrow(),
                        ),
                    ),
                    windowSize = EXPANDED,
                    isEligibleForIntroOffer = true,
                    state = SELECTED,
                    expected = LocalizedTextPartial(
                        from = PartialTextComponent(
                            visible = true,
                            text = LocalizationKey("compactKey"),
                            color = ColorScheme(light = ColorInfo.Alias("mediumColor")),
                            backgroundColor = ColorScheme(light = ColorInfo.Alias("mediumBgColor")),
                            fontName = "expandedFont",
                            fontWeight = FontWeight.BOLD,
                            fontSize = FontSize.BODY_XL,
                            horizontalAlignment = HorizontalAlignment.CENTER,
                            size = Size(width = Fixed(60.toUInt()), height = Fixed(60.toUInt())),
                            padding = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                            margin = Padding(top = 70.0, bottom = 70.0, leading = 70.0, trailing = 70.0),
                        ),
                        using = nonEmptyMapOf(
                            localeId to nonEmptyMapOf(
                                LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                            )
                        )
                    ).getOrThrow(),
                ),
            ),
        )
    }

    @Test
    fun `Should properly build presented partial`() {
        // Arrange, Act
        val actual: LocalizedTextPartial? = args.availableOverrides.buildPresentedPartial(
            windowSize = args.windowSize,
            isEligibleForIntroOffer = args.isEligibleForIntroOffer,
            state = args.state,
        )

        // Assert
        assert(actual == args.expected)
    }
}
