package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrElse
import dev.drewhamilton.poko.Poko

/**
 * Partial components transformed and ready for presentation.
 */
internal sealed interface PresentedPartial<T : PresentedPartial<T>> {
    /**
     * Combines this partial component with another, allowing for override behavior.
     *
     * @param with The overriding partial component.
     *
     * @return The combined partial component.
     */
    @JvmSynthetic
    fun combine(with: T?): T
}

/**
 * @return A combined partial if both [this] and [with] are non-null. Will return [with] if [this] is null.
 */
private fun <T : PresentedPartial<T>> PresentedPartial<T>?.combineOrReplace(with: T?): T? =
    this?.combine(with) ?: with

/**
 * Structure holding override configurations for different presentation states.
 */
@Poko
internal class PresentedOverride<T : PresentedPartial<T>>(
    /**
     * Conditions that need to be met to present the given partial
     */
    @get:JvmSynthetic val conditions: List<ComponentOverride.Condition>,
    /**
     * Partial that needs to be applied if the conditions are met
     */
    @get:JvmSynthetic val properties: T,
)

/**
 * Converts component overrides to presented overrides.
 */
@Suppress("ReturnCount")
@JvmSynthetic
internal fun <T : PartialComponent, P : PresentedPartial<P>> List<ComponentOverride<T>>.toPresentedOverrides(
    transform: (T) -> Result<P, NonEmptyList<PaywallValidationError>>,
): Result<List<PresentedOverride<P>>, PaywallValidationError> {
    return this.map { override ->
        val properties = transform(override.properties)
            .getOrElse { return Result.Error(it.head) }

        PresentedOverride(
            conditions = override.conditions,
            properties = properties,
        )
    }.let { Result.Success(it) }
}

/**
 * Builds a presentable partial component based on current view state, screen condition and whether the user
 * is eligible for an intro offer.
 *
 * @param windowSize Current screen condition (compact / medium / expanded).
 * @param introOfferEligibility Whether the user is eligible for an intro offer.
 * @param state Current view state (selected / unselected).
 *
 * @return A presentable partial component, or null if [this] the list of [PresentedOverride] did not contain any
 * available overrides to use.
 */
@JvmSynthetic
internal fun <T : PresentedPartial<T>> List<PresentedOverride<T>>.buildPresentedPartial(
    screenCondition: ScreenConditionSnapshot,
    introOfferEligibility: IntroOfferEligibility,
    state: ComponentViewState,
    selectedPackageIdentifier: String?,
): T? {
    var partial: T? = null
    for (override in this) {
        if (
            override.shouldApply(
                screenCondition,
                introOfferEligibility,
                state,
                selectedPackageIdentifier,
            )
        ) {
            partial = partial.combineOrReplace(override.properties)
        }
    }
    return partial
}

@Suppress("ReturnCount")
private fun <T : PresentedPartial<T>> PresentedOverride<T>.shouldApply(
    screenCondition: ScreenConditionSnapshot,
    introOfferEligibility: IntroOfferEligibility,
    state: ComponentViewState,
    selectedPackageIdentifier: String?,
): Boolean = this.conditions.all { condition ->
    conditionMatches(condition, screenCondition, introOfferEligibility, state, selectedPackageIdentifier)
}

private fun conditionMatches(
    condition: ComponentOverride.Condition,
    screenCondition: ScreenConditionSnapshot,
    introOfferEligibility: IntroOfferEligibility,
    state: ComponentViewState,
    selectedPackageIdentifier: String?,
): Boolean = when (condition) {
    is ComponentOverride.Condition.MultipleIntroOffers -> when (condition.operator) {
        ComponentOverride.Condition.EqualityOperatorType.EQUALS ->
            introOfferEligibility.hasMultipleIntroOffers() == condition.value
        ComponentOverride.Condition.EqualityOperatorType.NOT_EQUALS ->
            introOfferEligibility.hasMultipleIntroOffers() != condition.value
    }

    ComponentOverride.Condition.Selected ->
        state == ComponentViewState.SELECTED

    ComponentOverride.Condition.Unsupported -> false

    is ComponentOverride.Condition.IntroOffer -> when (condition.operator) {
        ComponentOverride.Condition.EqualityOperatorType.EQUALS ->
            introOfferEligibility.isEligible() == condition.value
        ComponentOverride.Condition.EqualityOperatorType.NOT_EQUALS ->
            introOfferEligibility.isEligible() != condition.value
    }

    is ComponentOverride.Condition.Orientation ->
        matchesOrientation(condition, screenCondition.orientation)

    is ComponentOverride.Condition.ScreenSize ->
        matchesScreenSize(condition, screenCondition.screenSize?.name)

    is ComponentOverride.Condition.SelectedPackage ->
        matchesSelectedPackage(condition, selectedPackageIdentifier)
}

private fun matchesOrientation(
    condition: ComponentOverride.Condition.Orientation,
    orientation: ScreenOrientation,
): Boolean {
    val activeOrientation = orientation.toConditionOrientationType()
    return when (condition.operator) {
        ComponentOverride.Condition.ArrayOperatorType.IN ->
            activeOrientation != null && condition.orientations.contains(activeOrientation)
        ComponentOverride.Condition.ArrayOperatorType.NOT_IN ->
            activeOrientation == null || !condition.orientations.contains(activeOrientation)
    }
}

private fun matchesScreenSize(
    condition: ComponentOverride.Condition.ScreenSize,
    activeName: String?,
): Boolean {
    activeName ?: return false
    return when (condition.operator) {
        ComponentOverride.Condition.ArrayOperatorType.IN ->
            condition.sizes.contains(activeName)
        ComponentOverride.Condition.ArrayOperatorType.NOT_IN ->
            !condition.sizes.contains(activeName)
    }
}

private fun matchesSelectedPackage(
    condition: ComponentOverride.Condition.SelectedPackage,
    selectedPackageIdentifier: String?,
): Boolean {
    val selected = selectedPackageIdentifier ?: return false
    return when (condition.operator) {
        ComponentOverride.Condition.ArrayOperatorType.IN ->
            condition.packages.contains(selected)
        ComponentOverride.Condition.ArrayOperatorType.NOT_IN ->
            !condition.packages.contains(selected)
    }
}

private fun ScreenOrientation.toConditionOrientationType(): ComponentOverride.Condition.OrientationType? =
    when (this) {
        ScreenOrientation.PORTRAIT -> ComponentOverride.Condition.OrientationType.PORTRAIT
        ScreenOrientation.LANDSCAPE -> ComponentOverride.Condition.OrientationType.LANDSCAPE
        ScreenOrientation.UNKNOWN -> null
    }

private fun IntroOfferEligibility.isEligible(): Boolean =
    this != IntroOfferEligibility.INELIGIBLE

private fun IntroOfferEligibility.hasMultipleIntroOffers(): Boolean =
    this == IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE
