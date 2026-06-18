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
 * Only [protocolVersion] `1` is currently supported. No protocol-version-specific behavior is
 * implemented yet.
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
    public val id: String? = null,
    @get:JvmSynthetic
    public val name: String? = null,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @SerialName("protocol_version")
    @get:JvmSynthetic
    public val protocolVersion: Int? = null,
    @get:JvmSynthetic
    public val size: Size = Size(width = Fill, height = SizeConstraint.Fit),
    @get:JvmSynthetic
    public val fallback: StackComponent? = null,
    @get:JvmSynthetic
    public val capabilities: Capabilities? = null,
) : PaywallComponent {

    /**
     * Web view capabilities as declared by the schema. Currently decoded and preserved only; none of
     * these values change WebView behavior yet.
     */
    @InternalRevenueCatAPI
    @Poko
    @Serializable
    @Immutable
    public class Capabilities(
        @SerialName("network_access")
        @get:JvmSynthetic
        public val networkAccess: NetworkAccess? = null,
        @get:JvmSynthetic
        public val camera: Boolean? = null,
        @get:JvmSynthetic
        public val microphone: Boolean? = null,
        @SerialName("clipboard_write")
        @get:JvmSynthetic
        public val clipboardWrite: Boolean? = null,
        @SerialName("clipboard_read")
        @get:JvmSynthetic
        public val clipboardRead: Boolean? = null,
        @get:JvmSynthetic
        public val geolocation: Boolean? = null,
    ) {

        @InternalRevenueCatAPI
        @Poko
        @Serializable
        @Immutable
        public class NetworkAccess(
            @SerialName("allowed_domains")
            @get:JvmSynthetic
            public val allowedDomains: List<String> = emptyList(),
        )
    }
}
