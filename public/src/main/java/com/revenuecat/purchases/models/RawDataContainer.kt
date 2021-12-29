package com.revenuecat.purchases.models

/**
 * A type which exposes its underlying raw data, for debugging purposes or for getting access
 * to future data while using an older version of the SDK.
 */
interface RawDataContainer<Data> {
    /**
     * The underlying data.
     */
    val rawData: Data
}
