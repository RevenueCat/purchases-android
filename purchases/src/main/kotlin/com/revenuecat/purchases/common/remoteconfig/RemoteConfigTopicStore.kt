package com.revenuecat.purchases.common.remoteconfig

/** Read-only access to a topic's persisted item index (metadata only — no blob bytes, no waiting). */
internal fun interface RemoteConfigTopicStore {
    /**
     * The saved items for [topic], or `null` when nothing has been persisted for it yet. Implementations map the
     * [RemoteConfigTopic] to its [RemoteConfigTopic.wireName] to look it up in the string-keyed disk cache.
     */
    fun topic(topic: RemoteConfigTopic): ConfigTopic?
}
