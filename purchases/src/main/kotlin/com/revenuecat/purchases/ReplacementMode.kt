package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.models.legacyPlayBackendName
import com.revenuecat.purchases.models.storeBackendName
import com.revenuecat.purchases.models.toGoogleReplacementMode
import com.revenuecat.purchases.models.toStoreReplacementMode
import com.revenuecat.purchases.models.toStoreReplacementModeOrNull
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
 * Returns the backend name for this [ReplacementMode].
 * For [GoogleReplacementMode], this returns the legacy proration mode name.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Deprecated("Use ReplacementMode.backendName(store: Store) instead")
internal val ReplacementMode.backendName: String
    get() = when (this) {
        is GoogleReplacementMode -> this.toStoreReplacementMode().legacyPlayBackendName()
        is StoreReplacementMode -> this.legacyPlayBackendName()
        else -> this.name
    }

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun ReplacementMode.backendName(store: Store): String? {
    return this.toStoreReplacementModeOrNull()?.storeBackendName(store)
}

internal object ReplacementModeSerializer : KSerializer<ReplacementMode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ReplacementMode") {
        element("type", String.serializer().descriptor)
        element("name", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: ReplacementMode) {
        encoder.encodeStructure(descriptor) {
            val normalizedValue = when (value) {
                is GoogleReplacementMode -> value.toStoreReplacementMode()
                is StoreReplacementMode -> value
                else -> throw SerializationException("Unknown ReplacementMode type: ${value::class.simpleName}")
            }
            // Keep writing GoogleReplacementMode to maintain backwards compatibility with older SDKs.
            encodeStringElement(descriptor, 0, "GoogleReplacementMode")
            encodeStringElement(descriptor, 1, normalizedValue.toGoogleReplacementMode().name)
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
                    StoreReplacementMode.fromName(name)
                        ?: throw SerializationException("Invalid StoreReplacementMode name: $name")
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
