package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("sticky_footer")
@Immutable
public class StickyFooterComponent(
    @get:JvmSynthetic
    public val stack: StackComponent,
    @get:JvmSynthetic
    public val layout: Layout? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialStickyFooterComponent>> = emptyList(),
) : PaywallComponent {
    @Serializable(with = FooterLayoutDeserializer::class)
    public enum class Layout {
        NORMAL,
        HIDDEN,
        SPLIT_LEADING,
        SPLIT_TRAILING,
    }
}

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialStickyFooterComponent(
    @get:JvmSynthetic
    public val layout: StickyFooterComponent.Layout? = null,
) : PartialComponent

@OptIn(InternalRevenueCatAPI::class)
internal object FooterLayoutDeserializer : EnumDeserializerWithDefault<StickyFooterComponent.Layout>(
    serialName = "com.revenuecat.purchases.paywalls.components.StickyFooterComponent.Layout",
    valuesByType = mapOf(
        "normal" to StickyFooterComponent.Layout.NORMAL,
        "hidden" to StickyFooterComponent.Layout.HIDDEN,
        "split_leading" to StickyFooterComponent.Layout.SPLIT_LEADING,
        "split_trailing" to StickyFooterComponent.Layout.SPLIT_TRAILING,
    ),
    defaultValue = StickyFooterComponent.Layout.NORMAL,
)
