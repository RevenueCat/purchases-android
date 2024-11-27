package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides

/**
 * Partial components transformed and ready for presentation.
 */
internal sealed interface PresentedPartial<T : PresentedPartial<T>> {
    // FIXME This whole file is a test candidate.

    companion object {
        /**
         * Builds a presentable partial component based on current view state and screen condition.
         *
         * @param state Current view state (selected / unselected).
         * @param condition Current screen condition (compact / medium / expanded).
         * @param with Override configurations to apply.
         */
        @JvmSynthetic
        fun <T : PresentedPartial<T>> buildPartial(
            state: ComponentViewState,
            condition: ScreenCondition,
            isEligibleForIntroOffer: Boolean,
            with: PresentedOverrides<T>?,
        ): T? {
            var conditionPartial = buildScreenConditionPartial(condition, with)

            if (isEligibleForIntroOffer) {
                conditionPartial = conditionPartial?.combine(with?.introOffer)
            }

            when (state) {
                ComponentViewState.DEFAULT -> {
                    // Nothing to do.
                }

                ComponentViewState.SELECTED -> conditionPartial = conditionPartial?.combine(with?.states?.selected)
            }

            return conditionPartial
        }

        /**
         * Builds a partial component based on screen conditions.
         * @param condition Screen size condition.
         * @param presentedOverrides Override configurations to apply.
         * @return Configured partial component for the given screen condition.
         */
        private fun <T : PresentedPartial<T>> buildScreenConditionPartial(
            condition: ScreenCondition,
            presentedOverrides: PresentedOverrides<T>?,
        ): T? {
            val conditions = presentedOverrides?.conditions
            val applicableConditions: List<T> = condition.applicableConditions
                .mapNotNull { type ->
                    when (type) {
                        ScreenCondition.COMPACT -> conditions?.compact
                        ScreenCondition.MEDIUM -> conditions?.medium
                        ScreenCondition.EXPANDED -> conditions?.expanded
                    }
                }

            return applicableConditions.reduceOrNull { partial, next ->
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
    }

    /**
     * Combines this partial component with another, allowing for override behavior.
     *
     * @param with The overriding partial component.
     *
     * @return The combined partial component.
     */
    @JvmSynthetic
    fun combine(with: T?): T
// TODO This needs to support a null receiver / be static. Return should still be non-null.
}

/**
 * Structure holding override configurations for different presentation states.
 */
internal class PresentedOverrides<T : PresentedPartial<T>>(
    /**
     * Override for different selection states.
     */
    @get:JvmSynthetic val introOffer: T?,
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
internal class PresentedStates<T : PresentedPartial<T>>(
    /**
     * Override for selected state
     */
    @get:JvmSynthetic val selected: T?,
)

/**
 * Structure defining overrides for different screen size conditions.
 */
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
    transform: (T) -> Result<P>,
): Result<PresentedOverrides<P>> {
    val introOffer = introOffer?.let(transform)
        ?.getOrElse { return Result.failure(it) }

    val selectedState = states?.selected?.let(transform)
        ?.getOrElse { return Result.failure(it) }

    val states = states?.let { PresentedStates(selected = selectedState) }

    val conditions = conditions?.let { conditions ->
        PresentedConditions(
            compact = conditions.compact?.let(transform)?.getOrElse { return Result.failure(it) },
            medium = conditions.compact?.let(transform)?.getOrElse { return Result.failure(it) },
            expanded = conditions.compact?.let(transform)?.getOrElse { return Result.failure(it) },
        )
    }

    return Result.success(
        PresentedOverrides(
            introOffer = introOffer,
            states = states,
            conditions = conditions,
        ),
    )
}
