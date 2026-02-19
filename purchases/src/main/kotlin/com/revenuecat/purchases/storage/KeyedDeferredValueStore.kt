package com.revenuecat.purchases.storage

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.coroutines.Deferred

@InternalRevenueCatAPI
public class KeyedDeferredValueStore<H, T>(
    private val lock: Any = object {},
) {
    public val deferred: MutableMap<H, Deferred<T>> = mutableMapOf()

    public fun getOrPut(key: H, task: () -> Deferred<T>): Deferred<T> = synchronized(lock) {
        deferred.get(key) ?: forgettingFailure(key, task).also { deferred.put(key, it) }
    }

    private fun forgettingFailure(key: H, task: () -> Deferred<T>): Deferred<T> =
        task().apply {
            invokeOnCompletion { exception ->
                exception?.run {
                    synchronized(lock) {
                        deferred.remove(key)
                    }
                }
            }
        }
}
