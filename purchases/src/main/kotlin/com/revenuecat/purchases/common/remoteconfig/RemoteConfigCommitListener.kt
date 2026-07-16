package com.revenuecat.purchases.common.remoteconfig

/**
 * A topic-agnostic hook into [RemoteConfigManager]'s commit lifecycle so consumers (e.g. the topic providers)
 * can keep an in-memory cache of parsed config data coherent with the persisted state without polling.
 *
 * The [generation] is a monotonically increasing token the manager advances on every mutation: a successful
 * `/v1/config` persist ([onConfigCommitted]), and an identity-change wipe or 4xx disable ([onConfigInvalidated]).
 * A listener that warms an in-memory cache asynchronously should tag its result with the generation it was
 * started for and store it only if that generation is still the newest it has seen (store-if-newer), so a
 * slower disk warm can never clobber a fresher network commit.
 *
 * The manager never interprets what a topic means; it only emits the generation. Callbacks fire on the
 * manager's thread while it holds its cache lock, so implementations must not block — do IO on their own scope.
 */
internal fun interface RemoteConfigCommitListener {
    fun onConfigCommitted(generation: Int)

    // Default no-op so a listener that only reacts to commits (re-warm) needn't handle invalidation explicitly.
    fun onConfigInvalidated(generation: Int) {}
}
