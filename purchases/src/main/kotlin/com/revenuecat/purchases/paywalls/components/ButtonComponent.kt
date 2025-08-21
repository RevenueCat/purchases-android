package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent.Action
import com.revenuecat.purchases.paywalls.components.ButtonComponent.Destination
import com.revenuecat.purchases.paywalls.components.ButtonComponent.UrlMethod
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("button")
class ButtonComponent(
    @get:JvmSynthetic val action: Action,
    @get:JvmSynthetic val stack: StackComponent,
    @get:JvmSynthetic val transition: PaywallTransition? = null,
) : PaywallComponent {

    @InternalRevenueCatAPI
    @Serializable(with = ActionSerializer::class)
    sealed interface Action {
        // SerialNames are handled by the ActionSerializer.

        @Serializable
        object Unknown : Action

        @Serializable
        object RestorePurchases : Action

        @Serializable
        object NavigateBack : Action

        @Serializable
        data class NavigateTo(@get:JvmSynthetic val destination: Destination) : Action
    }

    @InternalRevenueCatAPI
    @Serializable
    sealed interface Destination {
        // SerialNames are handled by the ActionSerializer.

        @Serializable
        object Unknown : Destination

        @Serializable
        object CustomerCenter : Destination

        @Serializable
        data class PrivacyPolicy(
            @get:JvmSynthetic val urlLid: LocalizationKey,
            @get:JvmSynthetic val method: UrlMethod,
        ) : Destination

        @Serializable
        data class Terms(
            @get:JvmSynthetic val urlLid: LocalizationKey,
            @get:JvmSynthetic val method: UrlMethod,
        ) : Destination

        @Serializable
        data class Url(
            @get:JvmSynthetic val urlLid: LocalizationKey,
            @get:JvmSynthetic val method: UrlMethod,
        ) : Destination

        @Serializable
        data class Sheet(
            @get:JvmSynthetic val id: String,
            @get:JvmSynthetic val name: String?,
            @get:JvmSynthetic val stack: StackComponent,
            @get:JvmSynthetic @SerialName("background_blur") val backgroundBlur: Boolean,
            @get:JvmSynthetic val size: Size?,
        ) : Destination
    }

    @InternalRevenueCatAPI
    @Serializable(with = UrlMethodDeserializer::class)
    enum class UrlMethod {
        // SerialNames are handled by the UrlMethodDeserializer.

        IN_APP_BROWSER,
        EXTERNAL_BROWSER,
        DEEP_LINK,
        UNKNOWN,
    }
}

/**
 * A custom (de)serializer for Actions using the surrogate pattern. The JSON we get from the backend does not map 1 to
 * 1 to the class structure, as we want to decrease the level of nesting in the JSON that would otherwise occur with
 * this many nested classes.
 *
 * See also:
 * * [RevenueCat/purchases-ios#4408](https://github.com/RevenueCat/purchases-ios/pull/4408)
 * * [Composite serializers via surrogate](https://github.com/Kotlin/kotlinx.serialization/blob/31ab68caec3a448e1485f0d924106044fef112a7/docs/serializers.md#composite-serializer-via-surrogate)
 * * `ButtonComponentTests` for the expected format.
 */
