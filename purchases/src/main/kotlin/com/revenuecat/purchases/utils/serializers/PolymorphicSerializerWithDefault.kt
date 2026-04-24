package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KClass

/** Returns true only when [key] is present in the object and its value is a non-null JSON primitive. */
internal fun JsonObject.hasNonNullField(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.contentOrNull != null

/**
 * [JsonContentPolymorphicSerializer] that dispatches on a string discriminator field and
 * falls back to [unknownSerializer] for any unrecognized value, a missing discriminator, or
 * a subclass whose fields couldn't be parsed — indicated by [selectByType] returning `null`.
 *
 * For the common case where each type maps to a single required field, pass a [serializers] map
 * of `type -> (requiredField to serializer)` instead of overriding [selectByType].
 */
internal abstract class PolymorphicSerializerWithDefault<T : Any>(
    baseClass: KClass<T>,
    private val unknownSerializer: DeserializationStrategy<T>,
    private val typeField: String = "type",
    private val serializers: Map<String, Pair<String, DeserializationStrategy<T>>> = emptyMap(),
) : JsonContentPolymorphicSerializer<T>(baseClass) {

    final override fun selectDeserializer(element: JsonElement): DeserializationStrategy<T> {
        val obj = element.jsonObject
        val type = (obj[typeField] as? JsonPrimitive)?.content ?: return unknownSerializer
        return serializers[type]
            ?.let { (requiredField, serializer) ->
                if (obj.hasNonNullField(requiredField)) serializer else unknownSerializer
            }
            ?: selectByType(type, obj)
            ?: unknownSerializer
    }

    /**
     * Returns the serializer for the given discriminator value, or `null` to fall back to the
     * default unknown serializer. Only needed for types not covered by the [serializers] map
     * (e.g. when multiple required fields must be checked).
     */
    protected open fun selectByType(type: String, element: JsonObject): DeserializationStrategy<T>? = null
}
