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
    val components: List<PaywallComponent>,
    @get:JvmSynthetic
    val visible: Boolean? = null,
    @get:JvmSynthetic
    val dimension: Dimension = Vertical(CENTER, START),
    @get:JvmSynthetic
    val size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    val spacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val background: Background? = null,
    @get:JvmSynthetic
    val padding: Padding = zero,
    @get:JvmSynthetic
    val margin: Padding = zero,
    @get:JvmSynthetic
    val shape: Shape? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    val badge: Badge? = null,
    @get:JvmSynthetic
    val overflow: Overflow? = null,
    @get:JvmSynthetic
    val overrides: List<ComponentOverride<PartialStackComponent>> = emptyList(),
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
    val visible: Boolean? = true,
    @get:JvmSynthetic
    val dimension: Dimension? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val spacing: Float? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val background: Background? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
    @get:JvmSynthetic
    val shape: Shape? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    val badge: Badge? = null,
    @get:JvmSynthetic
    val overflow: StackComponent.Overflow? = null,
) : PartialComponent

@OptIn(InternalRevenueCatAPI::class)
internal object StackOverflowDeserializer : EnumDeserializerWithDefault<StackComponent.Overflow>(
    defaultValue = StackComponent.Overflow.NONE,
)
