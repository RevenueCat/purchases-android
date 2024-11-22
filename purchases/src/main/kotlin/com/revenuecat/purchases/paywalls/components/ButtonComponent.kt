package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.paywalls.components.ButtonComponent.Action
import com.revenuecat.purchases.paywalls.components.ButtonComponent.Destination
import com.revenuecat.purchases.paywalls.components.ButtonComponent.UrlMethod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@SerialName("button")
internal data class ButtonComponent(
    val action: Action,
    val stack: StackComponent,
) : PaywallComponent {

    @Serializable(with = ActionSerializer::class)
    sealed interface Action {
        // SerialNames are handled by the ActionSerializer.

        @Serializable
        object RestorePurchases : Action

        @Serializable
        object NavigateBack : Action

        @Serializable
        data class NavigateTo(val destination: Destination) : Action
    }

    @Serializable
    sealed interface Destination {
        // SerialNames are handled by the ActionSerializer.

        @Serializable
        object CustomerCenter : Destination

        @Serializable
        data class PrivacyPolicy(val urlLid: String, val method: UrlMethod) : Destination

        @Serializable
        data class Terms(val urlLid: String, val method: UrlMethod) : Destination

        @Serializable
        data class Url(val urlLid: String, val method: UrlMethod) : Destination
    }

    @Serializable
    enum class UrlMethod {
        @SerialName("in_app_browser")
        IN_APP_BROWSER,

        @SerialName("external_browser")
        EXTERNAL_BROWSER,

        @SerialName("deep_link")
        DEEP_LINK,
    }
}

/**
 * Using the surrogate pattern to serialize Actions in a custom way. We want to decrease the level of nesting in the
 * JSON that would otherwise occur with this many nested classes.
 *
 * See also:
 * * [RevenueCat/purchases-ios#4408](https://github.com/RevenueCat/purchases-ios/pull/4408)
 * * [Composite serializers via surrogate](https://github.com/Kotlin/kotlinx.serialization/blob/31ab68caec3a448e1485f0d924106044fef112a7/docs/serializers.md#composite-serializer-via-surrogate)
 * * `ButtonComponentTests` for the expected format.
 */
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

@Serializable
private class ActionSurrogate(
    val type: ActionTypeSurrogate,
    val destination: DestinationSurrogate? = null,
    val url: UrlSurrogate? = null,
) {
    constructor(action: Action) : this(
        type = when (action) {
            is Action.NavigateBack -> ActionTypeSurrogate.navigate_back
            is Action.NavigateTo -> ActionTypeSurrogate.navigate_to
            is Action.RestorePurchases -> ActionTypeSurrogate.restore_purchases
        },
        destination = when (action) {
            is Action.NavigateBack,
            is Action.RestorePurchases,
            -> null

            is Action.NavigateTo -> when (action.destination) {
                is Destination.CustomerCenter -> DestinationSurrogate.customer_center
                is Destination.PrivacyPolicy -> DestinationSurrogate.privacy_policy
                is Destination.Terms -> DestinationSurrogate.terms
                is Destination.Url -> DestinationSurrogate.url
            }
        },
        url = when (action) {
            is Action.NavigateBack,
            is Action.RestorePurchases,
            -> null

            is Action.NavigateTo -> when (action.destination) {
                is Destination.CustomerCenter -> null
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
    )

    fun toAction(): Action =
        when (type) {
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

                    null -> error("`destination` cannot be null when `action` is `navigate_to`.")
                },
            )
        }
}

@Suppress("EnumNaming", "EnumEntryName", "EnumEntryNameCase")
@Serializable
private enum class ActionTypeSurrogate {
    restore_purchases,
    navigate_back,
    navigate_to,
}

@Suppress("EnumNaming", "EnumEntryName", "EnumEntryNameCase")
@Serializable
private enum class DestinationSurrogate {
    customer_center,
    privacy_policy,
    terms,
    url,
}

@Suppress("ConstructorParameterNaming", "PropertyName")
@Serializable
private class UrlSurrogate(val url_lid: String, val method: UrlMethod)
