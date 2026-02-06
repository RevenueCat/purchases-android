package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
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
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState.DEFAULT
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState.SELECTED
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition.COMPACT
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition.EXPANDED
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition.MEDIUM
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility.Ineligible
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility.IntroOfferMultiple
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility.IntroOfferSingle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility.PromoOfferSingle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility.PromoOfferMultiple
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class BuildPresentedPartialTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    // LocalizedTextPartial is an arbitrary choice. Any PresentedPartial type would do to test
    // the buildPresentedPartial() logic.
    class Args(
        val availableOverrides: List<PresentedOverride<LocalizedTextPartial>>,
        val windowSize: ScreenCondition,
        val offerEligibility: OfferEligibility,
        val state: ComponentViewState,
        val expected: LocalizedTextPartial?,
    )

    @Suppress("LargeClass")
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
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val introOfferPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello intro"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val multipleIntroOffersPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello multiple intros"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val compactPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello compact"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val mediumPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello medium"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val expandedPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello expanded"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val introOfferAndSelectedPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello intro and selected"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val promoOfferPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello promo"),
                )
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()

        @JvmStatic
        private fun buildPresentedOverrides(
            introOffer: LocalizedTextPartial? = introOfferPartial,
            multipleIntroOffers: LocalizedTextPartial? = multipleIntroOffersPartial,
            selected: LocalizedTextPartial? = selectedPartial,
            expanded: LocalizedTextPartial? = expandedPartial,
            medium: LocalizedTextPartial? = mediumPartial,
            compact: LocalizedTextPartial? = compactPartial,
            promoOffer: LocalizedTextPartial? = null,
        ): List<PresentedOverride<LocalizedTextPartial>> {
            val overrides: MutableList<PresentedOverride<LocalizedTextPartial>> = mutableListOf()
            compact?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.Compact),
                    properties = it,
                ))
            }
            medium?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.Medium),
                    properties = it,
                ))
            }
            expanded?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.Expanded),
                    properties = it,
                ))
            }
            introOffer?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.IntroOffer),
                    properties = it,
                ))
            }
            promoOffer?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.PromoOffer),
                    properties = it,
                ))
            }
            multipleIntroOffers?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.MultiplePhaseOffers),
                    properties = it,
                ))
            }
            selected?.let {
                overrides.add(PresentedOverride(
                    conditions = listOf(ComponentOverride.Condition.Selected),
                    properties = it,
                ))
            }
            return overrides
        }

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "should pick selected when all overrides available and applicable",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick multiple intros when all overrides available and state is not selected",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = DEFAULT,
                    expected = multipleIntroOffersPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides available and state is not selected and eligibility is single",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferSingle,
                    state = DEFAULT,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should pick compact when all overrides available, only window applies, and size is compact",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    windowSize = COMPACT,
                    offerEligibility = Ineligible,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should pick medium when all overrides available, only window applies, and size is medium",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    windowSize = MEDIUM,
                    offerEligibility = Ineligible,
                    state = DEFAULT,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "should pick expanded when all overrides available, only window applies, and size is expanded",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    windowSize = EXPANDED,
                    offerEligibility = Ineligible,
                    state = DEFAULT,
                    expected = expandedPartial,
                ),
            ),
            arrayOf(
                "should pick multiple intros when all overrides applicable, but selected override unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(selected = null),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = multipleIntroOffersPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides applicable, but selected and multiple intro override unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(multipleIntroOffers = null, selected = null),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should pick medium when all overrides applicable, but selected and intro overrides not available",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = null,
                        multipleIntroOffers = null,
                        selected = null,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "should pick medium when window is expanded, all overrides applicable, but selected, intro and " +
                    "expanded overrides not available",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = null,
                        multipleIntroOffers = null,
                        selected = null,
                        expanded = null,
                    ),
                    windowSize = EXPANDED,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "should pick compact when window is medium, all overrides applicable, but selected, intro and medium " +
                    "overrides not available",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = null,
                        multipleIntroOffers = null,
                        selected = null,
                        medium = null,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should return null when window is compact, all overrides applicable, but selected, intro and " +
                    "compact overrides not available",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = null,
                        multipleIntroOffers = null,
                        selected = null,
                        compact = null,
                    ),
                    windowSize = COMPACT,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = null,
                ),
            ),
            arrayOf(
                "should return null when all overrides applicable, but none available",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = null,
                        multipleIntroOffers = null,
                        selected = null,
                        expanded = null,
                        medium = null,
                        compact = null,
                    ),
                    windowSize = COMPACT,
                    offerEligibility = IntroOfferSingle,
                    state = SELECTED,
                    expected = null,
                ),
            ),
            arrayOf(
                "should pick selected when all overrides applicable, but window override unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        compact = null,
                        medium = null,
                        expanded = null,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick selected when all overrides applicable, but window and intro overrides unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        compact = null,
                        medium = null,
                        expanded = null,
                        introOffer = null,
                        multipleIntroOffers = null,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick multiple phase when all overrides applicable, but window and selected overrides " +
                    "unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        compact = null,
                        medium = null,
                        expanded = null,
                        selected = null,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = multipleIntroOffersPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides applicable, but window and selected overrides unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        compact = null,
                        medium = null,
                        expanded = null,
                        selected = null,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferSingle,
                    state = SELECTED,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "overrides with multiple conditions that are all applicable should override previous overrides",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(ComponentOverride.Condition.Medium),
                            properties = mediumPartial,
                        ),
                        PresentedOverride(
                            conditions = listOf(ComponentOverride.Condition.Selected),
                            properties = selectedPartial,
                        ),
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Medium,
                                ComponentOverride.Condition.Selected,
                            ),
                            properties = introOfferAndSelectedPartial,
                        ),
                    ),
                    windowSize = EXPANDED,
                    offerEligibility = IntroOfferSingle,
                    state = SELECTED,
                    expected = introOfferAndSelectedPartial,
                )
            ),
            arrayOf(
                "overrides with multiple conditions that are not all applicable should not override previous overrides",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(ComponentOverride.Condition.Medium),
                            properties = mediumPartial,
                        ),
                        PresentedOverride(
                            conditions = listOf(ComponentOverride.Condition.Selected),
                            properties = selectedPartial,
                        ),
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Medium,
                                ComponentOverride.Condition.Selected,
                            ),
                            properties = introOfferAndSelectedPartial,
                        ),
                    ),
                    windowSize = COMPACT,
                    offerEligibility = IntroOfferSingle,
                    state = SELECTED,
                    expected = selectedPartial,
                )
            ),
            arrayOf(
                "should combine all window overrides when they're all available and applicable",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = null,
                        multipleIntroOffers = null,
                        selected = null,
                        expanded = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontWeightInt = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = Size(width = Fixed(30.toUInt()), height = Fixed(30.toUInt())),
                                padding = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                                margin = Padding(top = 40.0, bottom = 40.0, leading = 40.0, trailing = 40.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                                )
                            ),
                            aliases = emptyMap(),
                            fontAliases = emptyMap(),
                        ).getOrThrow(),
                        medium = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = FontAlias("mediumFont"),
                                fontWeight = FontWeight.MEDIUM,
                                fontWeightInt = 500,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                            fontAliases = mapOf(
                                FontAlias("mediumFont") to FontSpec.System("mediumFont"),
                            ),
                        ).getOrThrow(),
                        compact = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("compactKey"),
                                color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                fontName = FontAlias("compactFont"),
                                fontWeight = FontWeight.LIGHT,
                                fontWeightInt = 200,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                                )
                            ),
                            aliases = emptyMap(),
                            fontAliases = mapOf(
                                FontAlias("compactFont") to FontSpec.System("compactFont"),
                            ),
                        ).getOrThrow(),
                    ),
                    windowSize = EXPANDED,
                    offerEligibility = Ineligible,
                    state = DEFAULT,
                    // We expect all of the non-null properties from the expanded override, the non-null properties
                    // from the medium override that are null in expanded, and the non-null properties from the compact
                    // override that are null in medium or expanded.
                    expected = LocalizedTextPartial(
                        from = PartialTextComponent(
                            visible = true,
                            text = LocalizationKey("compactKey"),
                            color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                            fontName = FontAlias("mediumFont"),
                            fontWeight = FontWeight.MEDIUM,
                            fontWeightInt = 500,
                            fontSize = 15,
                            horizontalAlignment = HorizontalAlignment.CENTER,
                            size = Size(width = Fixed(30.toUInt()), height = Fixed(30.toUInt())),
                            padding = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                            margin = Padding(top = 40.0, bottom = 40.0, leading = 40.0, trailing = 40.0),
                        ),
                        using = nonEmptyMapOf(
                            localeId to nonEmptyMapOf(
                                LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                            )
                        ),
                        aliases = emptyMap(),
                        fontAliases = mapOf(
                            FontAlias("mediumFont") to FontSpec.System("mediumFont"),
                        ),
                    ).getOrThrow(),
                ),
            ),
            arrayOf(
                "should combine all overrides when all are available and applicable",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        introOffer = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontWeightInt = null,
                                fontSize = 18,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(50.toUInt()), height = Fixed(50.toUInt())),
                                padding = Padding(top = 50.0, bottom = 50.0, leading = 50.0, trailing = 50.0),
                                margin = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                            fontAliases = emptyMap(),
                        ).getOrThrow(),
                        multipleIntroOffers = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontWeightInt = null,
                                fontSize = 34,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(50.toUInt()), height = Fixed(50.toUInt())),
                                padding = Padding(top = 50.0, bottom = 50.0, leading = 50.0, trailing = 50.0),
                                margin = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                            fontAliases = emptyMap(),
                        ).getOrThrow(),
                        selected = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = null,
                                fontWeight = null,
                                fontWeightInt = null,
                                fontSize = null,
                                horizontalAlignment = null,
                                size = Size(width = Fixed(60.toUInt()), height = Fixed(60.toUInt())),
                                padding = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                                margin = Padding(top = 70.0, bottom = 70.0, leading = 70.0, trailing = 70.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                            fontAliases = emptyMap(),
                        ).getOrThrow(),
                        compact = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = LocalizationKey("compactKey"),
                                color = ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb())),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                                fontName = FontAlias("compactFont"),
                                fontWeight = FontWeight.LIGHT,
                                fontWeightInt = 200,
                                fontSize = 13,
                                horizontalAlignment = HorizontalAlignment.LEADING,
                                size = Size(width = Fixed(10.toUInt()), height = Fixed(10.toUInt())),
                                padding = Padding(top = 10.0, bottom = 10.0, leading = 10.0, trailing = 10.0),
                                margin = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                            ),
                            using = nonEmptyMapOf(
                                localeId to nonEmptyMapOf(
                                    LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                                )
                            ),
                            aliases = emptyMap(),
                            fontAliases = mapOf(
                                FontAlias("compactFont") to FontSpec.System("compactFont"),
                            ),
                        ).getOrThrow(),
                        medium = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                                backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                                fontName = FontAlias("mediumFont"),
                                fontWeight = FontWeight.MEDIUM,
                                fontWeightInt = 500,
                                fontSize = 15,
                                horizontalAlignment = HorizontalAlignment.CENTER,
                                size = Size(width = Fixed(20.toUInt()), height = Fixed(20.toUInt())),
                                padding = Padding(top = 20.0, bottom = 20.0, leading = 20.0, trailing = 20.0),
                                margin = Padding(top = 30.0, bottom = 30.0, leading = 30.0, trailing = 30.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                            fontAliases = mapOf(
                                FontAlias("mediumFont") to FontSpec.System("mediumFont"),
                            ),
                        ).getOrThrow(),
                        expanded = LocalizedTextPartial(
                            from = PartialTextComponent(
                                visible = true,
                                text = null,
                                color = null,
                                backgroundColor = null,
                                fontName = FontAlias("expandedFont"),
                                fontWeight = FontWeight.BOLD,
                                fontWeightInt = 600,
                                fontSize = 17,
                                horizontalAlignment = HorizontalAlignment.TRAILING,
                                size = Size(width = Fixed(40.toUInt()), height = Fixed(40.toUInt())),
                                padding = Padding(top = 40.0, bottom = 40.0, leading = 40.0, trailing = 40.0),
                                margin = Padding(top = 50.0, bottom = 50.0, leading = 50.0, trailing = 50.0),
                            ),
                            using = nonEmptyMapOf(localeId to dummyLocalizationDictionary),
                            aliases = emptyMap(),
                            fontAliases = mapOf(
                                FontAlias("expandedFont") to FontSpec.System("expandedFont"),
                            ),
                        ).getOrThrow(),
                    ),
                    windowSize = EXPANDED,
                    offerEligibility = IntroOfferMultiple,
                    state = SELECTED,
                    expected = LocalizedTextPartial(
                        from = PartialTextComponent(
                            visible = true,
                            text = LocalizationKey("compactKey"),
                            color = ColorScheme(light = ColorInfo.Hex(Color.Cyan.toArgb())),
                            backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.Yellow.toArgb())),
                            fontName = FontAlias("expandedFont"),
                            fontWeight = FontWeight.BOLD,
                            fontWeightInt = 600,
                            fontSize = 34,
                            horizontalAlignment = HorizontalAlignment.CENTER,
                            size = Size(width = Fixed(60.toUInt()), height = Fixed(60.toUInt())),
                            padding = Padding(top = 60.0, bottom = 60.0, leading = 60.0, trailing = 60.0),
                            margin = Padding(top = 70.0, bottom = 70.0, leading = 70.0, trailing = 70.0),
                        ),
                        using = nonEmptyMapOf(
                            localeId to nonEmptyMapOf(
                                LocalizationKey("compactKey") to LocalizationData.Text("compactText"),
                            )
                        ),
                        aliases = emptyMap(),
                        fontAliases = mapOf(
                            FontAlias("expandedFont") to FontSpec.System("expandedFont"),
                        ),
                    ).getOrThrow(),
                ),
            ),
            arrayOf(
                "should pick promo offer when PromoOffer condition available and eligibility is PromoOfferSingle",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = promoOfferPartial),
                    windowSize = MEDIUM,
                    offerEligibility = PromoOfferSingle,
                    state = DEFAULT,
                    expected = promoOfferPartial,
                ),
            ),
            arrayOf(
                "should not pick promo offer when eligibility is IntroOfferSingle",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = promoOfferPartial),
                    windowSize = MEDIUM,
                    offerEligibility = IntroOfferSingle,
                    state = DEFAULT,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should not pick promo offer when eligibility is Ineligible",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = promoOfferPartial),
                    windowSize = MEDIUM,
                    offerEligibility = Ineligible,
                    state = DEFAULT,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "PromoOfferSingle without matching override uses screen size fallback",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = null),
                    windowSize = MEDIUM,
                    offerEligibility = PromoOfferSingle,
                    state = DEFAULT,
                    expected = mediumPartial,
                ),
            ),
            arrayOf(
                "PromoOfferMultiple matches MultiplePhaseOffers override",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = null),
                    windowSize = MEDIUM,
                    offerEligibility = PromoOfferMultiple,
                    state = DEFAULT,
                    expected = multipleIntroOffersPartial,
                ),
            ),
            arrayOf(
                "PromoOfferMultiple uses MultiplePhaseOffers override instead of PromoOffer",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = promoOfferPartial),
                    windowSize = MEDIUM,
                    offerEligibility = PromoOfferMultiple,
                    state = DEFAULT,
                    expected = multipleIntroOffersPartial,
                ),
            ),
            arrayOf(
                "PromoOfferMultiple without MultiplePhaseOffers override falls back to PromoOffer",
                Args(
                    availableOverrides = buildPresentedOverrides(
                        multipleIntroOffers = null,
                        selected = null,
                        promoOffer = promoOfferPartial,
                    ),
                    windowSize = MEDIUM,
                    offerEligibility = PromoOfferMultiple,
                    state = DEFAULT,
                    expected = promoOfferPartial,
                ),
            ),
            arrayOf(
                "selected should take precedence over promo offer",
                Args(
                    availableOverrides = buildPresentedOverrides(promoOffer = promoOfferPartial),
                    windowSize = MEDIUM,
                    offerEligibility = PromoOfferSingle,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
        )
    }

    @Test
    fun `Should properly build presented partial`() {
        // Arrange, Act
        val actual: LocalizedTextPartial? = args.availableOverrides.buildPresentedPartial(
            windowSize = args.windowSize,
            offerEligibility = args.offerEligibility,
            state = args.state,
        )

        // Assert
        Assertions.assertThat(actual).isEqualTo(args.expected)
    }
}
