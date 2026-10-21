@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributeMap

/**
 * The public view of stored attributes, sorted by key. A null or empty value is how a deletion is stored, so
 * those are left out.
 */
internal fun SubscriberAttributeMap.toCustomerInfoAttributes(): List<CustomerInfo.Attribute> =
    values
        .mapNotNull { attribute ->
            val value = attribute.value?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            CustomerInfo.Attribute(attribute.key.backendKey, value, attribute.setTime)
        }
        .sortedBy { it.key }
