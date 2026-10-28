package com.revenuecat.purchases

import dev.drewhamilton.poko.Poko
import java.util.Date

/**
 * A subscriber attribute value, as returned by [Purchases.attributesForCurrentUser].
 *
 * For more information, refer to [our guide](https://docs.revenuecat.com/docs/subscriber-attributes).
 *
 * @property key The key for this attribute. Keys that begin with `$` are reserved names used by RevenueCat.
 * @property value The value for this attribute.
 * @property lastUpdatedDate The last time this value was modified.
 */
@InternalRevenueCatAPI
@Poko
public class CustomerAttribute internal constructor(
    public val key: String,
    public val value: String,
    public val lastUpdatedDate: Date,
)
