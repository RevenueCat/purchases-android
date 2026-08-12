@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.rules.Value
import kotlinx.coroutines.CancellationException
import java.util.Date

/**
 * An immutable, point-in-time scope ready to be handed to the rules engine.
 */
internal data class RulesDimensionSnapshot(
    val values: Map<String, Value>,
    val evaluationDate: Date,
)

internal sealed class RulesDimensionResolutionException(message: String) : Exception(message) {

    internal data class ProviderFailed(
        val identifier: String,
        val reason: String,
    ) : RulesDimensionResolutionException("dimension provider '$identifier' failed: $reason")

    internal data class ConflictingDimension(
        val path: String,
    ) : RulesDimensionResolutionException("two dimension providers supplied '$path'")
}

/**
 * Builds the scope local rule evaluation runs against by collecting every provider once and nesting its values
 * under the provider's namespace.
 *
 * All providers see the same reference instant, so every dimension in one snapshot is consistent with the others.
 *
 * Both failure modes are configuration bugs rather than runtime conditions — a provider that cannot produce its
 * values, and two providers claiming the same path — so they fail the whole snapshot instead of silently
 * degrading a rule to a non-match, which would be indistinguishable from a customer who genuinely does not match.
 * Cancellation is neither, and propagates.
 */
internal class RulesDimensionResolver(
    private val providers: List<RulesDimensionProvider>,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    /**
     * [customVariables] are the caller's own values for this one evaluation, exposed under
     * [RulesDimensionNamespace.Custom]. An empty map contributes no namespace at all rather than an empty object,
     * which is truthy in JSON Logic: a call with no values should read as absent, not as present-but-empty.
     */
    @Suppress("ReturnCount")
    suspend fun snapshot(
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
    ): Result<RulesDimensionSnapshot> {
        val date = dateProvider.now
        val values = mutableMapOf<String, MutableMap<String, Value>>()

        for (provider in providers) {
            val dimensions = try {
                provider.dimensions(date)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                return Result.failure(
                    RulesDimensionResolutionException.ProviderFailed(
                        identifier = provider.identifier,
                        reason = error.message ?: error.toString(),
                    ),
                )
            }
            values.addDimensions(provider.namespace, dimensions)?.let { conflict ->
                return Result.failure(conflict)
            }
        }

        if (customVariables.isNotEmpty()) {
            values.addDimensions(RulesDimensionNamespace.Custom, customVariables)?.let { conflict ->
                return Result.failure(conflict)
            }
        }

        return Result.success(
            RulesDimensionSnapshot(
                values = values.mapValues { (_, dimensions) -> Value.ObjectValue(dimensions) },
                evaluationDate = date,
            ),
        )
    }
}

/** Nests [dimensions] under [namespace], or returns the conflict that stops the whole snapshot. */
private fun MutableMap<String, MutableMap<String, Value>>.addDimensions(
    namespace: RulesDimensionNamespace,
    dimensions: Map<String, RulesDimensionValue>,
): RulesDimensionResolutionException.ConflictingDimension? {
    val target = getOrPut(namespace.key) { mutableMapOf() }
    for ((name, value) in dimensions) {
        if (target.containsKey(name)) {
            return RulesDimensionResolutionException.ConflictingDimension("${namespace.key}.$name")
        }
        target[name] = value.asRulesEngineValue
    }
    return null
}

private val RulesDimensionValue.asRulesEngineValue: Value
    get() = when (this) {
        is RulesDimensionValue.StringValue -> Value.StringValue(value)
        is RulesDimensionValue.BoolValue -> Value.BoolValue(value)
        is RulesDimensionValue.IntValue -> Value.IntValue(value)
        is RulesDimensionValue.DoubleValue -> Value.FloatValue(value)
    }