@OptIn(InternalRevenueCatAPI::class)
private object ActionSerializer : KSerializer<Action> {
    override val descriptor: SerialDescriptor = ActionSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Action) {
        val surrogate = ActionSurrogate(action = value)
        encoder.encodeSerializableValue(ActionSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Action {
        val surrogate = decoder.decodeSerializableValue(ActionSurrogate.serializer())
        return surrogate.toAction()
    }
}

@OptIn(InternalRevenueCatAPI::class)
@Serializable
private class ActionSurrogate(
    val type: ActionTypeSurrogate,
    val destination: DestinationSurrogate? = null,
    val url: UrlSurrogate? = null,
    val sheet: Destination.Sheet? = null,
) {
    constructor(action: Action) : this(
        type = when (action) {
            is Action.Unknown -> ActionTypeSurrogate.unknown
            is Action.NavigateBack -> ActionTypeSurrogate.navigate_back
            is Action.NavigateTo -> ActionTypeSurrogate.navigate_to
            is Action.RestorePurchases -> ActionTypeSurrogate.restore_purchases
        },
        destination = when (action) {
            is Action.Unknown,
            is Action.NavigateBack,
            is Action.RestorePurchases,
            -> null

            is Action.NavigateTo -> when (action.destination) {
                is Destination.CustomerCenter -> DestinationSurrogate.customer_center
                is Destination.PrivacyPolicy -> DestinationSurrogate.privacy_policy
                is Destination.Terms -> DestinationSurrogate.terms
                is Destination.Url -> DestinationSurrogate.url
                is Destination.Sheet -> DestinationSurrogate.sheet
                is Destination.Unknown -> DestinationSurrogate.unknown
            }
        },
        url = when (action) {
            is Action.Unknown,
            is Action.NavigateBack,
            is Action.RestorePurchases,
            -> null

            is Action.NavigateTo -> when (action.destination) {
                is Destination.Unknown,
                is Destination.CustomerCenter,
                is Destination.Sheet,
                -> null
                is Destination.PrivacyPolicy -> UrlSurrogate(
                    url_lid = action.destination.urlLid,
                    method = action.destination.method,
                )

                is Destination.Terms -> UrlSurrogate(
                    url_lid = action.destination.urlLid,
                    method = action.destination.method,
                )

                is Destination.Url -> UrlSurrogate(
                    url_lid = action.destination.urlLid,
                    method = action.destination.method,
                )
            }
        },
        sheet = when (action) {
            is Action.Unknown,
            is Action.NavigateBack,
            is Action.RestorePurchases,
            -> null

            is Action.NavigateTo -> when (action.destination) {
                is Destination.CustomerCenter,
                is Destination.PrivacyPolicy,
                is Destination.Terms,
                is Destination.Unknown,
                is Destination.Url,
                -> null
                is Destination.Sheet -> action.destination
            }
        },
    )

    fun toAction(): Action =
        when (type) {
            ActionTypeSurrogate.unknown -> Action.Unknown
            ActionTypeSurrogate.restore_purchases -> Action.RestorePurchases
            ActionTypeSurrogate.navigate_back -> Action.NavigateBack
            ActionTypeSurrogate.navigate_to -> Action.NavigateTo(
                destination = when (destination) {
                    DestinationSurrogate.customer_center -> Destination.CustomerCenter
                    DestinationSurrogate.privacy_policy -> {
                        checkNotNull(url) { "`url` cannot be null when `destination` is `privacy_policy`." }
                        Destination.PrivacyPolicy(
                            urlLid = url.url_lid,
                            method = url.method,
                        )
                    }

                    DestinationSurrogate.terms -> {
                        checkNotNull(url) { "`url` cannot be null when `destination` is `terms`." }
                        Destination.Terms(
                            urlLid = url.url_lid,
                            method = url.method,
                        )
                    }

                    DestinationSurrogate.url -> {
                        checkNotNull(url) { "`url` cannot be null when `destination` is `url`." }
                        Destination.Url(
                            urlLid = url.url_lid,
                            method = url.method,
                        )
                    }

                    DestinationSurrogate.sheet -> {
                        checkNotNull(sheet) { "`sheet` cannot be null when `destination` is `sheet`." }
                        sheet
                    }

                    DestinationSurrogate.unknown -> Destination.Unknown

                    null -> error("`destination` cannot be null when `action` is `navigate_to`.")
                },
            )
        }
}

@Suppress("EnumNaming", "EnumEntryName", "EnumEntryNameCase")
@Serializable(with = ActionTypeSurrogateDeserializer::class)
private enum class ActionTypeSurrogate {
    restore_purchases,
    navigate_back,
    navigate_to,
    unknown,
}

@Suppress("EnumNaming", "EnumEntryName", "EnumEntryNameCase")
@Serializable(with = DestinationSurrogateDeserializer::class)
private enum class DestinationSurrogate {
    customer_center,
    privacy_policy,
    terms,
    url,
    sheet,
    unknown,
}

@OptIn(InternalRevenueCatAPI::class)
@Suppress("ConstructorParameterNaming", "PropertyName")
@Serializable
private class UrlSurrogate(val url_lid: LocalizationKey, val method: UrlMethod)

private object ActionTypeSurrogateDeserializer : EnumDeserializerWithDefault<ActionTypeSurrogate> (
    defaultValue = ActionTypeSurrogate.unknown,
)

private object DestinationSurrogateDeserializer : EnumDeserializerWithDefault<DestinationSurrogate> (
    defaultValue = DestinationSurrogate.unknown,
)

@OptIn(InternalRevenueCatAPI::class)
private object UrlMethodDeserializer : EnumDeserializerWithDefault<UrlMethod> (
    defaultValue = UrlMethod.UNKNOWN,
)
