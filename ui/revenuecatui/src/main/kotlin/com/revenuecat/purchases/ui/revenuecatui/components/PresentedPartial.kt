@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
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
 * Context needed to evaluate conditions on component overrides.
 */
internal class ConditionContext(
    val selectedPackageId: String?,
    val customVariables: Map<String, CustomVariableValue>,
)

/**
 * Builds a presentable partial component based on current view state, screen condition and offer eligibility.
 *
 * @param windowSize Current screen condition (compact / medium / expanded).
 * @param offerEligibility The offer eligibility, encoding both the offer type (intro/promo) and phase count.
 * @param state Current view state (selected / unselected).
 * @param conditionContext Additional context for evaluating new condition types.
 *
 * @return A presentable partial component, or null if [this] the list of [PresentedOverride] did not contain any
 * available overrides to use.
 */
@JvmSynthetic
internal fun <T : PresentedPartial<T>> List<PresentedOverride<T>>.buildPresentedPartial(
    windowSize: ScreenCondition,
    offerEligibility: OfferEligibility,
    state: ComponentViewState,
    conditionContext: ConditionContext = ConditionContext(null, emptyMap()),
): T? {
    var partial: T? = null
    for (override in this) {
        if (override.shouldApply(windowSize, offerEligibility, state, conditionContext)) {
            partial = partial.combineOrReplace(override.properties)
        }
    }
    return partial
}

private fun <T : PresentedPartial<T>> PresentedOverride<T>.shouldApply(
    windowSize: ScreenCondition,
    offerEligibility: OfferEligibility,
    state: ComponentViewState,
    conditionContext: ConditionContext,
): Boolean = conditions.all { condition ->
    condition.evaluate(windowSize, offerEligibility, state, conditionContext)
}

private fun ComponentOverride.Condition.evaluate(
    windowSize: ScreenCondition,
    offerEligibility: OfferEligibility,
    state: ComponentViewState,
    conditionContext: ConditionContext,
): Boolean = when (this) {
    ComponentOverride.Condition.Compact,
    ComponentOverride.Condition.Medium,
    ComponentOverride.Condition.Expanded,
    -> windowSize.applicableConditions.contains(this)
    ComponentOverride.Condition.MultiplePhaseOffers -> offerEligibility.hasMultipleDiscountedPhases
    is ComponentOverride.Condition.IntroOffer -> evaluate(offerEligibility)
    ComponentOverride.Condition.Selected -> state == ComponentViewState.SELECTED
    is ComponentOverride.Condition.PromoOffer -> evaluate(offerEligibility)
    is ComponentOverride.Condition.SelectedPackage -> evaluate(conditionContext.selectedPackageId)
    is ComponentOverride.Condition.Variable -> evaluate(conditionContext.customVariables)
    ComponentOverride.Condition.Unsupported -> false
}

private fun ComponentOverride.Condition.IntroOffer.evaluate(offerEligibility: OfferEligibility): Boolean {
    val eligibility = offerEligibility.isIntroOffer
    val conditionValue = value ?: true
    return when (operator ?: ComponentOverride.EqualityOperator.EQUALS) {
        ComponentOverride.EqualityOperator.EQUALS -> eligibility == conditionValue
        ComponentOverride.EqualityOperator.NOT_EQUALS -> eligibility != conditionValue
    }
}

private fun ComponentOverride.Condition.PromoOffer.evaluate(offerEligibility: OfferEligibility): Boolean {
    val eligibility = offerEligibility.isPromoOffer
    val conditionValue = value ?: true
    return when (operator ?: ComponentOverride.EqualityOperator.EQUALS) {
        ComponentOverride.EqualityOperator.EQUALS -> eligibility == conditionValue
        ComponentOverride.EqualityOperator.NOT_EQUALS -> eligibility != conditionValue
    }
}

private fun ComponentOverride.Condition.SelectedPackage.evaluate(selectedPackageId: String?): Boolean {
    if (selectedPackageId == null) return false
    return when (operator) {
        ComponentOverride.ArrayOperator.IN -> selectedPackageId in packages
        ComponentOverride.ArrayOperator.NOT_IN -> selectedPackageId !in packages
    }
}

private fun ComponentOverride.Condition.Variable.evaluate(
    customVariables: Map<String, CustomVariableValue>,
): Boolean {
    val variableValue = customVariables[variable] ?: return operator == ComponentOverride.EqualityOperator.NOT_EQUALS
    val matches = matchesValue(variableValue)
    return when (operator) {
        ComponentOverride.EqualityOperator.EQUALS -> matches
        ComponentOverride.EqualityOperator.NOT_EQUALS -> !matches
    }
}

private fun ComponentOverride.Condition.Variable.matchesValue(
    variableValue: CustomVariableValue,
): Boolean {
    val conditionValue = value
    return when (conditionValue) {
        is ComponentOverride.ConditionValue.StringValue ->
            variableValue is CustomVariableValue.String && variableValue.value == conditionValue.value
        is ComponentOverride.ConditionValue.BoolValue ->
            variableValue is CustomVariableValue.Boolean && variableValue.value == conditionValue.value
        is ComponentOverride.ConditionValue.IntValue ->
            variableValue is CustomVariableValue.Number && variableValue.value == conditionValue.value.toDouble()
        is ComponentOverride.ConditionValue.DoubleValue ->
            variableValue is CustomVariableValue.Number && variableValue.value == conditionValue.value
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
