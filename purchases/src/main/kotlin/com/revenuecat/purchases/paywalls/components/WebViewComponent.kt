package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Paywalls V2 component that renders generic hosted web content (a web bundle entrypoint) inside a
 * web view.
 *
 * Only [protocolVersion] `1` is currently supported. Isolation from external sources is expected from
 * the Content-Security-Policy served by the backend with the web content; the bundle must be fully
 * self-contained.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("web_view")
@Immutable
public class WebViewComponent(
    @get:JvmSynthetic
    public val url: String,
    @get:JvmSynthetic
    public val id: String,
    @get:JvmSynthetic
    public val name: String? = null,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @SerialName("protocol_version")
    @get:JvmSynthetic
    public val protocolVersion: Int,
    @get:JvmSynthetic
    public val size: Size = Size(width = Fill, height = SizeConstraint.Fit()),
) : PaywallComponent
