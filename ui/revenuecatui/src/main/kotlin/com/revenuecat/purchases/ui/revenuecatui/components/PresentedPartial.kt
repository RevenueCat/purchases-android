package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
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
internal class PresentedOverrides<T : PresentedPartial<T>>(
    /**
     * Override when the user is eligible for a single offer.
     */
    @get:JvmSynthetic val introOffer: T?,
    /**
     * Override when the user is eligible for multiple offers.
     */
    @get:JvmSynthetic val multipleIntroOffers: T?,
    /**
     * Override for different selection states.
     */
    @get:JvmSynthetic val states: PresentedStates<T>?,
    /**
     * Override for different screen size conditions.
     */
    @get:JvmSynthetic val conditions: PresentedConditions<T>?,
)

/**
 * Structure defining states for selected/unselected components.
 */
@Poko
internal class PresentedStates<T : PresentedPartial<T>>(
    /**
     * Override for selected state
     */
    @get:JvmSynthetic val selected: T?,
)

/**
 * Structure defining overrides for different screen size conditions.
 */
@Poko
internal class PresentedConditions<T : PresentedPartial<T>>(
    /**
     * Override for compact size
     */
    @get:JvmSynthetic val compact: T?,
    /**
     * Override for medium size
     */
    @get:JvmSynthetic val medium: T?,
    /**
     * Override for expanded size
     */
    @get:JvmSynthetic val expanded: T?,
)

/**
 * Converts component overrides to presented overrides.
 */
@Suppress("ReturnCount")
@JvmSynthetic
internal fun <T : PartialComponent, P : PresentedPartial<P>> ComponentOverrides<T>.toPresentedOverrides(
    transform: (T) -> Result<P, NonEmptyList<PaywallValidationError>>,
): Result<PresentedOverrides<P>, PaywallValidationError> {
    val introOffer = introOffer?.let(transform)
        ?.getOrElse { return Result.Error(it.head) }

    val multipleIntroOffers = multipleIntroOffers?.let(transform)
        ?.getOrElse { return Result.Error(it.head) }

    val selectedState = states?.selected?.let(transform)
        ?.getOrElse { return Result.Error(it.head) }

    val states = states?.let { PresentedStates(selected = selectedState) }

    val conditions = conditions?.let { conditions ->
        PresentedConditions(
            compact = conditions.compact?.let(transform)?.getOrElse { return Result.Error(it.head) },
            medium = conditions.medium?.let(transform)?.getOrElse { return Result.Error(it.head) },
            expanded = conditions.expanded?.let(transform)?.getOrElse { return Result.Error(it.head) },
        )
    }

    return Result.Success(
        PresentedOverrides(
            introOffer = introOffer,
            multipleIntroOffers = multipleIntroOffers,
            states = states,
            conditions = conditions,
        ),
    )
}

/**
 * Builds a presentable partial component based on current view state, screen condition and whether the user
 * is eligible for an intro offer.
 *
 * @param windowSize Current screen condition (compact / medium / expanded).
 * @param isEligibleForIntroOffer Whether the user is eligible for an intro offer.
 * @param state Current view state (selected / unselected).
 *
 * @return A presentable partial component, or null if [this] [PresentedOverrides] did not contain any
 * available overrides to use.
 */
@JvmSynthetic
internal fun <T : PresentedPartial<T>> PresentedOverrides<T>.buildPresentedPartial(
    windowSize: ScreenCondition,
    isEligibleForIntroOffer: Boolean,
    state: ComponentViewState,
): T? {
    var conditionPartial = buildScreenConditionPartial(windowSize)

    if (isEligibleForIntroOffer) {
        // If conditionPartial is null here, we want to continue with the introOffer partial.
        conditionPartial = conditionPartial.combineOrReplace(introOffer)
    }

    when (state) {
        ComponentViewState.DEFAULT -> {
            // Nothing to do.
        }

        ComponentViewState.SELECTED -> {
            // If conditionPartial is null here, we want to continue with the selected state partial.
            conditionPartial = conditionPartial.combineOrReplace(states?.selected)
        }
    }

    return conditionPartial
}

/**
 * Builds a partial component based on the current screen conditions.
 * @param currentScreenCondition Screen size condition.
 * @return Configured partial component for the given screen condition, or null if this
 * `PresentedOverrides.conditions` is null, which means there are no overrides defined for screen conditions.
 */
private fun <T : PresentedPartial<T>> PresentedOverrides<T>.buildScreenConditionPartial(
    currentScreenCondition: ScreenCondition,
): T? {
    val availableScreenOverrides: PresentedConditions<T>? = conditions
    val applicableScreenOverrides: List<T> = currentScreenCondition.applicableConditions
        .mapNotNull { type ->
            when (type) {
                ScreenCondition.COMPACT -> availableScreenOverrides?.compact
                ScreenCondition.MEDIUM -> availableScreenOverrides?.medium
                ScreenCondition.EXPANDED -> availableScreenOverrides?.expanded
            }
        }

    // Combine all applicable overrides into a single one.
    return applicableScreenOverrides.reduceOrNull { partial, next ->
        partial.combine(with = next)
    }
}

private val ScreenCondition.applicableConditions: List<ScreenCondition>
    get() = when (this) {
        ScreenCondition.COMPACT -> listOf(ScreenCondition.COMPACT)
        ScreenCondition.MEDIUM -> listOf(ScreenCondition.COMPACT, ScreenCondition.MEDIUM)
        ScreenCondition.EXPANDED -> listOf(
            ScreenCondition.COMPACT,
            ScreenCondition.MEDIUM,
            ScreenCondition.EXPANDED,
        )
    }
