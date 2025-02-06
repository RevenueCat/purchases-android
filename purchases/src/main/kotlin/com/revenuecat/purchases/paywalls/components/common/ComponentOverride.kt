package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.paywalls.components.PartialComponent
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentOverride<T : PartialComponent>(
    @get:JvmSynthetic val conditions: List<Condition>,
    @get:JvmSynthetic val properties: T,
) {

    @Serializable(with = ConditionSerializer::class)
    enum class Condition {
        COMPACT,
        MEDIUM,
        EXPANDED,
        INTRO_OFFER,
        MULTIPLE_INTRO_OFFERS,
        SELECTED,
        UNSUPPORTED,
    }

    private object ConditionSerializer : KSerializer<Condition> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ComponentOverride.Condition") {
            element<String>("type")
        }

        override fun serialize(encoder: Encoder, value: Condition) {
            // Serialization is not implemented as it is not needed.
        }

        override fun deserialize(decoder: Decoder): Condition {
            val composite = decoder.beginStructure(descriptor)
            var result: Condition? = null

            loop@ while (true) {
                when (val index = composite.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> {
                        val typeValue = composite.decodeStringElement(descriptor, 0)
                        result = try {
                            Condition.valueOf(typeValue.uppercase())
                        } catch (_: IllegalArgumentException) {
                            warnLog("Paywalls: Unknown override condition. Value: $typeValue. Override won't apply.")
                            Condition.UNSUPPORTED
                        }
                    }
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            composite.endStructure(descriptor)
            return result ?: throw SerializationException("Missing 'type' property")
        }
    }
}
