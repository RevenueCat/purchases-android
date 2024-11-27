package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Serializable
@JvmInline
internal value class LocaleId(@get:JvmSynthetic val value: String)

@InternalRevenueCatAPI
@Serializable
@JvmInline
value class LocalizationKey internal constructor(@get:JvmSynthetic val value: String)

@InternalRevenueCatAPI
typealias LocalizationDictionary = Map<LocalizationKey, LocalizationData>

/**
 * A make-shift union type. LocalizationData is either a plain String or a ThemeImageUrls object.
 */
@InternalRevenueCatAPI
@Serializable(with = LocalizationDataSerializer::class)
sealed interface LocalizationData {
    @Serializable
    @JvmInline
    value class Text internal constructor(@get:JvmSynthetic val value: String) : LocalizationData

    @Serializable
    @JvmInline
    value class Image internal constructor(@get:JvmSynthetic val value: ThemeImageUrls) : LocalizationData
}

@OptIn(InternalRevenueCatAPI::class)
private object LocalizationDataSerializer : KSerializer<LocalizationData> {
    // Documentation says to use either PrimitiveSerialDescriptor or buildClassSerialDescriptor. However, we need a
    // polymorphic descriptor that's either a primitive (string) or a class. So neither of those options fit the bill.
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "LocalizationData",
        kind = PolymorphicKind.SEALED,
    )

    override fun serialize(encoder: Encoder, value: LocalizationData) {
        error("Serialization is not implemented as it is not (yet) needed.")
    }

    @Suppress("SwallowedException")
    override fun deserialize(decoder: Decoder): LocalizationData =
        // We have no `type` descriptor field, so we resort to trial and error.
        try {
            decoder.decodeSerializableValue(LocalizationData.Text.serializer())
        } catch (e: SerializationException) {
            decoder.decodeSerializableValue(LocalizationData.Image.serializer())
        }
}
