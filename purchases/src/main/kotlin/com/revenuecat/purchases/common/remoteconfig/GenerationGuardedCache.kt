package com.revenuecat.purchases.common.remoteconfig

/**
 * A small thread-safe in-memory cache tagged with the [RemoteConfigManager.configGeneration] the value belongs
 * to, shared by the topic providers (`UiConfigProvider`, `WorkflowsConfigProvider`).
 *
 * Writes are **store-if-newer**: [store] and [invalidate] only apply when their generation is `>=` the newest
 * generation this cache has already acted on ([lastGeneration]), so an out-of-order low-generation warm landing
 * after a fresher commit or an identity-change invalidation can't repopulate stale data.
 *
 * On the read side, a cache **miss** resolves a value off a snapshotted generation; [isCurrent] lets the caller
 * discard that just-resolved value if an invalidation advanced past the snapshot mid-resolve, so an in-flight
 * cold read never serves the previous user's data across an identity change.
 */
internal class GenerationGuardedCache<T : Any> {
    private val lock = Any()

    @Volatile
    private var value: T? = null

    // Newest generation acted on (store or invalidation); an update applies only if its generation is >= this.
    // -1 = nothing seen yet.
    private var lastGeneration: Int = -1

    val cached: T?
        get() = value

    fun isWarm(): Boolean = value != null

    /** Whether this cache has already acted on [generation] or a newer one. */
    fun isAtOrAbove(generation: Int): Boolean = synchronized(lock) { lastGeneration >= generation }

    /** Whether this cache holds a value acted on at [generation] or newer (warm and not older). */
    fun isWarmAtOrAbove(generation: Int): Boolean =
        synchronized(lock) { lastGeneration >= generation && value != null }

    /** True while [generation] is still the newest acted-on generation (nothing newer stored/invalidated). */
    fun isCurrent(generation: Int): Boolean = synchronized(lock) { generation >= lastGeneration }

    fun store(generation: Int, newValue: T) {
        synchronized(lock) {
            if (generation >= lastGeneration) {
                lastGeneration = generation
                value = newValue
            }
        }
    }

    fun invalidate(generation: Int) {
        synchronized(lock) {
            if (generation >= lastGeneration) {
                lastGeneration = generation
                value = null
            }
        }
    }
}
