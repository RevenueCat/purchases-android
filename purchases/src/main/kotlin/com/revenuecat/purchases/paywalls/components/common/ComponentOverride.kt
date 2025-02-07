package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
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
    sealed interface Condition {
        object Compact : Condition
        object Medium : Condition
        object Expanded : Condition
        object IntroOffer : Condition
        object MultipleIntroOffers : Condition
        object Selected : Condition
        object Unsupported : Condition
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
            var typeValue: String? = null

            loop@ while (true) {
                when (composite.decodeElementIndex(descriptor)) {
                    0 -> typeValue = composite.decodeStringElement(descriptor, 0)
                    else -> break@loop
                }
            }
            composite.endStructure(descriptor)

            return when (typeValue) {
                "compact" -> Condition.Compact
                "medium" -> Condition.Medium
                "expanded" -> Condition.Expanded
                "intro_offer" -> Condition.IntroOffer
                "multiple_intro_offers" -> Condition.MultipleIntroOffers
                "selected" -> Condition.Selected
                else -> Condition.Unsupported
            }
        }
    }
}
