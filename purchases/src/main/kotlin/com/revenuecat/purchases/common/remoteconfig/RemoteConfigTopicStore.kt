package com.revenuecat.purchases.common.remoteconfig

/** Read-only access to a topic's persisted item index (metadata only — no blob bytes, no waiting). */
internal fun interface RemoteConfigTopicStore {
    /** The saved items for [name], or `null` when the topic is unknown / nothing has been persisted yet. */
    fun topic(name: String): ConfigTopic?
}
