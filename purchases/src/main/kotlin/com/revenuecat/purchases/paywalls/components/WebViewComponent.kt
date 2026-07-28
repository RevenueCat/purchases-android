package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Size
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
    public val size: Size,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialWebViewComponent>> = emptyList(),
) : PaywallComponent {

    public companion object {
        /**
         * The host<->component bridge protocol version this SDK implements. A config declaring any
         * other version is treated as an unrecognized component (rendered via its `fallback`). This is
         * the single source of truth: `WebViewEnvelope.DEFAULT_PROTOCOL_VERSION` in `:ui:revenuecatui`
         * derives from it, so the schema gate and the runtime handshake can never disagree.
         */
        @InternalRevenueCatAPI
        public const val SUPPORTED_PROTOCOL_VERSION: Int = 1
    }
}

/**
 * The subset of [WebViewComponent] properties that can be adjusted by a rule-based
 * [ComponentOverride]. Only [visible] is overridable; `url` and `size` always come from the base
 * component.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialWebViewComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = true,
) : PartialComponent
