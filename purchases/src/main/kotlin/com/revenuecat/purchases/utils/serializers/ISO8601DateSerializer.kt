package com.revenuecat.purchases.utils.serializers

import com.revenuecat.purchases.utils.Iso8601Utils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

internal object ISO8601DateSerializer : KSerializer<Date> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Date {
        val isoDateString = decoder.decodeString()
        return Iso8601Utils.parse(isoDateString)
    }

    override fun serialize(encoder: Encoder, value: Date) {
        val isoDateString = Iso8601Utils.format(value)
        encoder.encodeString(isoDateString)
    }
}
