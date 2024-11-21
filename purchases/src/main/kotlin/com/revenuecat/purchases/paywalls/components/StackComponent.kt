package com.revenuecat.purchases.paywalls.components

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("stack")
internal data class StackComponent(
    val components: List<PaywallComponent>,
    val dimension: Dimension = Vertical(CENTER, START),
    val size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
    val spacing: Float? = null,
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    val padding: Padding = zero,
    val margin: Padding = zero,
    val shape: Shape? = null,
    val border: Border? = null,
    val shadow: Shadow? = null,
    val overrides: ComponentOverrides<PartialStackComponent>? = null,
) : PaywallComponent

@Serializable
internal data class PartialStackComponent(
    val visible: Boolean? = true,
    val dimension: Dimension? = null,
    val size: Size? = null,
    val spacing: Float? = null,
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    val padding: Padding? = null,
    val margin: Padding? = null,
    val shape: Shape? = null,
    val border: Border? = null,
    val shadow: Shadow? = null,
) : PartialComponent
