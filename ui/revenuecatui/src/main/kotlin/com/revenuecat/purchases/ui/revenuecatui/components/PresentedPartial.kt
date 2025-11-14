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
): Boolean {
    for (condition in conditions) {
        when (condition) {
            ComponentOverride.Condition.Compact,
            ComponentOverride.Condition.Medium,
            ComponentOverride.Condition.Expanded,
            -> {
                if (!screenCondition.condition.applicableConditions.contains(condition)) return false
            }
            ComponentOverride.Condition.MultipleIntroOffers -> {
                if (introOfferEligibility != IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE) return false
            }
            ComponentOverride.Condition.IntroOffer -> {
                if (introOfferEligibility == IntroOfferEligibility.INELIGIBLE) return false
            }
            ComponentOverride.Condition.Selected -> {
                if (state != ComponentViewState.SELECTED) return false
            }
            ComponentOverride.Condition.Unsupported -> {
                return false
            }
            is ComponentOverride.Condition.Orientation -> {
                val activeOrientation = screenCondition.orientation.toConditionOrientationType()
                when (condition.operator) {
                    ComponentOverride.Condition.ArrayOperatorType.IN ->
                        if (activeOrientation == null || !condition.orientations.contains(activeOrientation)) {
                            return false
                        }
                    ComponentOverride.Condition.ArrayOperatorType.NOT_IN ->
                        if (activeOrientation != null && condition.orientations.contains(activeOrientation)) {
                            return false
                        }
                }
            }
            is ComponentOverride.Condition.ScreenSize -> {
                val activeScreenSize = screenCondition.screenSize?.name ?: return false
                when (condition.operator) {
                    ComponentOverride.Condition.ArrayOperatorType.IN ->
                        if (!condition.sizes.contains(activeScreenSize)) {
                            return false
                        }
                    ComponentOverride.Condition.ArrayOperatorType.NOT_IN ->
                        if (condition.sizes.contains(activeScreenSize)) {
                            return false
                        }
                }
            }
            is ComponentOverride.Condition.SelectedPackage -> {
                val selected = selectedPackageIdentifier ?: return false
                when (condition.operator) {
                    ComponentOverride.Condition.ArrayOperatorType.IN ->
                        if (!condition.packages.contains(selected)) {
                            return false
                        }
                    ComponentOverride.Condition.ArrayOperatorType.NOT_IN ->
                        if (condition.packages.contains(selected)) {
                            return false
                        }
                }
            }
        }
    }
    return true
}

private val ScreenCondition.applicableConditions: Set<ComponentOverride.Condition>
    get() = when (this) {
        ScreenCondition.COMPACT -> setOf(ComponentOverride.Condition.Compact)
        ScreenCondition.MEDIUM -> setOf(ComponentOverride.Condition.Compact, ComponentOverride.Condition.Medium)
        ScreenCondition.EXPANDED -> setOf(
            ComponentOverride.Condition.Compact,
            ComponentOverride.Condition.Medium,
            ComponentOverride.Condition.Expanded,
        )
    }

private fun ScreenOrientation.toConditionOrientationType(): ComponentOverride.Condition.OrientationType? =
    when (this) {
        ScreenOrientation.PORTRAIT -> ComponentOverride.Condition.OrientationType.PORTRAIT
        ScreenOrientation.LANDSCAPE -> ComponentOverride.Condition.OrientationType.LANDSCAPE
        ScreenOrientation.UNKNOWN -> null
    }
