package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PresentedOfferingContextSerializer
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.utils.serializers.DateSerializer
import com.revenuecat.purchases.utils.serializers.UUIDSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import java.util.UUID

/**
 * Types of paywall events. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Serializable
public enum class PaywallEventType(public val value: String) {
    /**
     * The paywall was shown to the user.
     */
    IMPRESSION("paywall_impression"),

    /**
     * The user cancelled a purchase.
     */
    CANCEL("paywall_cancel"),

    /**
     * The paywall was dismissed.
     */
    CLOSE("paywall_close"),

    /**
     * The user initiated a purchase through the paywall.
     */
    PURCHASE_INITIATED("paywall_purchase_initiated"),

    /**
     * The user encountered an error during purchase.
     */
    PURCHASE_ERROR("paywall_purchase_error"),

    /**
     * An exit offer will be shown to the user.
     */
    EXIT_OFFER("paywall_exit_offer"),

    /**
     * User interacted with a paywall component (tabs, carousel, non-purchase button, etc.).
     */
    COMPONENT_INTERACTION("paywall_component_interacted"),
}

/**
 * Component categories for [PaywallEventType.COMPONENT_INTERACTION].
 */
@InternalRevenueCatAPI
@Serializable
public enum class PaywallComponentType {
    @SerialName("tab")
    TAB,

    @SerialName("switch")
    SWITCH,

    @SerialName("carousel")
    CAROUSEL,

    @SerialName("button")
    BUTTON,

    @SerialName("text")
    TEXT,

    @SerialName("package")
    PACKAGE,

    @SerialName("package_selection_sheet")
    PACKAGE_SELECTION_SHEET,

    @SerialName("purchase_button")
    PURCHASE_BUTTON,
}

/**
 * Payload for [PaywallEventType.COMPONENT_INTERACTION].
 */
@InternalRevenueCatAPI
@Serializable
public data class PaywallComponentInteractionData(
    public val componentType: PaywallComponentType,
    public val componentName: String? = null,
    public val componentValue: String,
    public val componentUrl: String? = null,
    public val originIndex: Int? = null,
    public val destinationIndex: Int? = null,
    public val originContextName: String? = null,
    public val destinationContextName: String? = null,
    public val defaultIndex: Int? = null,
    public val originPackageIdentifier: String? = null,
    public val destinationPackageIdentifier: String? = null,
    public val defaultPackageIdentifier: String? = null,
    public val originProductIdentifier: String? = null,
    public val destinationProductIdentifier: String? = null,
    public val defaultProductIdentifier: String? = null,
    public val currentPackageIdentifier: String? = null,
    public val resultingPackageIdentifier: String? = null,
    public val currentProductIdentifier: String? = null,
    public val resultingProductIdentifier: String? = null,
)

/**
 * Types of exit offers. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Serializable
public enum class ExitOfferType(public val value: String) {
    /**
     * An exit offer shown when the user attempts to dismiss the paywall without interacting.
     */
    DISMISS("dismiss"),
}

/**
 * Type representing a paywall event and associated data. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Serializable
public data class PaywallEvent(
    public val creationData: CreationData,
    public val data: Data,
    public val type: PaywallEventType,
    public val componentInteraction: PaywallComponentInteractionData? = null,
) : FeatureEvent {

    override val isPriorityEvent: Boolean get() = type == PaywallEventType.IMPRESSION

    @Serializable
    public data class CreationData(
        @Serializable(with = UUIDSerializer::class)
        public val id: UUID,
        @Serializable(with = DateSerializer::class)
        public val date: Date,
    )

    @Serializable(with = PaywallEventDataSerializer::class)
    public data class Data(
        val paywallIdentifier: String?,
        val presentedOfferingContext: PresentedOfferingContext,
        val paywallRevision: Int,
        @Serializable(with = UUIDSerializer::class)
        public val sessionIdentifier: UUID,
        val displayMode: String, // Refer to PaywallMode in the RevenueCatUI module.
        val localeIdentifier: String,
        val darkMode: Boolean,
        val exitOfferType: ExitOfferType? = null,
        val exitOfferingIdentifier: String? = null,
        val packageIdentifier: String? = null,
        val productIdentifier: String? = null,
        val errorCode: Int? = null,
        val errorMessage: String? = null,
    )

    internal fun toPaywallPostReceiptData(): PaywallPostReceiptData {
        return PaywallPostReceiptData(
            paywallID = data.paywallIdentifier,
            sessionID = data.sessionIdentifier.toString(),
            revision = data.paywallRevision,
            displayMode = data.displayMode,
            darkMode = data.darkMode,
            localeIdentifier = data.localeIdentifier,
            offeringId = data.presentedOfferingContext.offeringIdentifier,
        )
    }
}

/**
 * Custom serializer for PaywallEvent.Data that supports parsing both the old
 * `offeringIdentifier` and the new `presentedOfferingContext`.
 */
