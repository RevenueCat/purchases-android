package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonObject.toStringAnyMap(): Map<String, Any> =
    entries.mapNotNull { (key, value) ->
        val mapped = value.toAny() ?: return@mapNotNull null
        key to mapped
    }.toMap()

private fun JsonElement.toAny(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull!!
        intOrNull != null -> intOrNull!!
        longOrNull != null -> longOrNull!!
        doubleOrNull != null -> doubleOrNull!!
        else -> content
    }
    is JsonObject -> toStringAnyMap()
    is JsonArray -> mapNotNull { it.toAny() }
}
