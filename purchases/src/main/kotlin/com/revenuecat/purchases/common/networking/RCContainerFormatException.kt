package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.utils.SerializationException

/**
 * Thrown when a byte buffer cannot be parsed as a valid RC Container Format v1 payload
 * (bad magic, unsupported version, truncated data, or sizes that exceed the buffer).
 */
internal class RCContainerFormatException(message: String) : SerializationException(message)
