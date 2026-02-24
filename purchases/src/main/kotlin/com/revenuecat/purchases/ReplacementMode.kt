package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Contains information about the replacement mode to use in case of a product upgrade.
 * Use the platform specific subclasses in each implementation.
 * @property name Identifier of the proration mode to be used
 */
public interface ReplacementMode : Parcelable {
    @IgnoredOnParcel
    public val name: String
}

/**
 * [GoogleReplacementMode] used to be `GoogleProrationMode`. The backend still expects these values, hence this enum.
 */
private enum class LegacyProrationMode {
    IMMEDIATE_WITHOUT_PRORATION,
    IMMEDIATE_WITH_TIME_PRORATION,
    IMMEDIATE_AND_CHARGE_FULL_PRICE,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE,
    DEFERRED,
}

private val GoogleReplacementMode.asLegacyProrationMode: LegacyProrationMode
    get() = when (this) {
        GoogleReplacementMode.WITHOUT_PRORATION -> LegacyProrationMode.IMMEDIATE_WITHOUT_PRORATION
        GoogleReplacementMode.WITH_TIME_PRORATION -> LegacyProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        GoogleReplacementMode.CHARGE_FULL_PRICE -> LegacyProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE
        GoogleReplacementMode.CHARGE_PRORATED_PRICE -> LegacyProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE
        GoogleReplacementMode.DEFERRED -> LegacyProrationMode.DEFERRED
    }

/**
 * Returns the backend name for this [ReplacementMode].
 * For [GoogleReplacementMode], this returns the legacy proration mode name.
 * For [GalaxyReplacementMode], this returns the enum name directly.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal val ReplacementMode.backendName: String
    get() = when (this) {
        is GoogleReplacementMode -> this.asLegacyProrationMode.name
        is GalaxyReplacementMode -> this.name
        else -> this.name
    }

internal object ReplacementModeSerializer : KSerializer<ReplacementMode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ReplacementMode") {
        element("type", String.serializer().descriptor)
        element("name", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: ReplacementMode) {
        encoder.encodeStructure(descriptor) {
            val type = when (value) {
                is GoogleReplacementMode -> "GoogleReplacementMode"
                else -> throw SerializationException("Unknown ReplacementMode type: ${value::class.simpleName}")
            }
            encodeStringElement(descriptor, 0, type)
            encodeStringElement(descriptor, 1, value.name)
        }
    }

    override fun deserialize(decoder: Decoder): ReplacementMode {
        return decoder.decodeStructure(descriptor) {
            var type = ""
            var name = ""

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> name = decodeStringElement(descriptor, 1)
                    -1 -> break
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }

            when (type) {
                "GoogleReplacementMode" -> {
                    try {
                        GoogleReplacementMode.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        throw SerializationException("Invalid GoogleReplacementMode name: $name", e)
                    }
                }
                else -> throw SerializationException("Unknown ReplacementMode type: $type")
            }
        }
    }
}