@OptIn(InternalRevenueCatAPI::class)
internal object PaywallEventDataSerializer : KSerializer<PaywallEvent.Data> {
    private const val PAYWALL_IDENTIFIER_INDEX = 0
    private const val PRESENTED_OFFERING_CONTEXT_INDEX = 1
    private const val PAYWALL_REVISION_INDEX = 2
    private const val SESSION_IDENTIFIER_INDEX = 3
    private const val DISPLAY_MODE_INDEX = 4
    private const val LOCALE_IDENTIFIER_INDEX = 5
    private const val DARK_MODE_INDEX = 6
    private const val EXIT_OFFER_TYPE_INDEX = 7
    private const val EXIT_OFFERING_IDENTIFIER_INDEX = 8
    private const val PACKAGE_IDENTIFIER_INDEX = 9
    private const val PRODUCT_IDENTIFIER_INDEX = 10
    private const val ERROR_CODE_INDEX = 11
    private const val ERROR_MESSAGE_INDEX = 12

    private val nullableStringSerializer = String.serializer().nullable
    private val nullableIntSerializer = Int.serializer().nullable
    private val nullableExitOfferTypeSerializer = ExitOfferType.serializer().nullable

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PaywallEvent.Data") {
        element("paywallIdentifier", nullableStringSerializer.descriptor)
        element("presentedOfferingContext", PresentedOfferingContextSerializer.descriptor)
        element("paywallRevision", Int.serializer().descriptor)
        element("sessionIdentifier", UUIDSerializer.descriptor)
        element("displayMode", String.serializer().descriptor)
        element("localeIdentifier", String.serializer().descriptor)
        element("darkMode", Boolean.serializer().descriptor)
        element("exitOfferType", nullableExitOfferTypeSerializer.descriptor)
        element("exitOfferingIdentifier", nullableStringSerializer.descriptor)
        element("packageIdentifier", nullableStringSerializer.descriptor)
        element("productIdentifier", nullableStringSerializer.descriptor)
        element("errorCode", nullableIntSerializer.descriptor)
        element("errorMessage", nullableStringSerializer.descriptor)
        // Legacy field for backward compatibility
        element("offeringIdentifier", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: PaywallEvent.Data) {
        encoder.encodeStructure(descriptor) {
            // Encode paywallIdentifier (nullable)
            value.paywallIdentifier?.let {
                encodeStringElement(descriptor, PAYWALL_IDENTIFIER_INDEX, it)
            }
            // Always encode required fields
            encodeSerializableElement(
                descriptor,
                PRESENTED_OFFERING_CONTEXT_INDEX,
                PresentedOfferingContextSerializer,
                value.presentedOfferingContext,
            )
            encodeIntElement(descriptor, PAYWALL_REVISION_INDEX, value.paywallRevision)
            encodeSerializableElement(descriptor, SESSION_IDENTIFIER_INDEX, UUIDSerializer, value.sessionIdentifier)
            encodeStringElement(descriptor, DISPLAY_MODE_INDEX, value.displayMode)
            encodeStringElement(descriptor, LOCALE_IDENTIFIER_INDEX, value.localeIdentifier)
            encodeBooleanElement(descriptor, DARK_MODE_INDEX, value.darkMode)

            // Only encode optional fields if they have values
            value.exitOfferType?.let {
                encodeSerializableElement(descriptor, EXIT_OFFER_TYPE_INDEX, ExitOfferType.serializer(), it)
            }
            value.exitOfferingIdentifier?.let {
                encodeStringElement(descriptor, EXIT_OFFERING_IDENTIFIER_INDEX, it)
            }
            value.packageIdentifier?.let {
                encodeStringElement(descriptor, PACKAGE_IDENTIFIER_INDEX, it)
            }
            value.productIdentifier?.let {
                encodeStringElement(descriptor, PRODUCT_IDENTIFIER_INDEX, it)
            }
            value.errorCode?.let {
                encodeIntElement(descriptor, ERROR_CODE_INDEX, it)
            }
            value.errorMessage?.let {
                encodeStringElement(descriptor, ERROR_MESSAGE_INDEX, it)
            }
        }
    }

