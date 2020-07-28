package com.revenuecat.purchases.utils

/**
 * A generic exception indicating the problem during serialization or deserialization process
 */
internal open class SerializationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
