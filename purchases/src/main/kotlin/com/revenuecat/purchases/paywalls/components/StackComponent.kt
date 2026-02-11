package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("stack")
@Immutable
public class StackComponent(
    @get:JvmSynthetic
    public val components: List<PaywallComponent>,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    public val dimension: Dimension = Vertical(CENTER, START),
    @get:JvmSynthetic
    public val size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    public val spacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    public val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    public val background: Background? = null,
    @get:JvmSynthetic
    public val padding: Padding = zero,
    @get:JvmSynthetic
    public val margin: Padding = zero,
    @get:JvmSynthetic
    public val shape: Shape? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    public val badge: Badge? = null,
    @get:JvmSynthetic
    public val overflow: Overflow? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialStackComponent>> = emptyList(),
) : PaywallComponent {

    @Serializable(with = StackOverflowDeserializer::class)
    enum class Overflow {
        // SerialNames are handled by the StackOverflowDeserializer

        NONE,
        SCROLL,
    }
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialStackComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = true,
    @get:JvmSynthetic
    public val dimension: Dimension? = null,
    @get:JvmSynthetic
    public val size: Size? = null,
    @get:JvmSynthetic
    public val spacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    public val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    public val background: Background? = null,
    @get:JvmSynthetic
    public val padding: Padding? = null,
    @get:JvmSynthetic
    public val margin: Padding? = null,
    @get:JvmSynthetic
    public val shape: Shape? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    public val badge: Badge? = null,
    @get:JvmSynthetic
    public val overflow: StackComponent.Overflow? = null,
) : PartialComponent

@OptIn(InternalRevenueCatAPI::class)
internal object StackOverflowDeserializer : EnumDeserializerWithDefault<StackComponent.Overflow>(
    defaultValue = StackComponent.Overflow.NONE,
)
