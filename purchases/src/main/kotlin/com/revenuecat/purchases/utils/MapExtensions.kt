package com.revenuecat.purchases.utils

@Suppress("UNCHECKED_CAST")
internal fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    filterValues { it != null } as Map<K, V>
