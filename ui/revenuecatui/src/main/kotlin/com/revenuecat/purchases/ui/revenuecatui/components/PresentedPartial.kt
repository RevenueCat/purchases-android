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
    windowSize: ScreenCondition,
    introOfferEligibility: IntroOfferEligibility,
    state: ComponentViewState,
): T? {
    var partial: T? = null
    for (override in this) {
        if (override.shouldApply(windowSize, introOfferEligibility, state)) {
            partial = partial.combineOrReplace(override.properties)
        }
    }
    return partial
}

@Suppress("ReturnCount")
private fun <T : PresentedPartial<T>> PresentedOverride<T>.shouldApply(
    windowSize: ScreenCondition,
    introOfferEligibility: IntroOfferEligibility,
    state: ComponentViewState,
): Boolean {
    for (condition in conditions) {
        when (condition) {
            ComponentOverride.Condition.Compact,
            ComponentOverride.Condition.Medium,
            ComponentOverride.Condition.Expanded,
            -> {
                if (!windowSize.applicableConditions.contains(condition)) return false
            }
            ComponentOverride.Condition.MultiplePhaseOffers -> {
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
