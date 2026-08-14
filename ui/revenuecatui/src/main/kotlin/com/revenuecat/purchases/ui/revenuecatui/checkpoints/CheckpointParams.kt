package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Per-call parameters for [com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint].
 *
 * [customVariables] values must be [String], [Int], [Long], [Double], [Float] or [Boolean]. Invalid values are
 * dropped with a warning. Keys must start with a letter and contain only letters, numbers and underscores;
 * anything else is dropped when the value is used, since it cannot be addressed as `custom.<key>`.
 */
@InternalRevenueCatAPI
public class CheckpointParams(
    customVariables: Map<String, Any?> = emptyMap(),
) {

    public constructor(vararg customVariables: Pair<String, Any?>) : this(customVariables.toMap())

    public val customVariables: Map<String, Any> = customVariables.mapNotNull { (key, value) ->
        when (value) {
            is String, is Int, is Long, is Double, is Float, is Boolean -> key to value
            else -> {
                Logger.w("Dropping invalid checkpoint custom variable '$key': ${value?.javaClass?.name ?: "null"}")
                null
            }
        }
    }.toMap()

    override fun equals(other: Any?): Boolean =
        other is CheckpointParams && other.customVariables == customVariables

    override fun hashCode(): Int = customVariables.hashCode()

    override fun toString(): String = "CheckpointParams(customVariables=$customVariables)"
}

// Safe because the constructor drops every value type CustomVariableValue.from does not accept. Keys are left
// alone here: PaywallOptions.Builder.setCustomVariables validates them for the paywall, and rule evaluation
// applies the same policy on its own side.
@OptIn(InternalRevenueCatAPI::class)
internal val CheckpointParams.paywallCustomVariables: Map<String, CustomVariableValue>
    get() = customVariables.mapValues { (_, value) -> CustomVariableValue.from(value) }
