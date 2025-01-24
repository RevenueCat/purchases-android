package com.revenuecat.purchases.utils

@Suppress("UNCHECKED_CAST")
internal fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    filterValues { it != null } as Map<K, V>

internal fun <K, V, R : Any> Map<K, V>.mapNotNullKeys(transform: (Map.Entry<K, V>) -> R?): Map<R, V> {
    val destination = LinkedHashMap<R, V>(size)
    forEach { entry ->
        val transformedKey = transform(entry)
        if (transformedKey != null) {
            destination[transformedKey] = entry.value
        }
    }
    return destination
}
