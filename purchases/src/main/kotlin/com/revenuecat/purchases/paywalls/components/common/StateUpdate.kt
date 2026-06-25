package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A declarative state mutation on an interactive component (state-driven paywalls). Decode-only in this phase: only
 * `set` is defined, and unknown or malformed shapes decode to [Unsupported] so future operations stay additive.
 */
@InternalRevenueCatAPI
@Serializable(with = StateUpdateSerializer::class)
public sealed interface StateUpdate {

    /** Wire shape: `{ "set": "<key>", "to": <value | "$value"> }`. */
    @InternalRevenueCatAPI
    @Poko
    public class Set(
        @get:JvmSynthetic public val key: String,
        @get:JvmSynthetic public val value: StateUpdateValue,
    ) : StateUpdate

    @InternalRevenueCatAPI
    public object Unsupported : StateUpdate
}

/**
 * The `to` value of a [StateUpdate.Set]: a [Literal] value or a [PayloadReference] (the wire sentinel `"$value"`).
 */
@InternalRevenueCatAPI
public sealed interface StateUpdateValue {

    @InternalRevenueCatAPI
    @Poko
    public class Literal(@get:JvmSynthetic public val value: JsonPrimitive) : StateUpdateValue

    @InternalRevenueCatAPI
    public object PayloadReference : StateUpdateValue

    @InternalRevenueCatAPI
    public companion object {
        public const val PAYLOAD_REFERENCE_TOKEN: String = "\$value"
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object StateUpdateSerializer : KSerializer<StateUpdate> {
    private const val KEY_SET = "set"
    private const val KEY_TO = "to"

    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    @Suppress("ReturnCount")
    override fun deserialize(decoder: Decoder): StateUpdate {
        val jsonDecoder = decoder as? JsonDecoder ?: return StateUpdate.Unsupported
        val jsonObject = jsonDecoder.decodeJsonElement() as? JsonObject ?: return StateUpdate.Unsupported
        val key = (jsonObject[KEY_SET] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: return StateUpdate.Unsupported
        val toElement = jsonObject[KEY_TO] as? JsonPrimitive ?: return StateUpdate.Unsupported
        val value = if (toElement.isString && toElement.content == StateUpdateValue.PAYLOAD_REFERENCE_TOKEN) {
            StateUpdateValue.PayloadReference
        } else {
            StateUpdateValue.Literal(toElement)
        }
        return StateUpdate.Set(key = key, value = value)
    }

    override fun serialize(encoder: Encoder, value: StateUpdate) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
