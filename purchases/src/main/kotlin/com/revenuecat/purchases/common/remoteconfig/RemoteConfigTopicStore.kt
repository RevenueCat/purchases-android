package com.revenuecat.purchases.common.remoteconfig

/**
 * Reads a topic's persisted item index (metadata only — no blob bytes, no waiting). This is the seam consumers
 * reach through `RemoteConfigManager.topic(name)`.
 *
 * Declared as an interface, not a concrete class: the broader config layer can supply its own backing. The
 * manager binds it inline to the persisted topic metadata by default so feature read paths work, without this
 * change shipping a standalone implementation.
 */
internal fun interface RemoteConfigTopicStore {
    /** The saved items for [name], or `null` when the topic is unknown / nothing has been persisted yet. */
    fun topic(name: String): ConfigTopic?
}
