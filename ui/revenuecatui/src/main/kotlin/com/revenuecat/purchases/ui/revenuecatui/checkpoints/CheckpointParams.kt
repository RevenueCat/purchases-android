package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * Per-call parameters for [com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint].
 *
 * [customProperties] values must be [String], [Int], [Long], [Double], [Float] or [Boolean]. Invalid values are
 * dropped with a warning.
 */
@InternalRevenueCatAPI
public class CheckpointParams(
    customProperties: Map<String, Any?> = emptyMap(),
) {

    public constructor(vararg customProperties: Pair<String, Any?>) : this(customProperties.toMap())

    public val customProperties: Map<String, Any> = customProperties.mapNotNull { (key, value) ->
        when (value) {
            is String, is Int, is Long, is Double, is Float, is Boolean -> key to value
            else -> {
                Logger.w("Dropping invalid checkpoint custom property '$key': ${value?.javaClass?.name ?: "null"}")
                null
            }
        }
    }.toMap()

    override fun equals(other: Any?): Boolean =
        other is CheckpointParams && other.customProperties == customProperties

    override fun hashCode(): Int = customProperties.hashCode()

    override fun toString(): String = "CheckpointParams(customProperties=$customProperties)"
}

// Safe because the constructor drops every value type CustomVariableValue.from does not accept. Keys are left
// alone here: PaywallOptions.Builder.setCustomVariables validates them for the paywall, while the rules engine
// takes them unfiltered.
@OptIn(InternalRevenueCatAPI::class)
internal val CheckpointParams.customVariables: Map<String, CustomVariableValue>
    get() = customProperties.mapValues { (_, value) -> CustomVariableValue.from(value) }
