package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A named state key declared in a paywall's top-level `state_declarations` map (state-driven paywalls).
 */
@InternalRevenueCatAPI
@Poko
@Serializable
public class StateDeclaration(
    @get:JvmSynthetic public val type: ValueType,
    @get:JvmSynthetic @SerialName("default") public val defaultValue: JsonPrimitive,
) {

    /** Declared type of a state key. Unrecognized wire types decode to [UNKNOWN]. */
    @Serializable(with = ValueTypeSerializer::class)
    public enum class ValueType {
        // SerialNames are handled by the ValueTypeSerializer.
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING,
        UNKNOWN,
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object ValueTypeSerializer : EnumDeserializerWithDefault<StateDeclaration.ValueType>(
    defaultValue = StateDeclaration.ValueType.UNKNOWN,
)

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
                val declaration = jsonDecoder.json.decodeFromJsonElement(StateDeclaration.serializer(), element)
                // Parity with iOS, where a null default fails ConditionValue decoding: drop the declaration.
                declaration.takeIf { it.defaultValue !is JsonNull }?.let { key to it }
            } catch (_: IllegalArgumentException) {
                null
            }
        }.toMap()
    }

    override fun serialize(encoder: Encoder, value: Map<String, StateDeclaration>) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
