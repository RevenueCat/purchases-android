package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.IconComponent.Formats
import com.revenuecat.purchases.paywalls.components.IconComponent.IconBackground
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Immutable
@Serializable
@SerialName("icon")
public class IconComponent(
    @get:JvmSynthetic
    @SerialName("base_url")
    public val baseUrl: String,
    @get:JvmSynthetic
    @SerialName("icon_name")
    public val iconName: String,
    @get:JvmSynthetic
    public val formats: Formats,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    public val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    public val color: ColorScheme? = null,
    @get:JvmSynthetic
    public val padding: Padding = zero,
    @get:JvmSynthetic
    public val margin: Padding = zero,
    @get:JvmSynthetic
    @SerialName("icon_background")
    public val iconBackground: IconBackground? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialIconComponent>> = emptyList(),
) : PaywallComponent {

    @Poko
    @Serializable
    @Immutable
    public class Formats(
        @get:JvmSynthetic
        public val webp: String,
    )

    @Poko
    @Serializable
    @Immutable
    public class IconBackground(
        @get:JvmSynthetic
        public val color: ColorScheme,
        @get:JvmSynthetic
        public val shape: MaskShape,
        @get:JvmSynthetic
        public val border: Border? = null,
        @get:JvmSynthetic
        public val shadow: Shadow? = null,
    )
}

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialIconComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = true,
    @get:JvmSynthetic
    @SerialName("base_url")
    public val baseUrl: String? = null,
    @get:JvmSynthetic
    @SerialName("icon_name")
    public val iconName: String? = null,
    @get:JvmSynthetic
    public val formats: Formats? = null,
    @get:JvmSynthetic
    public val size: Size? = null,
    @get:JvmSynthetic
    public val color: ColorScheme? = null,
    @get:JvmSynthetic
    public val padding: Padding? = null,
    @get:JvmSynthetic
    public val margin: Padding? = null,
    @get:JvmSynthetic
    @SerialName("icon_background")
    public val iconBackground: IconBackground? = null,
) : PartialComponent
