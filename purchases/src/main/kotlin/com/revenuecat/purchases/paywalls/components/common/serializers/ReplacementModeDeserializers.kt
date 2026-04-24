package com.revenuecat.purchases.paywalls.components.common.serializers

import com.revenuecat.purchases.models.StoreReplacementMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object UpgradeReplacementModeDeserializer : StoreReplacementModeDeserializer(
    defaultValue = StoreReplacementMode.CHARGE_PRORATED_PRICE,
    typeForValue = { value -> value.name.lowercase() },
)

internal object DowngradeReplacementModeDeserializer : StoreReplacementModeDeserializer(
    defaultValue = StoreReplacementMode.DEFERRED,
    typeForValue = { value -> value.name.lowercase() },
)

internal abstract class StoreReplacementModeDeserializer(
    private val defaultValue: StoreReplacementMode,
    typeForValue: (StoreReplacementMode) -> String,
) : KSerializer<StoreReplacementMode> {
    private val valuesByType = listOf(
        StoreReplacementMode.WITHOUT_PRORATION,
        StoreReplacementMode.WITH_TIME_PRORATION,
        StoreReplacementMode.CHARGE_FULL_PRICE,
        StoreReplacementMode.CHARGE_PRORATED_PRICE,
        StoreReplacementMode.DEFERRED,
    ).associateBy(typeForValue)

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StoreReplacementMode", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): StoreReplacementMode {
        val key = decoder.decodeString()
        return valuesByType[key] ?: defaultValue
    }

    override fun serialize(encoder: Encoder, value: StoreReplacementMode) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
