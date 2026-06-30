package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.utils.SerializationException

/**
 * Thrown when a byte buffer cannot be parsed as a valid RC Container Format v1 payload
 * (bad magic, unsupported version, truncated data, sizes that exceed the buffer, or an
 * unsupported/corrupt element content encoding).
 */
internal class RCContainerFormatException(
    message: String,
    cause: Throwable? = null,
) : SerializationException(message, cause)
