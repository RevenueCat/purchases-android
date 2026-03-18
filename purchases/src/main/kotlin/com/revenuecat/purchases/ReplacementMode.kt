package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.models.toGoogleReplacementMode
import com.revenuecat.purchases.models.toStoreReplacementMode
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

private val StoreReplacementMode.galaxyName: String?
    get() = when (this) {
        StoreReplacementMode.WITHOUT_PRORATION -> "INSTANT_NO_PRORATION"
        StoreReplacementMode.WITH_TIME_PRORATION -> "INSTANT_PRORATED_DATE"
        StoreReplacementMode.CHARGE_FULL_PRICE -> null // Unsupported by Galaxy Store
        StoreReplacementMode.CHARGE_PRORATED_PRICE -> "INSTANT_PRORATED_CHARGE"
        StoreReplacementMode.DEFERRED -> "DEFERRED"
    }

private val GoogleReplacementMode.storeReplacementMode: StoreReplacementMode
    get() = when (this) {
        GoogleReplacementMode.WITHOUT_PRORATION -> StoreReplacementMode.WITHOUT_PRORATION
        GoogleReplacementMode.WITH_TIME_PRORATION -> StoreReplacementMode.WITH_TIME_PRORATION
        GoogleReplacementMode.CHARGE_FULL_PRICE -> StoreReplacementMode.CHARGE_FULL_PRICE
        GoogleReplacementMode.CHARGE_PRORATED_PRICE -> StoreReplacementMode.CHARGE_PRORATED_PRICE
        GoogleReplacementMode.DEFERRED -> StoreReplacementMode.DEFERRED
    }

/**
 * Returns the backend name for this [ReplacementMode].
 * For [GoogleReplacementMode], this returns the legacy proration mode name.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Deprecated("Use ReplacementMode.backendName(store: Store) instead")
internal val ReplacementMode.backendName: String
    get() = when (this) {
        is GoogleReplacementMode -> this.asLegacyProrationMode.name
        else -> this.name
    }

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun ReplacementMode.backendName(store: Store): String? {
    return when (store) {
        Store.PLAY_STORE -> when (this) {
            is GoogleReplacementMode -> this.asLegacyProrationMode.name
            is StoreReplacementMode -> this.toGoogleReplacementMode().asLegacyProrationMode.name
            else -> null
        }
        Store.GALAXY -> when (this) {
            is GoogleReplacementMode -> this.storeReplacementMode.galaxyName
            is StoreReplacementMode -> this.galaxyName
            else -> null
        }
        else -> null
    }
}

internal object ReplacementModeSerializer : KSerializer<ReplacementMode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ReplacementMode") {
        element("type", String.serializer().descriptor)
        element("name", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: ReplacementMode) {
        // Always encode to StoreReplacementMode since GoogleReplacementMode is deprecated
        encoder.encodeStructure(descriptor) {
            val normalizedValue = when (value) {
                is GoogleReplacementMode -> value.toStoreReplacementMode()
                is StoreReplacementMode -> value
                else -> throw SerializationException("Unknown ReplacementMode type: ${value::class.simpleName}")
            }
            encodeStringElement(descriptor, 0, "StoreReplacementMode")
            encodeStringElement(descriptor, 1, normalizedValue.name)
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
                "StoreReplacementMode" -> {
                    try {
                        StoreReplacementMode.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        throw SerializationException("Invalid StoreReplacementMode name: $name", e)
                    }
                }
                "GoogleReplacementMode" -> {
                    try {
                        // Parse GoogleReplacementMode to StoreReplacementMode
                        GoogleReplacementMode.valueOf(name).toStoreReplacementMode()
                    } catch (e: IllegalArgumentException) {
                        throw SerializationException("Invalid GoogleReplacementMode name: $name", e)
                    }
                }
                else -> throw SerializationException("Unknown ReplacementMode type: $type")
            }
        }
    }
}
