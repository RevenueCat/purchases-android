@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.warnLog
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
        val namespace: RulesDimensionNamespace,
        val reason: String,
    ) : RulesDimensionResolutionException("dimension provider '${namespace.key}' failed: $reason")

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
     * [RulesDimensionNamespace.Custom]. [backendValues] are the backend's pre-evaluated values for this one
     * evaluation, exposed under [RulesDimensionNamespace.Backend].
     */
    @Suppress("ReturnCount")
    suspend fun snapshot(
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
        backendValues: Map<String, RulesDimensionValue> = emptyMap(),
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
                        namespace = provider.namespace,
                        reason = error.message ?: error.toString(),
                    ),
                )
            }
            values.addDimensions(provider.namespace, dimensions)?.let { conflict ->
                return Result.failure(conflict)
            }
        }

        values.addDimensions(RulesDimensionNamespace.Custom, customVariables)?.let { conflict ->
            return Result.failure(conflict)
        }

        values.addDimensions(RulesDimensionNamespace.Backend, backendValues)?.let { conflict ->
            return Result.failure(conflict)
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
@Suppress("ReturnCount")
private fun MutableMap<String, MutableMap<String, Value>>.addDimensions(
    namespace: RulesDimensionNamespace,
    dimensions: Map<String, RulesDimensionValue>,
): RulesDimensionResolutionException.ConflictingDimension? {
    // Filtered before the check below, so a source whose every name was unreachable still contributes no namespace.
    val reachable = dimensions.filterReachable(namespace)
    // A source with nothing to say contributes no namespace at all: an empty object is truthy in JSON Logic, so
    // `{"var": "store"}` would read as present for a customer whose store never answered.
    if (reachable.isEmpty()) return null
    val target = getOrPut(namespace.key) { mutableMapOf() }
    for ((name, value) in reachable) {
        if (target.containsKey(name)) {
            return RulesDimensionResolutionException.ConflictingDimension("${namespace.key}.$name")
        }
        target[name] = value.asRulesEngineValue
    }
    return null
}

/**
 * Drops the names no predicate could ever read, at any depth. The engine's `var` walks a strict dot-path, so a
 * name containing a `.` would be read as a path through a nested object that does not exist, and an empty one is
 * not a name a predicate can be written against. Names inside [RulesDimensionValue.ObjectValue] and
 * [RulesDimensionValue.ObjectListValue] records are one `var` path segment each, so the same rule applies to
 * them, one entry at a time.
 *
 * Dropped rather than failing the snapshot, unlike this resolver's other two rejections: a namespace like
 * [RulesDimensionNamespace.SubscriberAttributes] is named by the app, so a single attribute the SDK cannot expose
 * must not stop every checkpoint from resolving. Same treatment `CustomVariableKeyValidator` gives the other
 * developer-named namespace.
 */
private fun Map<String, RulesDimensionValue>.filterReachable(
    namespace: RulesDimensionNamespace,
): Map<String, RulesDimensionValue> = filterReachable(parentPath = namespace.key)

private fun Map<String, RulesDimensionValue>.filterReachable(
    parentPath: String,
): Map<String, RulesDimensionValue> = mapNotNull { (name, value) ->
    val path = "$parentPath$DIMENSION_PATH_SEPARATOR$name"
    if (name.isEmpty() || name.contains(DIMENSION_PATH_SEPARATOR)) {
        warnLog {
            "Ignoring dimension '$path': a dimension name can't be empty or contain '$DIMENSION_PATH_SEPARATOR'."
        }
        null
    } else {
        name to value.filterReachable(path)
    }
}.toMap()

private fun RulesDimensionValue.filterReachable(path: String): RulesDimensionValue = when (this) {
    is RulesDimensionValue.ObjectValue -> RulesDimensionValue.ObjectValue(value.filterReachable(path))
    is RulesDimensionValue.ObjectListValue -> RulesDimensionValue.ObjectListValue(
        value.mapIndexed { index, record -> record.filterReachable("$path$DIMENSION_PATH_SEPARATOR$index") },
    )
    else -> this
}

private const val DIMENSION_PATH_SEPARATOR = '.'

private val RulesDimensionValue.asRulesEngineValue: Value
    get() = when (this) {
        is RulesDimensionValue.StringValue -> Value.StringValue(value)
        is RulesDimensionValue.BoolValue -> Value.BoolValue(value)
        is RulesDimensionValue.IntValue -> Value.IntValue(value)
        is RulesDimensionValue.DoubleValue -> Value.FloatValue(value)
        is RulesDimensionValue.DateValue -> Value.IntValue(value.time)
        is RulesDimensionValue.ObjectListValue -> Value.ArrayValue(
            value.map { record -> Value.ObjectValue(record.mapValues { (_, item) -> item.asRulesEngineValue }) },
        )
        is RulesDimensionValue.ObjectValue -> Value.ObjectValue(
            value.mapValues { (_, item) -> item.asRulesEngineValue },
        )
    }
