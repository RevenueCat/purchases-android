package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
public class ComponentsConfig(
    @get:JvmSynthetic public val base: PaywallComponentsConfig,
)

@InternalRevenueCatAPI
@Serializable(with = PaywallComponentsLayoutDirectionDeserializer::class)
public enum class PaywallComponentsLayoutDirection {
    @SerialName("system")
    SYSTEM,

    @SerialName("locale")
    LOCALE,

    @SerialName("rtl")
    RTL,

    @SerialName("ltr")
    LTR,
}

@OptIn(InternalRevenueCatAPI::class)
internal object PaywallComponentsLayoutDirectionDeserializer :
    EnumDeserializerWithDefault<PaywallComponentsLayoutDirection>(
        defaultValue = PaywallComponentsLayoutDirection.SYSTEM,
    )

@InternalRevenueCatAPI
@Poko
@Serializable
public class PaywallComponentsConfig(
    @get:JvmSynthetic public val stack: StackComponent,
    @get:JvmSynthetic public val background: Background,
    @get:JvmSynthetic
    @SerialName("sticky_footer")
    public val stickyFooter: StickyFooterComponent? = null,
    @get:JvmSynthetic
    public val header: HeaderComponent? = null,
    @get:JvmSynthetic
    @SerialName("layout_direction")
    public val layoutDirection: PaywallComponentsLayoutDirection? = null,
)