    @Suppress("LongMethod")
    override fun deserialize(decoder: Decoder): PaywallEvent.Data {
        // Only JSON deserialization is supported for backward compatibility
        if (decoder !is JsonDecoder) {
            throw SerializationException("PaywallEvent.Data only supports JSON deserialization")
        }

        val jsonElement = decoder.decodeJsonElement()
        val jsonObject = jsonElement.jsonObject

        val presentedOfferingContext = if (jsonObject.containsKey("presentedOfferingContext")) {
            // New format: decode the presentedOfferingContext object
            decoder.json.decodeFromJsonElement(
                PresentedOfferingContextSerializer,
                jsonObject["presentedOfferingContext"]!!,
            )
        } else if (jsonObject.containsKey("offeringIdentifier")) {
            // Old format: convert the offeringIdentifier string to PresentedOfferingContext
            val offeringId = jsonObject["offeringIdentifier"]!!.jsonPrimitive.content
            PresentedOfferingContext(offeringId)
        } else {
            throw SerializationException("Missing offering context information")
        }

        // Decode all other fields
        val paywallIdentifier = jsonObject["paywallIdentifier"]?.let {
            decoder.json.decodeFromJsonElement(String.serializer(), it)
        }
        val paywallRevision = decoder.json.decodeFromJsonElement(Int.serializer(), jsonObject["paywallRevision"]!!)
        val sessionIdentifier = decoder.json.decodeFromJsonElement(
            UUIDSerializer,
            jsonObject["sessionIdentifier"]!!,
        )
        val displayMode = decoder.json.decodeFromJsonElement(String.serializer(), jsonObject["displayMode"]!!)
        val localeIdentifier = decoder.json.decodeFromJsonElement(
            String.serializer(),
            jsonObject["localeIdentifier"]!!,
        )
        val darkMode = decoder.json.decodeFromJsonElement(Boolean.serializer(), jsonObject["darkMode"]!!)
        val exitOfferType = jsonObject["exitOfferType"]?.let {
            decoder.json.decodeFromJsonElement(ExitOfferType.serializer(), it)
        }
        val exitOfferingIdentifier = jsonObject["exitOfferingIdentifier"]?.let {
            decoder.json.decodeFromJsonElement(String.serializer(), it)
        }
        val packageIdentifier = jsonObject["packageIdentifier"]?.let {
            decoder.json.decodeFromJsonElement(String.serializer(), it)
        }
        val productIdentifier = jsonObject["productIdentifier"]?.let {
            decoder.json.decodeFromJsonElement(String.serializer(), it)
        }
        val errorCode = jsonObject["errorCode"]?.let {
            decoder.json.decodeFromJsonElement(Int.serializer(), it)
        }
        val errorMessage = jsonObject["errorMessage"]?.let {
            decoder.json.decodeFromJsonElement(String.serializer(), it)
        }

        return PaywallEvent.Data(
            paywallIdentifier = paywallIdentifier,
            presentedOfferingContext = presentedOfferingContext,
            paywallRevision = paywallRevision,
            sessionIdentifier = sessionIdentifier,
            displayMode = displayMode,
            localeIdentifier = localeIdentifier,
            darkMode = darkMode,
            exitOfferType = exitOfferType,
            exitOfferingIdentifier = exitOfferingIdentifier,
            packageIdentifier = packageIdentifier,
            productIdentifier = productIdentifier,
            errorCode = errorCode,
            errorMessage = errorMessage,
        )
    }
}

@InternalRevenueCatAPI
internal fun PaywallComponentType.toWireString(): String = when (this) {
    PaywallComponentType.TAB -> "tab"
    PaywallComponentType.SWITCH -> "switch"
    PaywallComponentType.CAROUSEL -> "carousel"
    PaywallComponentType.BUTTON -> "button"
    PaywallComponentType.TEXT -> "text"
    PaywallComponentType.PACKAGE -> "package"
    PaywallComponentType.PACKAGE_SELECTION_SHEET -> "package_selection_sheet"
    PaywallComponentType.PURCHASE_BUTTON -> "purchase_button"
}

/**
 * Flattened component-interaction values for [BackendEvent.Paywalls] (shared by stored-event paths).
 */
@InternalRevenueCatAPI
internal data class BackendPaywallComponentFields(
    val componentType: String? = null,
    val componentName: String? = null,
    val componentValue: String? = null,
    val componentUrl: String? = null,
    val originIndex: Int? = null,
    val destinationIndex: Int? = null,
    val originContextName: String? = null,
    val destinationContextName: String? = null,
    val defaultIndex: Int? = null,
    val originPackageIdentifier: String? = null,
    val destinationPackageIdentifier: String? = null,
    val defaultPackageIdentifier: String? = null,
    val originProductIdentifier: String? = null,
    val destinationProductIdentifier: String? = null,
    val defaultProductIdentifier: String? = null,
    val currentPackageIdentifier: String? = null,
    val resultingPackageIdentifier: String? = null,
    val currentProductIdentifier: String? = null,
    val resultingProductIdentifier: String? = null,
)

@InternalRevenueCatAPI
internal fun PaywallComponentInteractionData?.toBackendComponentFields(): BackendPaywallComponentFields {
    val interaction = this ?: return BackendPaywallComponentFields()
    return BackendPaywallComponentFields(
        componentType = interaction.componentType.toWireString(),
        componentName = interaction.componentName,
        componentValue = interaction.componentValue,
        componentUrl = interaction.componentUrl,
        originIndex = interaction.originIndex,
        destinationIndex = interaction.destinationIndex,
        originContextName = interaction.originContextName,
        destinationContextName = interaction.destinationContextName,
        defaultIndex = interaction.defaultIndex,
        originPackageIdentifier = interaction.originPackageIdentifier,
        destinationPackageIdentifier = interaction.destinationPackageIdentifier,
        defaultPackageIdentifier = interaction.defaultPackageIdentifier,
        originProductIdentifier = interaction.originProductIdentifier,
        destinationProductIdentifier = interaction.destinationProductIdentifier,
        defaultProductIdentifier = interaction.defaultProductIdentifier,
        currentPackageIdentifier = interaction.currentPackageIdentifier,
        resultingPackageIdentifier = interaction.resultingPackageIdentifier,
        currentProductIdentifier = interaction.currentProductIdentifier,
        resultingProductIdentifier = interaction.resultingProductIdentifier,
    )
}
