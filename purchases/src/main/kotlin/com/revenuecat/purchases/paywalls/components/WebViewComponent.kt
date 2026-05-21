package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.utils.serializers.URLSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("web_view")
@Immutable
public class WebViewComponent(
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    public val url: URL,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    public val size: Size = Size(width = Fill, height = Fixed(100u)),
) : PaywallComponent
