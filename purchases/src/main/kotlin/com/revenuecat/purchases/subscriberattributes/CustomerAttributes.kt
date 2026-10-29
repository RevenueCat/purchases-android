@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.CustomerAttribute
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributeMap

/**
 * The public view of stored attributes, sorted by key. A null or empty value is how a deletion is stored, so
 * those are left out.
 */
internal fun SubscriberAttributeMap.toCustomerAttributes(): List<CustomerAttribute> =
    values
        .mapNotNull { attribute ->
            val value = attribute.value?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            CustomerAttribute(attribute.key.backendKey, value, attribute.setTime)
        }
        .sortedBy { it.key }
