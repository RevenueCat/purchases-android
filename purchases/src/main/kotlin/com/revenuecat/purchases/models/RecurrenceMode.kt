package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Recurrence mode for a pricing phase
 */
@SuppressWarnings("MagicNumber")
public enum class RecurrenceMode(@ProductDetails.RecurrenceMode val identifier: Int?) {

    // Pricing phase repeats infinitely until cancellation
    INFINITE_RECURRING(1),

    // Pricing phase repeats for a fixed number of billing periods
    FINITE_RECURRING(2),

    // Pricing phase does not repeat
    NON_RECURRING(3),
    UNKNOWN(null),
}

public fun Int?.toRecurrenceMode(): RecurrenceMode =
    RecurrenceMode.values().firstOrNull { it.identifier == this } ?: RecurrenceMode.UNKNOWN

internal object RecurrenceModeSerializer : KSerializer<RecurrenceMode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RecurrenceMode") {
        element("name", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: RecurrenceMode) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
        }
    }

    override fun deserialize(decoder: Decoder): RecurrenceMode {
        return decoder.decodeStructure(descriptor) {
            var name = ""

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, 0)
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            RecurrenceMode.valueOf(name)
        }
    }
}
