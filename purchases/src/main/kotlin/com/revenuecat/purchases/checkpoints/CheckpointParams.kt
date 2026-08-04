package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog

/**
 * Per-call parameters for [com.revenuecat.purchases.Purchases.checkpoint].
 *
 * [customProperties] values must be [String], [Number] or [Boolean]. Invalid values are dropped with a warning.
 */
@InternalRevenueCatAPI
public class CheckpointParams(
    customProperties: Map<String, Any?> = emptyMap(),
) {

    public constructor(vararg customProperties: Pair<String, Any?>) : this(customProperties.toMap())

    public val customProperties: Map<String, Any> = customProperties.mapNotNull { (key, value) ->
        when (value) {
            is String, is Number, is Boolean -> key to value
            else -> {
                warnLog { "Dropping invalid checkpoint custom property '$key': ${value?.javaClass?.name ?: "null"}" }
                null
            }
        }
    }.toMap()

    override fun toString(): String = "CheckpointParams(customProperties=$customProperties)"
}
