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
        val providerName: String,
        val reason: String,
    ) : RulesDimensionResolutionException("dimension provider '$providerName' failed: $reason")

    internal data class ConflictingDimension(
        val path: String,
    ) : RulesDimensionResolutionException("two dimension sources supplied '$path'")
}

/**
 * Builds the scope local rule evaluation runs against by collecting every provider once and merging its values
 * into a single root scope. Every snapshot also carries the evaluation instant as `evaluated_at`.
 *
 * All providers see the same reference instant, so every dimension in one snapshot is consistent with the others.
 *
 * Both failure modes are configuration bugs rather than runtime conditions — a provider that cannot produce its
 * values, and two sources claiming the same root name — so they fail the whole snapshot instead of silently
 * degrading a rule to a non-match, which would be indistinguishable from a customer who genuinely does not match.
 * Cancellation is neither, and propagates.
 */
internal class RulesDimensionResolver(
    private val providers: List<RulesDimensionProvider>,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    /**
     * [customVariables] are the caller's own values for this one evaluation, exposed under `custom`. That root
     * is reserved whether or not this evaluation supplies values for it, so a provider colliding with it is
     * deterministic rather than dependent on call arguments.
     */
    @Suppress("ReturnCount")
    suspend fun snapshot(
        customVariables: Map<String, RulesDimensionValue> = emptyMap(),
    ): Result<RulesDimensionSnapshot> {
        val date = dateProvider.now
        // Seeded before any provider runs, so a provider claiming this root is an ordinary collision.
        val values = mutableMapOf<String, Value>(KEY_EVALUATED_AT to Value.IntValue(date.time))

        for (provider in providers) {
            val dimensions = try {
                provider.dimensions(date)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                return Result.failure(
                    RulesDimensionResolutionException.ProviderFailed(
                        providerName = provider.name,
                        reason = error.message ?: error.toString(),
                    ),
                )
            }
            values.addRootDimensions(dimensions)?.let { conflict ->
                return Result.failure(conflict)
            }
        }

        values.addNestedDimensions(KEY_CUSTOM, customVariables)?.let { conflict ->
            return Result.failure(conflict)
        }

        return Result.success(
            RulesDimensionSnapshot(
                values = values,
                evaluationDate = date,
            ),
        )
    }
}

/** Merges [dimensions] into the root scope, or returns the conflict that stops the whole snapshot. */
private fun MutableMap<String, Value>.addRootDimensions(
    dimensions: Map<String, RulesDimensionValue>,
): RulesDimensionResolutionException.ConflictingDimension? {
    for ((name, value) in dimensions.filterReachable(root = null)) {
        if (containsKey(name) || name == KEY_CUSTOM) {
            return RulesDimensionResolutionException.ConflictingDimension(name)
        }
        this[name] = value.asRulesEngineValue
    }
    return null
}

/** Nests [dimensions] under [root], or returns the conflict that stops the whole snapshot. */
@Suppress("ReturnCount")
private fun MutableMap<String, Value>.addNestedDimensions(
    root: String,
    dimensions: Map<String, RulesDimensionValue>,
): RulesDimensionResolutionException.ConflictingDimension? {
    // Filtered before the check below, so a source whose every name was unreachable still contributes no root.
    val reachable = dimensions.filterReachable(root)
    // A source with nothing to say contributes no root at all: an empty object is truthy in JSON Logic, so
    // `{"var": "custom"}` would read as present for an evaluation that supplied no custom values.
    if (reachable.isEmpty()) return null
    if (containsKey(root)) {
        return RulesDimensionResolutionException.ConflictingDimension(root)
    }
    this[root] = Value.ObjectValue(reachable.mapValues { (_, value) -> value.asRulesEngineValue })
    return null
}

/**
 * Drops the names no predicate could ever read, at any depth; see [String.isReachableDimensionName]. Names inside
 * [RulesDimensionValue.ObjectValue] and [RulesDimensionValue.ObjectListValue] records are one `var` path segment
 * each, so the same rule applies to them, one entry at a time. An object left with nothing readable is dropped
 * along with its names, for the same reason [addNestedDimensions] contributes no root for an empty source: an
 * empty object is truthy in JSON Logic, so keeping it would make `{"var": "profile"}` read as present.
 *
 * Dropped rather than failing the snapshot, unlike this resolver's other two rejections: custom variables are
 * named by the app, so a single value the SDK cannot expose must not stop every checkpoint from resolving. Same
 * treatment `CustomVariableKeyValidator` gives them at registration time, and
 * [SubscriberAttributesDimensionProvider] gives the other developer-named source.
 */
private fun Map<String, RulesDimensionValue>.filterReachable(
    root: String?,
): Map<String, RulesDimensionValue> = mapNotNull { (name, value) ->
    val path = root?.let { "$it$DIMENSION_PATH_SEPARATOR$name" } ?: name
    if (!name.isReachableDimensionName) {
        warnLog {
            "Ignoring dimension '$path': a dimension name can't be blank or contain '$DIMENSION_PATH_SEPARATOR'."
        }
        null
    } else {
        value.filterReachable(path)?.let { reachable -> name to reachable }
    }
}.toMap()

private fun RulesDimensionValue.filterReachable(path: String): RulesDimensionValue? = when (this) {
    is RulesDimensionValue.ObjectValue -> value.filterReachable(root = path)
        .takeIf { it.isNotEmpty() }
        ?.let { RulesDimensionValue.ObjectValue(it) }
    is RulesDimensionValue.ObjectListValue -> RulesDimensionValue.ObjectListValue(
        value.mapIndexedNotNull { index, record ->
            record.filterReachable(root = "$path$DIMENSION_PATH_SEPARATOR$index").takeIf { it.isNotEmpty() }
        },
    )
    else -> this
}

/**
 * Whether a predicate could read this name. The engine's `var` walks a strict dot-path, so a name containing a
 * `.` would be read as a path through a nested object that does not exist, and a blank one is not a name a
 * predicate can be written against.
 */
internal val String.isReachableDimensionName: Boolean
    get() = isNotBlank() && !contains(DIMENSION_PATH_SEPARATOR)

internal const val DIMENSION_PATH_SEPARATOR = '.'

private const val KEY_EVALUATED_AT = "evaluated_at"
private const val KEY_CUSTOM = "custom"

private val RulesDimensionValue.asRulesEngineValue: Value
    get() = when (this) {
        is RulesDimensionValue.StringValue -> Value.StringValue(value)
        is RulesDimensionValue.BoolValue -> Value.BoolValue(value)
        is RulesDimensionValue.IntValue -> Value.IntValue(value)
        is RulesDimensionValue.DoubleValue -> Value.FloatValue(value)
        is RulesDimensionValue.DateValue -> Value.IntValue(value.time)
        is RulesDimensionValue.NullValue -> Value.Null
        is RulesDimensionValue.ObjectListValue -> Value.ArrayValue(
            value.map { record -> Value.ObjectValue(record.mapValues { (_, item) -> item.asRulesEngineValue }) },
        )
        is RulesDimensionValue.ObjectValue -> Value.ObjectValue(
            value.mapValues { (_, item) -> item.asRulesEngineValue },
        )
    }
