package com.revenuecat.purchases.common.remoteconfig

/**
 * The remote-config topics the SDK can read through [RemoteConfigManager]. Restricting reads to this closed set
 * keeps consumers from requesting arbitrary (or misspelled) topic names.
 *
 * [wireName] is the key the backend uses in the response's `active_topics` and `topics` map, and the key under
 * which the item index is persisted — the manager maps a [RemoteConfigTopic] to it when reading. The sync/persist
 * path itself stays topic-agnostic (string-keyed), so unknown server topics are still stored and forwarded.
 */
internal enum class RemoteConfigTopic(val wireName: String) {
    Workflows("workflows"),
    UiConfig("ui_config"),
    Sources("sources"),
    ProductEntitlementMapping("product_entitlement_mapping"),
}
