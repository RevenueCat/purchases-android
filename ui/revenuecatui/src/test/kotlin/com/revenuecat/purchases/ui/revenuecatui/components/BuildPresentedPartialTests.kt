package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState.DEFAULT
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState.SELECTED
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility.INELIGIBLE
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE
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
        val screenCondition: ScreenConditionSnapshot,
        val introOfferEligibility: IntroOfferEligibility,
        val state: ComponentViewState,
        val expected: LocalizedTextPartial?,
        val selectedPackageIdentifier: String? = null,
    )

    @Suppress("LargeClass")
    companion object {
        private val localeId = LocaleId("en_US")
        private val dummyLocalizationDictionary = nonEmptyMapOf(
            LocalizationKey("dummyKey") to LocalizationData.Text("dummyText"),
        )
        private val selectedPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello selected"),
                ),
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val introOfferPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello intro"),
                ),
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val multipleIntroOffersPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello multiple intros"),
                ),
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()
        private val compactPartial = LocalizedTextPartial(
            from = PartialTextComponent(),
            using = nonEmptyMapOf(
                localeId to nonEmptyMapOf(
                    LocalizationKey("key") to LocalizationData.Text("Hello compact"),
                ),
            ),
            aliases = emptyMap(),
            fontAliases = emptyMap(),
        ).getOrThrow()

        @JvmStatic
        private fun buildPresentedOverrides(
            introOffer: LocalizedTextPartial? = introOfferPartial,
            multipleIntroOffers: LocalizedTextPartial? = multipleIntroOffersPartial,
            selected: LocalizedTextPartial? = selectedPartial,
        ): List<PresentedOverride<LocalizedTextPartial>> {
            val overrides: MutableList<PresentedOverride<LocalizedTextPartial>> = mutableListOf()
            introOffer?.let {
                overrides.add(
                    PresentedOverride(
                        conditions = listOf(ComponentOverride.Condition.IntroOffer(
                            operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                            value = true,
                        )),
                        properties = it,
                    ),
                )
            }
            multipleIntroOffers?.let {
                overrides.add(
                    PresentedOverride(
                        conditions = listOf(
                            ComponentOverride.Condition.MultipleIntroOffers(
                                operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                value = true,
                            )
                        ),
                        properties = it,
                    ),
                )
            }
            selected?.let {
                overrides.add(
                    PresentedOverride(
                        conditions = listOf(ComponentOverride.Condition.Selected),
                        properties = it,
                    ),
                )
            }
            return overrides
        }

        private fun snapshot(
            condition: ScreenCondition,
            orientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
            screenSizeName: String? = null,
        ): ScreenConditionSnapshot = ScreenConditionSnapshot(
            condition = condition,
            orientation = orientation,
            screenSize = screenSizeName?.let { UiConfig.AppConfig.ScreenSize(name = it, width = 0) },
        )

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "should pick selected when all overrides available and applicable",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = MULTIPLE_OFFERS_ELIGIBLE,
                    state = SELECTED,
                    expected = selectedPartial,
                ),
            ),
            arrayOf(
                "should pick multiple intros when all overrides available and state is not selected",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = MULTIPLE_OFFERS_ELIGIBLE,
                    state = DEFAULT,
                    expected = multipleIntroOffersPartial,
                ),
            ),
            arrayOf(
                "should pick intro when all overrides available and state is not selected and eligibility is single",
                Args(
                    availableOverrides = buildPresentedOverrides(),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should respect orientation condition",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.LANDSCAPE),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.LANDSCAPE,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should reject orientation when not matching",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.LANDSCAPE),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.PORTRAIT,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                ),
            ),
            arrayOf(
                "should respect screen size condition",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.ScreenSize(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    sizes = listOf("tablet"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        screenSizeName = "tablet",
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should respect selected package condition",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should pick intro when all overrides applicable, but selected and multiple intro override unavailable",
                Args(
                    availableOverrides = buildPresentedOverrides(multipleIntroOffers = null, selected = null),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = MULTIPLE_OFFERS_ELIGIBLE,
                    state = SELECTED,
                    expected = introOfferPartial,
                ),
            ),
            arrayOf(
                "should respect orientation condition with NOT_IN operator",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.LANDSCAPE),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.PORTRAIT,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should apply orientation NOT_IN when orientation unknown",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.PORTRAIT),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.UNKNOWN,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should reject orientation when NOT_IN matches",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.PORTRAIT),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.PORTRAIT,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                ),
            ),
            arrayOf(
                "should respect screen size condition with NOT_IN operator",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.ScreenSize(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    sizes = listOf("phone"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        screenSizeName = "tablet",
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should reject screen size when active size is unknown",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.ScreenSize(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    sizes = listOf("tablet"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        screenSizeName = null,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                ),
            ),
            arrayOf(
                "should reject screen size when NOT_IN matches",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.ScreenSize(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    sizes = listOf("tablet"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        screenSizeName = "tablet",
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                ),
            ),
            arrayOf(
                "should respect selected package condition with NOT_IN operator",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    packages = listOf("rc_monthly"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should reject selected package when not provided",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    packages = listOf("rc_annual"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                    selectedPackageIdentifier = null,
                ),
            ),
            arrayOf(
                "should reject selected package when NOT_IN matches",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    packages = listOf("rc_annual"),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should reject unsupported condition",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(ComponentOverride.Condition.Unsupported),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                ),
            ),
            arrayOf(
                "should apply override when all multiple conditions match",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Selected,
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.LANDSCAPE),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.LANDSCAPE,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = SELECTED,
                    expected = compactPartial,
                ),
            ),
            arrayOf(
                "should reject override when one of multiple conditions does not match",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.Selected,
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.LANDSCAPE),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.PORTRAIT,
                    ),
                    introOfferEligibility = INELIGIBLE,
                    state = SELECTED,
                    expected = null,
                ),
            ),
            arrayOf(
                "should apply override when SelectedPackage and IntroOffer both match (single offer)",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should apply override when SelectedPackage and IntroOffer both match (multiple offers)",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = MULTIPLE_OFFERS_ELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should reject override when SelectedPackage matches but IntroOffer does not",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = INELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should reject override when IntroOffer matches but SelectedPackage does not",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                    selectedPackageIdentifier = "rc_monthly",
                ),
            ),
            arrayOf(
                "should apply override with SelectedPackage NOT_IN and IntroOffer when both match",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.NOT_IN,
                                    packages = listOf("rc_monthly"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should apply override when SelectedPackage and MultipleIntroOffers both match",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.MultipleIntroOffers(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = MULTIPLE_OFFERS_ELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should reject override when SelectedPackage matches but MultipleIntroOffers does not",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.MultipleIntroOffers(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should apply override with multiple packages and IntroOffer",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual", "rc_six_month"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(ScreenCondition.MEDIUM),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_six_month",
                ),
            ),
            arrayOf(
                "should apply override with three combined conditions: SelectedPackage, IntroOffer, and Orientation",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.PORTRAIT),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.PORTRAIT,
                    ),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = compactPartial,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
            arrayOf(
                "should reject override when SelectedPackage and IntroOffer match but Orientation does not",
                Args(
                    availableOverrides = listOf(
                        PresentedOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.SelectedPackage(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    packages = listOf("rc_annual"),
                                ),
                                ComponentOverride.Condition.IntroOffer(
                                    operator = ComponentOverride.Condition.EqualityOperatorType.EQUALS,
                                    value = true,
                                ),
                                ComponentOverride.Condition.Orientation(
                                    operator = ComponentOverride.Condition.ArrayOperatorType.IN,
                                    orientations = listOf(ComponentOverride.Condition.OrientationType.LANDSCAPE),
                                ),
                            ),
                            properties = compactPartial,
                        ),
                    ),
                    screenCondition = snapshot(
                        condition = ScreenCondition.MEDIUM,
                        orientation = ScreenOrientation.PORTRAIT,
                    ),
                    introOfferEligibility = SINGLE_OFFER_ELIGIBLE,
                    state = DEFAULT,
                    expected = null,
                    selectedPackageIdentifier = "rc_annual",
                ),
            ),
        )
    }

    @Test
    fun `Should properly build presented partial`() {
        // Arrange, Act
        val actual: LocalizedTextPartial? = args.availableOverrides.buildPresentedPartial(
            screenCondition = args.screenCondition,
            introOfferEligibility = args.introOfferEligibility,
            state = args.state,
            selectedPackageIdentifier = args.selectedPackageIdentifier,
        )

        // Assert
        Assertions.assertThat(actual).isEqualTo(args.expected)
    }
}
