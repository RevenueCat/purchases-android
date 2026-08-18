package com.revenuecat.purchases.common.remoteconfig

/**
 * Describes why the SDK is requesting remote config.
 *
 * Each case maps to the SDK situation that triggered the config fetch. [wireName] is sent in the remote config
 * request body as the `fetch_context` field.
 */
internal enum class RemoteConfigFetchContext(val wireName: String) {
    AppStart("app_start"),
    Foreground("foreground"),
    IdentityChange("identity_change"),
    Read("read"),
}
