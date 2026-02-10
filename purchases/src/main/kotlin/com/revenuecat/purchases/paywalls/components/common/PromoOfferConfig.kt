package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

/**
 * Configuration for a promo offer to use for a package in a paywall.
 * On Android, this maps to a Play Store offer ID.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class PromoOfferConfig(
    /**
     * The offer identifier to use for this package.
     * On Android, this should match the offer ID configured in Google Play Console.
     */
    @get:JvmSynthetic
    @SerialName("offer_id")
    val offerId: String,
)

@OptIn(InternalRevenueCatAPI::class)
internal object ResilientPromoOfferConfigSerializer : KSerializer<PromoOfferConfig?> {
    private val delegate = PromoOfferConfig.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): PromoOfferConfig? {
        return try {
            val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonObject) {
                jsonDecoder.json.decodeFromJsonElement(delegate, element)
            } else {
                null
            }
        } catch (e: SerializationException) {
            warnLog { "Failed to deserialize PromoOfferConfig, defaulting to null: ${e.message}" }
            null
        }
    }

    override fun serialize(encoder: Encoder, value: PromoOfferConfig?) {
        if (value != null) {
            delegate.serialize(encoder, value)
        }
    }
}
