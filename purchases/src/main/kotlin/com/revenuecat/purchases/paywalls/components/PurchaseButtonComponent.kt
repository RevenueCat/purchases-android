package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent.UrlMethod
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("purchase_button")
@Immutable
public class PurchaseButtonComponent(
    public @get:JvmSynthetic val stack: StackComponent,
    public @get:JvmSynthetic val action: Action? = null,
    public @get:JvmSynthetic val method: Method? = null,
) : PaywallComponent {
    @Serializable(with = ActionDeserializer::class)
    public enum class Action {
        IN_APP_CHECKOUT,
        WEB_CHECKOUT,
        WEB_PRODUCT_SELECTION,
        ;

        public fun toMethod(): Method = when (this) {
            IN_APP_CHECKOUT -> Method.InAppCheckout
            WEB_CHECKOUT -> Method.WebCheckout()
            WEB_PRODUCT_SELECTION -> Method.WebProductSelection()
        }
    }

    @Serializable(with = PurchaseButtonMethodDeserializer::class)
    public sealed interface Method {
        @Serializable
        public object InAppCheckout : Method

        @Serializable
        @Immutable
        public data class WebCheckout(
            @SerialName("auto_dismiss")
            public @get:JvmSynthetic val autoDismiss: Boolean? = null,
            @SerialName("open_method")
            public @get:JvmSynthetic val openMethod: UrlMethod? = null,
        ) : Method

        @Serializable
        @Immutable
        public data class WebProductSelection(
            @SerialName("auto_dismiss")
            public @get:JvmSynthetic val autoDismiss: Boolean? = null,
            @SerialName("open_method")
            public @get:JvmSynthetic val openMethod: UrlMethod? = null,
        ) : Method

        @Serializable
        @Immutable
        public data class CustomWebCheckout(
            @SerialName("custom_url")
            public @get:JvmSynthetic val customUrl: CustomUrl,
            @SerialName("auto_dismiss")
            public @get:JvmSynthetic val autoDismiss: Boolean? = null,
            @SerialName("open_method")
            public @get:JvmSynthetic val openMethod: UrlMethod? = null,
        ) : Method

        @Serializable
        public object Unknown : Method
    }

    @Serializable
    @Immutable
    public data class CustomUrl(
        @SerialName("url_lid")
        public val urlLid: LocalizationKey,
        @SerialName("package_param")
        public val packageParam: String? = null,
    )
}

@OptIn(InternalRevenueCatAPI::class)
private object ActionDeserializer : EnumDeserializerWithDefault<PurchaseButtonComponent.Action> (
    defaultValue = PurchaseButtonComponent.Action.IN_APP_CHECKOUT,
)

@OptIn(InternalRevenueCatAPI::class)
internal object PurchaseButtonMethodDeserializer : SealedDeserializerWithDefault<PurchaseButtonComponent.Method>(
    serialName = "Method",
    serializerByType = mapOf(
        "in_app_checkout" to { PurchaseButtonComponent.Method.InAppCheckout.serializer() },
        "web_checkout" to { PurchaseButtonComponent.Method.WebCheckout.serializer() },
        "web_product_selection" to { PurchaseButtonComponent.Method.WebProductSelection.serializer() },
        "custom_web_checkout" to { PurchaseButtonComponent.Method.CustomWebCheckout.serializer() },
    ),
    defaultValue = { PurchaseButtonComponent.Method.Unknown },
)
