@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig

/**
 * Outcome of resolving `ui_config` through the `/v1/config` ui_config topic. It lets a caller tell apart the
 * situations a bare `UiConfig?` conflates, so only a genuine failure is reported as one:
 *
 * - [Found]: the [UiConfig] is resolved and in memory.
 * - [NotConfigured]: the topic is absent, or carries none of the ui_config parts. A project with no paywalls
 *   configured legitimately has no `ui_config` at all, so there is nothing to resolve — not a failure.
 * - [Superseded]: the read was superseded twice in a row — a config commit or an identity change advanced the
 *   config generation while each attempt was in flight — so nothing trustworthy could be served. The next read
 *   re-resolves against the fresher config.
 * - [Unavailable]: the topic carries ui_config parts but they could not be resolved into one [UiConfig] (an
 *   unresolvable blob, or a merged object that doesn't decode). The only outcome worth reporting as an error.
 */
internal sealed interface UiConfigResolution {
    data class Found(val uiConfig: UiConfig) : UiConfigResolution

    object NotConfigured : UiConfigResolution

    object Superseded : UiConfigResolution

    object Unavailable : UiConfigResolution
}
