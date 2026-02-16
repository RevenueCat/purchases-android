package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
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
 * Builds a presentable partial component based on current view state, screen condition and offer eligibility.
 *
 * @param windowSize Current screen condition (compact / medium / expanded).
 * @param offerEligibility The offer eligibility, encoding both the offer type (intro/promo) and phase count.
 * @param state Current view state (selected / unselected).
 * @param selectedPackageId The identifier of the currently selected package, or null if none is selected.
 *
 * @return A presentable partial component, or null if [this] the list of [PresentedOverride] did not contain any
 * available overrides to use.
 */
@JvmSynthetic
internal fun <T : PresentedPartial<T>> List<PresentedOverride<T>>.buildPresentedPartial(
    windowSize: ScreenCondition,
    offerEligibility: OfferEligibility,
    state: ComponentViewState,
    selectedPackageId: String? = null,
): T? {
    var partial: T? = null
    for (override in this) {
        if (override.shouldApply(windowSize, offerEligibility, state, selectedPackageId)) {
            partial = partial.combineOrReplace(override.properties)
        }
    }
    return partial
}

private fun <T : PresentedPartial<T>> PresentedOverride<T>.shouldApply(
    windowSize: ScreenCondition,
    offerEligibility: OfferEligibility,
    state: ComponentViewState,
    selectedPackageId: String?,
): Boolean = conditions.all { condition ->
    condition.evaluate(windowSize, offerEligibility, state, selectedPackageId)
}

@Suppress("ReturnCount")
private fun ComponentOverride.Condition.evaluate(
    windowSize: ScreenCondition,
    offerEligibility: OfferEligibility,
    state: ComponentViewState,
    selectedPackageId: String?,
): Boolean = when (this) {
    ComponentOverride.Condition.Compact,
    ComponentOverride.Condition.Medium,
    ComponentOverride.Condition.Expanded,
    -> windowSize.applicableConditions.contains(this)
    ComponentOverride.Condition.MultiplePhaseOffers -> offerEligibility.hasMultipleDiscountedPhases
    is ComponentOverride.Condition.IntroOffer -> offerEligibility.isIntroOffer
    ComponentOverride.Condition.Selected -> state == ComponentViewState.SELECTED
    is ComponentOverride.Condition.PromoOffer -> offerEligibility.isPromoOffer
    is ComponentOverride.Condition.SelectedPackage -> evaluate(selectedPackageId)
    is ComponentOverride.Condition.Variable,
    ComponentOverride.Condition.Unsupported,
    -> false
}

private fun ComponentOverride.Condition.SelectedPackage.evaluate(selectedPackageId: String?): Boolean {
    if (selectedPackageId == null) return false
    return when (operator) {
        ComponentOverride.ArrayOperator.IN -> selectedPackageId in packages
        ComponentOverride.ArrayOperator.NOT_IN -> selectedPackageId !in packages
    }
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
