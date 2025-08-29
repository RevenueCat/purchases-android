package com.revenuecat.purchases.storage

import kotlinx.coroutines.Deferred

internal class KeyedDeferredValueStore<H, T>(
    private val lock: Any = object {},
) {
    val deferred: MutableMap<H, Deferred<T>> = mutableMapOf()

    fun getOrPut(key: H, task: () -> Deferred<T>) = synchronized(lock) {
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

    fun clear() = synchronized(lock) {
        deferred.clear()
    }
}
