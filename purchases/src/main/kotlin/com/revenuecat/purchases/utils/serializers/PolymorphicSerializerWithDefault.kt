package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

/**
 * [JsonContentPolymorphicSerializer] that dispatches on a string discriminator field and
 * falls back to [unknownSerializer] for any unrecognized value, a missing discriminator, or
 * a subclass whose fields couldn't be parsed — indicated by [selectByType] returning `null`.
 */
internal abstract class PolymorphicSerializerWithDefault<T : Any>(
    baseClass: KClass<T>,
    private val unknownSerializer: DeserializationStrategy<T>,
    private val typeField: String = "type",
) : JsonContentPolymorphicSerializer<T>(baseClass) {

    final override fun selectDeserializer(element: JsonElement): DeserializationStrategy<T> {
        val obj = element.jsonObject
        val type = obj[typeField]?.jsonPrimitive?.content ?: return unknownSerializer
        return selectByType(type, obj) ?: unknownSerializer
    }

    /**
     * Returns the serializer for the given discriminator value, or `null` to fall back
     * to the default unknown serializer (e.g. when a subclass's required fields are missing).
     */
    protected abstract fun selectByType(type: String, element: JsonObject): DeserializationStrategy<T>?
}
