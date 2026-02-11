package com.revenuecat.purchases.models

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.MessageDigest

/**
 * A checksum for validating file integrity
 */
@InternalRevenueCatAPI
@Serializable
public data class Checksum(
    /** The algorithm used to generate the checksum */
    @SerialName("algo") val algorithm: Algorithm,

    /** The checksum hash value */
    @SerialName("value") val value: String,
) {
    /**
     * Supported hashing algorithms
     */
    @Serializable
    enum class Algorithm(val algorithmName: String) {
        @SerialName("sha256")
        SHA256("SHA-256"),

        @SerialName("sha384")
        SHA384("SHA-384"),

        @SerialName("sha512")
        SHA512("SHA-512"),

        @SerialName("md5")
        MD5("MD5"),
        ;

        companion object {
            fun fromString(value: String): Algorithm? = when (value.lowercase()) {
                "sha256" -> SHA256
                "sha384" -> SHA384
                "sha512" -> SHA512
                "md5" -> MD5
                else -> null
            }
        }
    }

    public companion object {
        /**
         * Generate a checksum from data
         */
        public fun generate(data: ByteArray, algorithm: Algorithm): Checksum {
            val digest = MessageDigest.getInstance(algorithm.algorithmName)
            val hash = digest.digest(data)
            return Checksum(algorithm, hash.toHexString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Checksum) return false
        if (this.value.lowercase() != other.value.lowercase() || this.algorithm != other.algorithm) {
            return false
        }
        return true
    }

    /**
     * Exception thrown when checksum validation fails
     */
    class ChecksumValidationException : Exception()
}

internal fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}
