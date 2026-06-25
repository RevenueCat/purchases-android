package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A named state key declared in a paywall's top-level `state_declarations` map (state-driven paywalls).
 */
@InternalRevenueCatAPI
@Poko
@Serializable
public class StateDeclaration(
    // Raw String rather than an enum so unknown types decode without failing.
    @get:JvmSynthetic public val type: String,
    @get:JvmSynthetic @SerialName("default") public val defaultValue: JsonPrimitive,
) {

    // Constants rather than an enum so unrecognized wire types are preserved.
    @InternalRevenueCatAPI
    public object ValueType {
        public const val BOOLEAN: String = "boolean"
        public const val INTEGER: String = "integer"
        public const val DOUBLE: String = "double"
        public const val STRING: String = "string"
    }
}

/**
 * Decodes the top-level `state_declarations` map resiliently: individual malformed entries are dropped and a
 * non-object value decodes to an empty map, so a broken declaration never fails the whole paywall.
 */
@OptIn(InternalRevenueCatAPI::class)
internal object StateDeclarationMapSerializer : KSerializer<Map<String, StateDeclaration>> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    @Suppress("ReturnCount")
    override fun deserialize(decoder: Decoder): Map<String, StateDeclaration> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyMap()
        val jsonObject = jsonDecoder.decodeJsonElement() as? JsonObject ?: return emptyMap()
        return jsonObject.mapNotNull { (key, element) ->
            try {
                key to jsonDecoder.json.decodeFromJsonElement(StateDeclaration.serializer(), element)
            } catch (_: IllegalArgumentException) {
                null
            }
        }.toMap()
    }

    override fun serialize(encoder: Encoder, value: Map<String, StateDeclaration>) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
